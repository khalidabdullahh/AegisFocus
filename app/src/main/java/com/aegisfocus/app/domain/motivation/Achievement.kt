package com.aegisfocus.app.domain.motivation

/** Catalogue of all earnable achievements. */
enum class Achievement(
    val achievementId: String,
    val title: String,
    val description: String,
    val xpReward: Int
) {
    FIRST_FOCUS("FIRST_FOCUS", "First Focus", "Complete your first focus session", 50),
    FIRST_STRICT("FIRST_STRICT", "Strict Commitment", "Complete a Strict Mode session", 100),
    STREAK_3("STREAK_3", "3-Day Streak", "Maintain a 3-day focus streak", 75),
    STREAK_7("STREAK_7", "Week Warrior", "Maintain a 7-day focus streak", 150),
    STREAK_14("STREAK_14", "Fortnight Focus", "Maintain a 14-day focus streak", 300),
    STREAK_30("STREAK_30", "Iron Month", "30-day unbroken focus streak", 600),
    GOAL_MET_FIRST("GOAL_MET_FIRST", "Goal Crusher", "Meet your daily goal for the first time", 50),
    NO_DISTRACTIONS("NO_DISTRACTIONS", "Laser Focus", "Complete a session with 0 block attempts", 100),
    IRON_WILL_REACHED("IRON_WILL", "Iron Will", "Reach the Iron Will level (7500 XP)", 500)
}
