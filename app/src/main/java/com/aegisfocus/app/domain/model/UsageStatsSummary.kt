package com.aegisfocus.app.domain.model

data class UsageStatsSummary(
    val date: String,
    val totalScreenTimeMs: Long = 0L,
    val totalFocusTimeMs: Long = 0L,
    val sessionsCompleted: Int = 0,
    val streakDays: Int = 0,
    val topDistractingApps: List<AppUsageStat> = emptyList(),
    val hourlyFocusDistribution: List<HourlyDistribution> = emptyList()
) {
    val productivityScore: Int
        get() {
            if (totalScreenTimeMs == 0L && totalFocusTimeMs == 0L) return 80
            val totalTime = (totalScreenTimeMs + totalFocusTimeMs).toFloat()
            if (totalTime == 0f) return 80
            val focusRatio = (totalFocusTimeMs.toFloat() / totalTime) * 100f
            // Base score influenced by focus ratio and streak bonus
            val calculated = (focusRatio * 0.7f + (streakDays * 5f).coerceAtMost(30f)).toInt()
            return calculated.coerceIn(10, 100)
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
