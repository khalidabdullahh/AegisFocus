/*
 * ==============================================================================
 * Project: ASR / Aegis-Focus
 * File: AegisApplication.kt
 * Description: Main Android Application class initializing DI container,
 *              notification channels, and background periodic WorkManager tasks.
 * ==============================================================================
 */
package com.aegisfocus.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.aegisfocus.app.core.di.AppContainer
import com.aegisfocus.app.core.di.ServiceLocator
import com.aegisfocus.app.service.worker.DailyUsageSnapshotWorker
import com.aegisfocus.app.service.worker.ScheduleCheckWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class AegisApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = ServiceLocator.getContainer(this)
        createNotificationChannels()
        enqueueBackgroundWorkers()

        // Restore active focus session on application startup
        CoroutineScope(Dispatchers.IO).launch {
            container.focusSessionManager.restoreActiveSession()
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java) ?: return

            // Channel 1: Persistent Active Focus Timer
            val timerChannel = NotificationChannel(
                CHANNEL_TIMER_ID,
                getString(R.string.notification_channel_timer_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_timer_desc)
                setShowBadge(false)
            }

            // Channel 2: Reminders and Discipline Summaries
            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDERS_ID,
                getString(R.string.notification_channel_reminders_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.notification_channel_reminders_desc)
            }

            notificationManager.createNotificationChannels(listOf(timerChannel, reminderChannel))
        }
    }

    private fun enqueueBackgroundWorkers() {
        val workManager = WorkManager.getInstance(this)

        val scheduleCheckRequest = PeriodicWorkRequestBuilder<ScheduleCheckWorker>(
            15, TimeUnit.MINUTES
        ).build()

        workManager.enqueueUniquePeriodicWork(
            "ScheduleCheckWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            scheduleCheckRequest
        )

        val dailySnapshotRequest = PeriodicWorkRequestBuilder<DailyUsageSnapshotWorker>(
            1, TimeUnit.DAYS
        ).build()

        workManager.enqueueUniquePeriodicWork(
            "DailyUsageSnapshotWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            dailySnapshotRequest
        )
    }

    companion object {
        const val CHANNEL_TIMER_ID = "aegis_focus_timer_channel"
        const val CHANNEL_REMINDERS_ID = "aegis_focus_reminders_channel"
    }
}
