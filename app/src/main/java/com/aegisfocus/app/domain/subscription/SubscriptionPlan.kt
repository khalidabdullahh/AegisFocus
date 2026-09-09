package com.aegisfocus.app.domain.subscription

/** All available subscription tiers. */
enum class SubscriptionPlan(val planId: String, val displayName: String) {
    FREE("free", "Free"),
    PRO_MONTHLY("pro_monthly", "Pro Monthly"),
    PRO_YEARLY("pro_yearly", "Pro Yearly"),
    LIFETIME("lifetime", "Lifetime Pro"),
    STUDENT_YEARLY("student_yearly", "Student Pro")
}
