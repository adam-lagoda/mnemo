package com.mnemo

import android.app.Application
import android.os.BatteryManager
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
        NotificationHelper.createIndexingChannel(this)
        appModule.seedEmbeddingCorpus()
        screenshotMonitor = ScreenshotMonitor(
            context = this,
            appConfig = appModule.appConfig,
            batteryManager = getSystemService(BatteryManager::class.java)
        ).also { it.register() }
        MorningNotificationWorker.schedule(this)
    }

    override fun onTerminate() {
        super.onTerminate()
        screenshotMonitor.unregister()
    }
}
