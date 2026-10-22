package com.aegisfocus.app.domain.model

data class UsageStatsSummary(
    val date: String,
    val totalScreenTimeMs: Long = 0L,
    val totalFocusTimeMs: Long = 0L,
    val sessionsCompleted: Int = 0,
    val streakDays: Int = 0,
    val topDistractingApps: List<AppUsageStat> = emptyList(),
    val hourlyFocusDistribution: List<HourlyDistribution> = emptyList(),
    /** True when Android usage access returned real data for this period. */
    val hasScreenTimeData: Boolean = false
) {
    /**
     * Share of tracked time spent in focus sessions, 0..100.
     * Null when there is nothing real to base it on.
     */
    val productivityScore: Int?
        get() {
            val total = totalScreenTimeMs + totalFocusTimeMs
            if (total <= 0L || totalFocusTimeMs <= 0L) return null
            return ((totalFocusTimeMs.toFloat() / total) * 100f).toInt().coerceIn(0, 100)
        }
}

data class AppUsageStat(
    val packageName: String,
    val appName: String,
    val usageTimeMs: Long,
    val isBlockedDuringFocus: Boolean = true
)

data class HourlyDistribution(
    val hourOfDay: Int,
    val focusMinutes: Int
)
