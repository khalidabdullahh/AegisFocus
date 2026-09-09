package com.aegisfocus.app.domain.motivation

/** XP-gated discipline levels with display metadata. */
enum class DisciplineLevel(
    val levelId: String,
    val displayName: String,
    val minXp: Int,
    val maxXp: Int,
    val emoji: String,
    val description: String
) {
    NEW_STARTER(
        "NEW_STARTER", "New Starter", 0, 499, "🌱",
        "Beginning your discipline journey"
    ),
    CONSISTENT(
        "CONSISTENT", "Consistent", 500, 1_499, "⚡",
        "Building reliable focus habits"
    ),
    FOCUSED(
        "FOCUSED", "Focused", 1_500, 3_499, "🎯",
        "Mastering intentional attention"
    ),
    DISCIPLINED(
        "DISCIPLINED", "Disciplined", 3_500, 7_499, "🔥",
        "Disciplined mind, deliberate action"
    ),
    IRON_WILL(
        "IRON_WILL", "Iron Will", 7_500, Int.MAX_VALUE, "🏆",
        "Unbreakable. The highest tier."
    );

    companion object {
        fun fromXp(xp: Int): DisciplineLevel =
            values().lastOrNull { xp >= it.minXp } ?: NEW_STARTER

        fun xpToNextLevel(xp: Int): Int? {
            val next = values().firstOrNull { xp < it.minXp } ?: return null
            return next.minXp - xp
        }
    }
}
