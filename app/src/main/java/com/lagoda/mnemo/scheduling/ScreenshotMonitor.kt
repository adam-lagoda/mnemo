package com.lagoda.mnemo.scheduling

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.work.*
import com.lagoda.mnemo.data.prefs.AppConfig
import java.util.concurrent.TimeUnit

class ScreenshotMonitor(
    private val context: Context,
    private val appConfig: AppConfig,
    private val batteryManager: BatteryManager
) : ContentObserver(Handler(Looper.getMainLooper())) {

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        if (!appConfig.autoWatchEnabled) return
        if (batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) < 30) return
        if (uri == null || !isInConfiguredFolder(uri)) return

        val request = OneTimeWorkRequestBuilder<ExtractionWorker>()
            .setInitialDelay(5, TimeUnit.SECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(false)
                    .build()
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            ExtractionWorker.WORK_NAME,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request
        )
    }

    private fun isInConfiguredFolder(uri: Uri): Boolean {
        val requiredPath = appConfig.relativePath ?: return false
        return context.contentResolver.query(
            uri,
            arrayOf(MediaStore.Images.Media.RELATIVE_PATH),
            null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                (cursor.getString(0) ?: return false).startsWith(requiredPath)
            } else false
        } ?: false
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
