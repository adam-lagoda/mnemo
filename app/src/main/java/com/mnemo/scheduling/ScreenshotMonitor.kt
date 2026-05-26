package com.mnemo.scheduling

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * ContentObserver that watches MediaStore for new screenshots and enqueues
 * ExtractionWorker when changes are detected.
 */
class ScreenshotMonitor(private val context: Context) : ContentObserver(
    Handler(Looper.getMainLooper())
) {
    companion object {
        private const val DEBOUNCE_WORK_NAME = "extraction_debounce"
    }

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        // Debounce: schedule extraction 5s after last change
        val request = OneTimeWorkRequestBuilder<ExtractionWorker>()
            .setInitialDelay(5, TimeUnit.SECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(false)
                    .build()
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            DEBOUNCE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun register() {
        context.contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            this
        )
    }

    fun unregister() {
        context.contentResolver.unregisterContentObserver(this)
    }
}
