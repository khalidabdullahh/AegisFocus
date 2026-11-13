package com.aegisfocus.app.service

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.aegisfocus.app.data.local.AppLimitStore
import com.aegisfocus.app.engine.AppUsageTracker
import com.aegisfocus.app.presentation.limits.LimitReachedActivity

/**
 * Counts how often limited apps are opened and watches their daily time.
 * When a limit is reached the "limit reached" screen is shown over the app.
 */
class AppLimitController(
    private val context: Context,
    private val store: AppLimitStore,
    private val tracker: AppUsageTracker
) {
    private val main = Handler(Looper.getMainLooper())
    private var lastForegroundPkg: String? = null
    private val leftAt = HashMap<String, Long>()
    private var watching: String? = null
    private var lastLaunchMs = 0L

    private val tick = object : Runnable {
        override fun run() {
            val pkg = watching ?: return
            if (checkAndBlock(pkg)) {
                watching = null
                return
            }
            main.postDelayed(this, CHECK_INTERVAL_MS)
        }
    }

    /** Call when [packageName] comes to the foreground. Returns true if the limit screen was shown. */
    fun onForeground(packageName: String): Boolean {
        val now = SystemClock.elapsedRealtime()
        if (packageName != lastForegroundPkg) {
            lastForegroundPkg?.let { leftAt[it] = now }
            val limit = store.getLimit(packageName)
            if (limit != null && limit.enabled) {
                val gone = leftAt[packageName]
                if (gone == null || now - gone > REOPEN_GAP_MS) store.recordOpen(packageName)
            }
            lastForegroundPkg = packageName
        }

        main.removeCallbacks(tick)
        watching = null

        val limit = store.getLimit(packageName)?.takeIf { it.enabled } ?: return false
        if (checkAndBlock(packageName)) return true
        if (limit.dailyMinutes > 0) {
            watching = packageName
            main.postDelayed(tick, CHECK_INTERVAL_MS)
        }
        return false
    }

    fun release() {
        main.removeCallbacks(tick)
        watching = null
    }

    private fun checkAndBlock(packageName: String): Boolean {
        val limit = store.getLimit(packageName)?.takeIf { it.enabled } ?: return false

        val reason: String? = when {
            limit.maxOpens > 0 && store.opensToday(packageName) > limit.maxOpens ->
                "You have already opened ${limit.appName} ${limit.maxOpens} times today."
            limit.dailyMinutes > 0 && tracker.hasUsageAccess() &&
                tracker.todayUsageMs(packageName) >= limit.dailyMinutes * 60_000L ->
                "You have used ${limit.appName} for ${limit.dailyMinutes} minutes today."
            else -> null
        }
        if (reason == null) return false

        val now = SystemClock.elapsedRealtime()
        if (now - lastLaunchMs > 1500L) {
            lastLaunchMs = now
            val intent = Intent(context, LimitReachedActivity::class.java).apply {
                putExtra(LimitReachedActivity.EXTRA_APP_NAME, limit.appName)
                putExtra(LimitReachedActivity.EXTRA_REASON, reason)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
        return true
    }

    companion object {
        private const val CHECK_INTERVAL_MS = 15_000L
        private const val REOPEN_GAP_MS = 8_000L
    }
}
