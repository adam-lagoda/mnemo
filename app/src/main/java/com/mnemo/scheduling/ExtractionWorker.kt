package com.mnemo.scheduling

import android.content.Context
import android.net.Uri
import androidx.work.*
import com.mnemo.di.AppModule
import com.mnemo.util.BitmapUtils
import java.util.UUID

class ExtractionWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = AppModule.getInstance(applicationContext)
        val repo = app.screenshotRepository
        val extractor = app.vlmExtractor

        val unextracted = repo.getUnextracted()
        if (unextracted.isEmpty()) return Result.success()

        val total = unextracted.size
        var failed = 0
        val startTime = System.currentTimeMillis()

        unextracted.forEachIndexed { index, entity ->
            try {
                // ETA: based on average ms per completed item so far
                val remainingSeconds = if (index > 0) {
                    val avgMs = (System.currentTimeMillis() - startTime) / index.toLong()
                    ((total - index) * avgMs / 1000L).toInt()
                } else -1

                emitProgress(entity.uri, index, total, Step.LOADING, remainingSeconds)

                val bitmap = BitmapUtils.loadAndResize(
                    applicationContext.contentResolver,
                    Uri.parse(entity.uri)
                ) ?: return@forEachIndexed

                emitProgress(entity.uri, index, total, Step.EXTRACTING, remainingSeconds)

                val result = extractor.extract(bitmap, entity.uri)
                bitmap.recycle()

                if (result != null) {
                    emitProgress(entity.uri, index, total, Step.SAVING, remainingSeconds)

                    val json = kotlinx.serialization.json.Json
                        .encodeToString(com.mnemo.data.model.ExtractionResult.serializer(), result)
                    repo.update(entity.copy(
                        extractedJson = json,
                        sourceType = result.source_type
                    ))
                }
            } catch (e: Exception) {
                failed++
            }
        }

        val graphRequest = OneTimeWorkRequestBuilder<GraphUpdateWorker>().build()
        WorkManager.getInstance(applicationContext).enqueue(graphRequest)

        return if (failed == 0) Result.success() else Result.retry()
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
        SAVING("Saving result")
    }

    companion object {
        const val KEY_URI = "current_uri"
        const val KEY_STEP_LABEL = "step_label"
        const val KEY_STEP_NUM = "step_num"
        const val KEY_STEP_TOTAL = "step_total"
        const val KEY_ITEM_INDEX = "item_index"
        const val KEY_ITEM_TOTAL = "item_total"
        const val KEY_REMAINING_SECONDS = "remaining_seconds"

        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<ExtractionWorker>()
                .setConstraints(
                    Constraints.Builder().setRequiresBatteryNotLow(true).build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "extraction",
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }
}
