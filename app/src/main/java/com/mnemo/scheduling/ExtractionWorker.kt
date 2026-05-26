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

        var failed = 0
        unextracted.forEach { entity ->
            try {
                setProgress(workDataOf("current_uri" to entity.uri))
                val bitmap = BitmapUtils.loadAndResize(
                    applicationContext.contentResolver,
                    Uri.parse(entity.uri)
                ) ?: return@forEach

                val result = extractor.extract(bitmap, entity.uri)
                if (result != null) {
                    val json = kotlinx.serialization.json.Json
                        .encodeToString(com.mnemo.data.model.ExtractionResult.serializer(), result)
                    repo.update(entity.copy(
                        extractedJson = json,
                        sourceType = result.source_type
                    ))
                }
                bitmap.recycle()
            } catch (e: Exception) {
                failed++
            }
        }

        // Chain graph update after extraction
        val graphRequest = OneTimeWorkRequestBuilder<GraphUpdateWorker>().build()
        WorkManager.getInstance(applicationContext).enqueue(graphRequest)

        return if (failed == 0) Result.success() else Result.retry()
    }

    companion object {
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
