package com.aegisfocus.app.engine

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ForegroundAppMonitor(
    private val context: Context,
    private val interventionController: BlockInterventionController,
    private val blockingRuleRepository: BlockingRuleRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private var pollingJob: Job? = null
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

    /**
     * Called directly by AegisAccessibilityBlockerService whenever the active window changes.
     */
    fun onAccessibilityEvent(packageName: String) {
        interventionController.onForegroundAppDetected(packageName)
    }

    /**
     * Starts a background polling monitor as a fallback if accessibility service is unavailable or pending.
     */
    fun startFallbackPolling() {
        if (pollingJob?.isActive == true) return
        pollingJob = scope.launch {
            while (isActive) {
                if (blockingRuleRepository.activeBlockedPackages.value.isNotEmpty() && usageStatsManager != null) {
                    val now = System.currentTimeMillis()
                    val events = try {
                        usageStatsManager.queryEvents(now - 3000L, now)
                    } catch (e: Exception) {
                        null
                    }

                    if (events != null) {
                        val event = UsageEvents.Event()
                        var lastPackage: String? = null
                        while (events.hasNextEvent()) {
                            events.getNextEvent(event)
                            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                                event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
                            ) {
                                lastPackage = event.packageName
                            }
                        }
                        if (lastPackage != null && lastPackage != context.packageName) {
                            interventionController.onForegroundAppDetected(lastPackage)
                        }
                    }
                }
                delay(1000L)
            }
        }
    }

    fun stopFallbackPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }
}
