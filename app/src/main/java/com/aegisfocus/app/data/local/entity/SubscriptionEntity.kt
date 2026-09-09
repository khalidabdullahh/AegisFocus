package com.aegisfocus.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores the active subscription state. Singleton row (id=1).
 * planType: FREE | PRO_MONTHLY | PRO_YEARLY | LIFETIME | STUDENT_YEARLY
 */
@Entity(tableName = "subscription_state")
data class SubscriptionEntity(
    @PrimaryKey val id: Int = 1,
    val planType: String = "FREE",
    val expiryEpochMs: Long = 0L,          // 0 = no expiry (Lifetime) or Free tier
    val purchaseToken: String = "",         // Google Play purchase token (opaque, stored locally)
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)
