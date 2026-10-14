package com.aegisfocus.app.domain.motivation

enum class ShieldTier(
    val title: String,
    val minStreakDays: Int,
    val iconName: String,
    val accentHex: Long,
    val description: String
) {
    BRONZE("Bronze Aegis", 0, "shield", 0xFFCD7F32, "Initial shield. Fragile against temptations."),
    SILVER("Silver Aegis", 4, "shield-check", 0xFFC0C0C0, "Tempered resilience against impulsive reflexes."),
    GOLD("Gold Aegis", 14, "award", 0xFFFFD700, "Unbreakable digital discipline and clarity."),
    OBSIDIAN("Obsidian Aegis", 30, "shield-alert", 0xFF9333EA, "Monk Mode Master. Indomitable will.");

    companion object {
        fun fromStreak(days: Int): ShieldTier {
            return when {
                days >= OBSIDIAN.minStreakDays -> OBSIDIAN
                days >= GOLD.minStreakDays -> GOLD
                days >= SILVER.minStreakDays -> SILVER
                else -> BRONZE
            }
        }
    }
}

data class MonkShieldStatus(
    val tier: ShieldTier,
    val streakDays: Int,
    val isCracked: Boolean,
    val healthPercent: Int,
    val daysUntilNextTier: Int
)

class MonkShieldManager(
    private val xpManager: XpManager
) {
    fun calculateShield(streakDays: Int, recentViolations: Int): MonkShieldStatus {
        val tier = ShieldTier.fromStreak(streakDays)
        val isCracked = recentViolations > 0
        val healthPercent = (100 - (recentViolations * 25)).coerceIn(10, 100)
        
        val daysUntilNext = when (tier) {
            ShieldTier.BRONZE -> (ShieldTier.SILVER.minStreakDays - streakDays).coerceAtLeast(1)
            ShieldTier.SILVER -> (ShieldTier.GOLD.minStreakDays - streakDays).coerceAtLeast(1)
            ShieldTier.GOLD -> (ShieldTier.OBSIDIAN.minStreakDays - streakDays).coerceAtLeast(1)
            ShieldTier.OBSIDIAN -> 0
        }

        return MonkShieldStatus(
            tier = tier,
            streakDays = streakDays,
            isCracked = isCracked,
            healthPercent = healthPercent,
            daysUntilNextTier = daysUntilNext
        )
    }
}
