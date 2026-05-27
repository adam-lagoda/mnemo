package com.lagoda.mnemo.util

import java.util.Calendar

object DateUtils {
    fun isWeekday(timeMillis: Long = System.currentTimeMillis()): Boolean {
        val cal = Calendar.getInstance().apply { timeInMillis = timeMillis }
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        return dow != Calendar.SATURDAY && dow != Calendar.SUNDAY
    }

    fun isMorningWindow(
        timeMillis: Long = System.currentTimeMillis(),
        startHour: Int = 7,
        endHour: Int = 9
    ): Boolean {
        val cal = Calendar.getInstance().apply { timeInMillis = timeMillis }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        return hour in startHour until endHour
    }

    fun shouldSendMorningNotification(): Boolean =
        isWeekday() && isMorningWindow()

    fun millisSince(daysAgo: Int): Long =
        System.currentTimeMillis() - daysAgo * 24L * 60 * 60 * 1000
}
