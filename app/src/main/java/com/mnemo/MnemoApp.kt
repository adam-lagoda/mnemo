package com.mnemo

import android.app.Application
import com.mnemo.di.AppModule
import com.mnemo.notification.NotificationHelper
import com.mnemo.scheduling.MorningNotificationWorker
import com.mnemo.scheduling.ScreenshotMonitor

class MnemoApp : Application() {
    private lateinit var screenshotMonitor: ScreenshotMonitor

    override fun onCreate() {
        super.onCreate()
        val appModule = AppModule.getInstance(this)
        NotificationHelper.createChannel(this)
        appModule.seedEmbeddingCorpus()
        screenshotMonitor = ScreenshotMonitor(this).also { it.register() }
        MorningNotificationWorker.schedule(this)
    }

    override fun onTerminate() {
        super.onTerminate()
        screenshotMonitor.unregister()
    }
}
