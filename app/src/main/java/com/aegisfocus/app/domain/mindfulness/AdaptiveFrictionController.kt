package com.aegisfocus.app.domain.mindfulness

import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks per-package block attempt counts within a session window and returns
 * escalating delay durations: 5s → 10s → 20s → 30s → 30s (capped).
 *
 * Thread-safe via ConcurrentHashMap.
 */
class AdaptiveFrictionController {

    private val attemptCounts = ConcurrentHashMap<String, Int>()

    /** The escalating delay schedule in seconds. */
    private val delayScheduleSeconds = listOf(5, 10, 20, 30)

    /**
     * Records an attempt for [packageName] and returns the friction delay in seconds
     * to apply before allowing the user to dismiss the intervention.
     */
    fun recordAttemptAndGetDelay(packageName: String): Int {
        val count = (attemptCounts[packageName] ?: 0) + 1
        attemptCounts[packageName] = count
        val index = (count - 1).coerceIn(0, delayScheduleSeconds.lastIndex)
        return delayScheduleSeconds[index]
    }

    /** Returns the current attempt count for a package (read-only). */
    fun getAttemptCount(packageName: String): Int = attemptCounts[packageName] ?: 0

    /** Resets all friction counters (call on session end). */
    fun reset() = attemptCounts.clear()

    /** Resets friction for a single package. */
    fun resetFor(packageName: String) = attemptCounts.remove(packageName)
}
