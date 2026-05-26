package com.mnemo.scheduling

import android.content.Context
import androidx.work.*
import com.mnemo.di.AppModule
import com.mnemo.graph.GraphAnalytics
import com.mnemo.notification.NotificationHelper
import com.mnemo.util.DateUtils
import java.util.concurrent.TimeUnit

class MorningNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!DateUtils.shouldSendMorningNotification()) return Result.success()

        val app = AppModule.getInstance(applicationContext)
        val screenshotRepo = app.screenshotRepository
        val analytics = app.graphAnalytics

        val since = DateUtils.millisSince(2) // last 48 hours
        val unreviewed = screenshotRepo.getUnreviewedSince(since)
        if (unreviewed.isEmpty()) return Result.success()

        val communityLabels = analytics.getCommunityLabels(
            screenshotRepo.getAll()
        )
        val summary = unreviewed
            .groupBy { it.communityId }
            .mapKeys { (communityId, _) ->
                communityLabels[communityId] ?: "Group $communityId"
            }
            .mapValues { (_, items) -> items.size }

        NotificationHelper.sendDigestNotification(applicationContext, summary)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "morning_notification"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<MorningNotificationWorker>(
                24, TimeUnit.HOURS,
                1, TimeUnit.HOURS
            ).setConstraints(
                Constraints.Builder().setRequiresBatteryNotLow(false).build()
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
