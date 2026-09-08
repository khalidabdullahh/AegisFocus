package com.aegisfocus.app.domain.model

data class AppInfo(
    val packageName: String,
    val appName: String,
    val category: AppCategory = AppCategory.OTHER,
    val isBlocked: Boolean = false,
    val isSystemApp: Boolean = false,
    val usageTimeTodayMs: Long = 0L
)

enum class AppCategory(val displayName: String) {
    SOCIAL("Social"),
    ENTERTAINMENT("Entertainment"),
    GAMES("Games"),
    PRODUCTIVITY("Productivity"),
    COMMUNICATION("Communication"),
    OTHER("Other")
}
