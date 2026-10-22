package com.aegisfocus.app.domain.motivation

/**
 * Shield tiers shown on the navigation bar. The tier is derived from the current
 * discipline streak, so the shield visibly upgrades as the user stays consistent.
 */
enum class ShieldTier(
    val title: String,
    val minStreakDays: Int,
    val accentHex: Long
) {
    BRONZE("Bronze", 0, 0xFFC08A5A),
    SILVER("Silver", 4, 0xFFB8C4CC),
    GOLD("Gold", 14, 0xFFD8B574),
    PLATINUM("Platinum", 30, 0xFF8FC9D6),
    DIAMOND("Diamond", 60, 0xFF9FB4F0);

    /** The tier after this one, or null when this is the highest tier. */
    val next: ShieldTier?
        get() = entries.getOrNull(ordinal + 1)

    companion object {
        fun fromStreak(days: Int): ShieldTier =
            entries.lastOrNull { days >= it.minStreakDays } ?: BRONZE
    }
}

/** Progress of the current streak towards the next shield tier. */
data class ShieldProgress(
    val tier: ShieldTier,
    val streakDays: Int,
    /** 0f..1f progress within the current tier. 1f when the top tier is reached. */
    val fraction: Float,
    val daysUntilNextTier: Int
) {
    companion object {
        fun fromStreak(streakDays: Int): ShieldProgress {
            val days = streakDays.coerceAtLeast(0)
            val tier = ShieldTier.fromStreak(days)
            val next = tier.next
            return if (next == null) {
                ShieldProgress(tier, days, 1f, 0)
            } else {
                val span = (next.minStreakDays - tier.minStreakDays).coerceAtLeast(1)
                val done = (days - tier.minStreakDays).coerceIn(0, span)
                ShieldProgress(tier, days, done.toFloat() / span, next.minStreakDays - days)
            }
        }
    }
}
