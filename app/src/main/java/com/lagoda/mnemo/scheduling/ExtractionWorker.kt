package com.lagoda.mnemo.scheduling

import android.content.Context
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import androidx.work.*
import com.lagoda.mnemo.data.db.entities.ScreenshotEntity
import com.lagoda.mnemo.di.AppModule
import com.lagoda.mnemo.embedding.toEmbeddingBlob
import com.lagoda.mnemo.notification.NotificationHelper
import com.lagoda.mnemo.util.BitmapUtils
import com.lagoda.mnemo.util.DateUtils
import com.lagoda.mnemo.util.MediaStoreScanner
import java.util.UUID

class ExtractionWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = AppModule.getInstance(applicationContext)
        val repo = app.screenshotRepository
        val extractor = app.vlmExtractor
        val onnx = app.onnxEmbeddingEngine

        if (app.appConfig.autoWatchEnabled) scanAndInsertNew()

        val unextracted = repo.getUnextracted()
        if (unextracted.isEmpty()) return Result.success()

        val total = unextracted.size
        var failed = 0
        val startTime = System.currentTimeMillis()

        try {
            setForeground(buildForegroundInfo(applicationContext, 0, total))

            unextracted.forEachIndexed { index, entity ->
                try {
                    val battery = applicationContext
                        .getSystemService(BatteryManager::class.java)
                        .getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                    if (battery < 30) {
                        android.util.Log.i("ExtractionWorker", "Battery $battery% < 30%, stopping after $index items")
                        return Result.success()
                    }

                    // ETA: based on average ms per completed item so far
                    val remainingSeconds = if (index > 0) {
                        val avgMs = (System.currentTimeMillis() - startTime) / index.toLong()
                        ((total - index) * avgMs / 1000L).toInt()
                    } else -1

                    emitProgress(entity.uri, index, total, Step.LOADING, remainingSeconds)
                    setForeground(buildForegroundInfo(applicationContext, index, total, remainingSeconds))

                    val bitmap = BitmapUtils.loadAndResize(
                        applicationContext.contentResolver,
                        Uri.parse(entity.uri)
                    ) ?: run { failed++; return@forEachIndexed }

                    emitProgress(entity.uri, index, total, Step.EXTRACTING, remainingSeconds)

                    val result = extractor.extract(bitmap, entity.uri)
                    bitmap.recycle()

                    if (result != null) {
                        emitProgress(entity.uri, index, total, Step.SAVING, remainingSeconds)

                        val json = kotlinx.serialization.json.Json
                            .encodeToString(com.lagoda.mnemo.data.model.ExtractionResult.serializer(), result)
                        repo.update(entity.copy(
                            extractedJson = json,
                            sourceType = result.source_type
                        ))

                        if (onnx.isReady) {
                            emitProgress(entity.uri, index, total, Step.EMBEDDING, remainingSeconds)
                            val docText = buildDocText(result)
                            val vector = onnx.embed(docText)
                            if (vector.isNotEmpty()) {
                                repo.updateEmbedding(entity.id, vector.toEmbeddingBlob())
                            }
                        }

                        setForeground(buildForegroundInfo(applicationContext, index + 1, total, remainingSeconds))
                    } else {
                        android.util.Log.w("ExtractionWorker", "extract() returned null for ${entity.uri}")
                        failed++
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ExtractionWorker", "Failed to process ${entity.uri}", e)
                    failed++
                }
            }
        } finally {
            // Always post the completion notification — replaces the in-progress one in-place.
            // Runs before Result.success() so it lands before WorkManager detaches the service.
            NotificationHelper.sendIndexingCompleteNotification(applicationContext, total - failed, failed)
        }

        val graphRequest = OneTimeWorkRequestBuilder<GraphUpdateWorker>().build()
        WorkManager.getInstance(applicationContext).enqueue(graphRequest)

        return Result.success()
    }

    private suspend fun scanAndInsertNew() {
        val app = AppModule.getInstance(applicationContext)
        val config = app.appConfig
        val repo = app.screenshotRepository

        val relativePath = config.relativePath ?: return
        val since = if (config.dayFilter == -1) 0L else DateUtils.millisSince(config.dayFilter)

        val candidates = MediaStoreScanner.query(
            applicationContext.contentResolver,
            relativePath,
            since
        )

        for (candidate in candidates) {
            val uriString = candidate.uri.toString()
            if (!repo.existsByUri(uriString)) {
                repo.insert(
                    ScreenshotEntity(
                        id = UUID.randomUUID().toString(),
                        uri = uriString,
                        timestamp = candidate.timestamp
                    )
                )
            }
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo =
        buildForegroundInfo(applicationContext, 0, 0)

    private fun buildForegroundInfo(context: Context, done: Int, total: Int, remainingSeconds: Int = -1): ForegroundInfo {
        val notification = NotificationHelper.buildIndexingNotification(context, done, total, id, remainingSeconds)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NotificationHelper.INDEXING_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NotificationHelper.INDEXING_NOTIFICATION_ID, notification)
        }
    }

    private suspend fun emitProgress(uri: String, index: Int, total: Int, step: Step, remainingSeconds: Int) {
        setProgress(workDataOf(
            KEY_URI to uri,
            KEY_STEP_LABEL to step.label,
            KEY_STEP_NUM to step.ordinal + 1,
            KEY_STEP_TOTAL to Step.entries.size,
            KEY_ITEM_INDEX to index + 1,
            KEY_ITEM_TOTAL to total,
            KEY_REMAINING_SECONDS to remainingSeconds
        ))
    }

    enum class Step(val label: String) {
        LOADING("Loading image"),
        EXTRACTING("Running Gemma"),
        SAVING("Saving result"),
        EMBEDDING("Computing embedding"),
    }

    companion object {
        const val WORK_NAME = "extraction"
        const val KEY_URI = "current_uri"
        const val KEY_STEP_LABEL = "step_label"
        const val KEY_STEP_NUM = "step_num"
        const val KEY_STEP_TOTAL = "step_total"
        const val KEY_ITEM_INDEX = "item_index"
        const val KEY_ITEM_TOTAL = "item_total"
        const val KEY_REMAINING_SECONDS = "remaining_seconds"

        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<ExtractionWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )
        }

        fun buildDocText(result: com.lagoda.mnemo.data.model.ExtractionResult): String =
            buildString {
                if (result.title.isNotBlank()) { append(result.title); append(". ") }
                if (result.summary.isNotBlank()) { append(result.summary); append(". ") }
                if (result.topics.isNotEmpty()) append(result.topics.joinToString(" "))
                if (result.entities.isNotEmpty()) { append(" "); append(result.entities.joinToString(" ")) }
            }.trim()
    }
}
