package com.aegisfocus.app.domain.subscription

/** All available subscription tiers. */
enum class SubscriptionPlan(val planId: String, val displayName: String) {
    FREE("free", "Free"),
    PRO_1_MONTH("pro_1m", "Pro (1 Month)"),
    PRO_3_MONTHS("pro_3m", "Pro (3 Months)"),
    PRO_6_MONTHS("pro_6m", "Pro (6 Months)"),
    PRO_12_MONTHS("pro_12m", "Pro (12 Months)"),
    PRO_MONTHLY("pro_monthly", "Pro Monthly"),
    PRO_YEARLY("pro_yearly", "Pro Yearly"),
    LIFETIME("lifetime", "Lifetime Pro"),
    STUDENT_YEARLY("student_yearly", "Student Pro")
}
