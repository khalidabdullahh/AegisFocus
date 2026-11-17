package com.aegisfocus.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.aegisfocus.app.AegisApplication
import com.aegisfocus.app.R
import com.aegisfocus.app.engine.CampfireAudioEngine
import com.aegisfocus.app.presentation.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

/**
 * Foreground service for Campfire. It does not play audio itself (the engine does); it keeps the
 * process alive while sound plays with the screen off and shows play/pause/stop controls.
 */
class CampfireAudioService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var observeJob: Job? = null

    private val engine: CampfireAudioEngine
        get() = (application as AegisApplication).container.campfireEngine

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) engine.pause()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        registerReceiver(noisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Must promote to foreground right away when started with startForegroundService()
        startForegroundCompat(buildNotification(engine.state.value))

        when (intent?.action) {
            ACTION_TOGGLE -> engine.toggle()
            ACTION_STOP -> {
                engine.pause()
                shutdown(removeNotification = true)
                return START_NOT_STICKY
            }
        }
        observe()
        return START_NOT_STICKY
    }

    private fun observe() {
        observeJob?.cancel()
        observeJob = scope.launch {
            val ticker = flow { while (true) { emit(Unit); delay(30_000L) } }
            combine(engine.state, ticker) { s, _ -> s }.collect { s ->
                if (!s.isPlaying) {
                    // Leave a dismissible "paused" notification with a Play button
                    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    nm.notify(NOTIFICATION_ID, buildNotification(s))
                    shutdown(removeNotification = false)
                } else {
                    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    nm.notify(NOTIFICATION_ID, buildNotification(s))
                }
            }
        }
    }

    private fun shutdown(removeNotification: Boolean) {
        observeJob?.cancel()
        stopForeground(if (removeNotification) STOP_FOREGROUND_REMOVE else STOP_FOREGROUND_DETACH)
        stopSelf()
    }

    private fun startForegroundCompat(n: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, n)
        }
    }

    private fun buildNotification(s: CampfireAudioEngine.State): Notification {
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val remainingMin = if (s.timerEndEpochMs > 0)
            ((s.timerEndEpochMs - System.currentTimeMillis()) / 60_000L + 1).coerceAtLeast(1) else 0L
        val status = when {
            !s.isPlaying -> "Paused"
            remainingMin > 0 -> "Playing • stops in ${remainingMin} min"
            else -> "Playing • ${s.levels.size} sounds"
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_aegis_shield)
            .setContentTitle("Campfire • ${s.mode.label}")
            .setContentText(status)
            .setContentIntent(open)
            .setOnlyAlertOnce(true)
            .setOngoing(s.isPlaying)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        builder.addAction(
            if (s.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
            if (s.isPlaying) "Pause" else "Play",
            servicePendingIntent(ACTION_TOGGLE, 1)
        )
        builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", servicePendingIntent(ACTION_STOP, 2))
        return builder.build()
    }

    private fun servicePendingIntent(action: String, code: Int): PendingIntent {
        val i = Intent(this, CampfireAudioService::class.java).setAction(action)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            PendingIntent.getForegroundService(this, code, i, flags)
        else PendingIntent.getService(this, code, i, flags)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java) ?: return
            val ch = NotificationChannel(CHANNEL_ID, "Campfire sounds", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Controls for Campfire sleep and study sounds"
                setShowBadge(false)
            }
            nm.createNotificationChannel(ch)
        }
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(noisyReceiver) }
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.aegisfocus.app.campfire.START"
        const val ACTION_TOGGLE = "com.aegisfocus.app.campfire.TOGGLE"
        const val ACTION_STOP = "com.aegisfocus.app.campfire.STOP"
        private const val CHANNEL_ID = "campfire_audio"
        private const val NOTIFICATION_ID = 4107
    }
}
