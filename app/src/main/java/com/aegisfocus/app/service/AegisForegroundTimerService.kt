package com.aegisfocus.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.aegisfocus.app.R
import com.aegisfocus.app.core.common.TimeUtils
import com.aegisfocus.app.presentation.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AegisForegroundTimerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private var updateJob: Job? = null
    private var endEpochMs: Long = 0L
    private var sessionName: String = "Focus Session"
    private var isStrict: Boolean = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TIMER -> {
                sessionName = intent.getStringExtra(EXTRA_SESSION_NAME) ?: "Focus Session"
                endEpochMs = intent.getLongExtra(EXTRA_END_EPOCH_MS, System.currentTimeMillis())
                isStrict = intent.getBooleanExtra(EXTRA_IS_STRICT, false)

                startForegroundNotification()
                startTicker()
            }
            ACTION_STOP_TIMER -> {
                stopForegroundNotification()
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundNotification() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startTicker() {
        updateJob?.cancel()
        updateJob = serviceScope.launch {
            while (isActive) {
                val remaining = endEpochMs - System.currentTimeMillis()
                if (remaining <= 0) {
                    stopForegroundNotification()
                    break
                }
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, buildNotification())
                delay(1000L)
            }
        }
    }

    private fun buildNotification(): Notification {
        val remaining = (endEpochMs - System.currentTimeMillis()).coerceAtLeast(0L)
        val formattedTime = TimeUtils.formatRemainingMillis(remaining)
        val modeLabel = if (isStrict) "⚡ Strict Mode" else "🌱 Normal Mode"

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("$sessionName ($modeLabel)")
            .setContentText("Remaining: $formattedTime • Stay disciplined")
            .setSmallIcon(R.drawable.ic_aegis_shield)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun stopForegroundNotification() {
        updateJob?.cancel()
        updateJob = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Active Focus Sessions",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live focus countdown timer"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        updateJob?.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START_TIMER = "com.aegisfocus.app.START_TIMER"
        const val ACTION_STOP_TIMER = "com.aegisfocus.app.STOP_TIMER"
        const val EXTRA_SESSION_NAME = "extra_session_name"
        const val EXTRA_END_EPOCH_MS = "extra_end_epoch_ms"
        const val EXTRA_IS_STRICT = "extra_is_strict"

        private const val CHANNEL_ID = "aegis_focus_timer_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
