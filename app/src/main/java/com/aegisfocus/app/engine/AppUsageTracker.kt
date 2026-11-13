package com.aegisfocus.app.engine

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import java.time.LocalDate
import java.time.ZoneId

/** Reads how long an app has been in the foreground today (needs Usage Access). */
class AppUsageTracker(private val context: Context) {

    fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Foreground time of [packageName] since midnight, in milliseconds. */
    fun todayUsageMs(packageName: String): Long {
        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return 0L
        val start = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val end = System.currentTimeMillis()
        return try {
            val events = manager.queryEvents(start, end)
            val event = UsageEvents.Event()
            var foregroundSince = 0L
            var total = 0L
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.packageName != packageName) continue
                @Suppress("DEPRECATION")
                when (event.eventType) {
                    UsageEvents.Event.MOVE_TO_FOREGROUND -> if (foregroundSince == 0L) foregroundSince = event.timeStamp
                    UsageEvents.Event.MOVE_TO_BACKGROUND -> if (foregroundSince != 0L) {
                        total += event.timeStamp - foregroundSince
                        foregroundSince = 0L
                    }
                }
            }
            if (foregroundSince != 0L) total += end - foregroundSince
            total
        } catch (e: Exception) {
            0L
        }
    }
}
