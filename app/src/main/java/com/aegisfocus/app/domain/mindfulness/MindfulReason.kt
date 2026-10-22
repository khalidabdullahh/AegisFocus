package com.aegisfocus.app.domain.mindfulness

/** Reason categories the user selects when a mindful pause prompt appears. */
enum class MindfulReason(val displayLabel: String) {
    IMPORTANT("It's important"),
    JUST_CHECKING("Just checking"),
    BORED("I'm bored"),
    HABIT("Out of habit")
}
