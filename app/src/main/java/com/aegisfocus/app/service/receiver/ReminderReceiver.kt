package com.aegisfocus.app.service.receiver

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.aegisfocus.app.AegisApplication
import com.aegisfocus.app.R
import com.aegisfocus.app.data.local.ProductivityStore

/** Shows a reminder notification and queues the same reminder for the next day. */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        if (id == -1L) return

        val reminder = ProductivityStore(context).getReminder(id) ?: return
        if (!reminder.enabled) return

        ensureChannel(context)

        val canNotify = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

        if (canNotify) {
            val notification = NotificationCompat.Builder(context, AegisApplication.CHANNEL_REMINDERS_ID)
                .setSmallIcon(R.drawable.ic_aegis_shield)
                .setContentTitle("Reminder")
                .setContentText(reminder.title)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()
            try {
                NotificationManagerCompat.from(context).notify(NOTIFICATION_OFFSET + (id % 100_000L).toInt(), notification)
            } catch (e: SecurityException) {
                // Notification permission revoked between the check and the call.
            }
        }

        ReminderScheduler.schedule(context, reminder)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            if (manager.getNotificationChannel(AegisApplication.CHANNEL_REMINDERS_ID) == null) {
                manager.createNotificationChannel(
                    NotificationChannel(
                        AegisApplication.CHANNEL_REMINDERS_ID,
                        "Reminders",
                        NotificationManager.IMPORTANCE_DEFAULT
                    )
                )
            }
        }
    }

    companion object {
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        private const val NOTIFICATION_OFFSET = 80_000
    }
}
