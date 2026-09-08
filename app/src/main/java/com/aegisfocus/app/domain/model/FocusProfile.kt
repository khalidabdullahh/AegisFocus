package com.aegisfocus.app.domain.model

data class FocusProfile(
    val id: Long = 0,
    val name: String,
    val iconName: String = "shield",
    val defaultDurationMinutes: Int = 25,
    val isStrictModeDefault: Boolean = false,
    val blockedPackageNames: List<String> = emptyList(),
    val isDefault: Boolean = false
)
