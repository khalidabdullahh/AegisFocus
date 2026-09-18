package com.aegisfocus.app.domain.subscription

import com.aegisfocus.app.data.local.dao.SubscriptionDao
import com.aegisfocus.app.data.local.entity.SubscriptionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Centralized entitlement gatekeeper. All feature-gate queries go through this class.
 * Never scatter raw isPro checks across the codebase — always query a named capability here.
 */
class EntitlementManager(private val subscriptionDao: SubscriptionDao) {

    /** Observe active plan as a Flow for reactive UI updates. */
    val activePlan: Flow<SubscriptionPlan> = subscriptionDao.observeSubscription().map { entity ->
        entity?.toSubscriptionPlan() ?: SubscriptionPlan.FREE
    }

    /** Returns true if the user has any active paid plan (including non-expired trials). */
    suspend fun isPro(): Boolean {
        val entity = subscriptionDao.getSubscription() ?: return false
        val plan = entity.toSubscriptionPlan()
        if (plan == SubscriptionPlan.FREE) return false
        // Lifetime has no expiry
        if (plan == SubscriptionPlan.LIFETIME) return true
        // Check expiry for time-bound plans
        return entity.expiryEpochMs == 0L || entity.expiryEpochMs > System.currentTimeMillis()
    }

    // ----- Strict Mode & App Blocking Limits -----

    /** Maximum strict mode session duration in minutes. Free = 180 (3h). Pro = Int.MAX_VALUE (unlimited). */
    suspend fun maxStrictDurationMinutes(): Int = if (isPro()) Int.MAX_VALUE else 180

    /** Returns the selectable strict mode presets for the current tier. */
    suspend fun strictModePresets(): List<Int> = if (isPro()) {
        listOf(15, 30, 60, 90, 120, 180, 240, 360, 720) // minutes: 15m..12h
    } else {
        listOf(15, 30, 60, 90, 120, 180) // Free: max 3h (180 min)
    }

    /** Maximum number of blocked apps in free tier vs pro tier. Free = 3, Pro = unlimited. */
    suspend fun maxBlockedApps(): Int = if (isPro()) Int.MAX_VALUE else 3

    /** Whether the user can block additional apps beyond the current count. */
    suspend fun canBlockMoreApps(currentCount: Int): Boolean {
        return currentCount < maxBlockedApps()
    }

    /** Whether the user can commit a strict session of the given duration. */
    suspend fun canCommitStrictSession(durationMinutes: Int): Boolean {
        return durationMinutes <= maxStrictDurationMinutes()
    }

    // ----- Pro Features -----

    suspend fun canUseAdvancedStrictMode(): Boolean = isPro()
    suspend fun canUseWebsiteBlocking(): Boolean = isPro()
    suspend fun canUseNotificationShield(): Boolean = isPro()
    suspend fun canUseAdaptiveFriction(): Boolean = isPro()
    suspend fun canCreateUnlimitedProfiles(): Boolean = isPro()
    suspend fun canUseAdvancedSchedules(): Boolean = isPro()
    suspend fun canViewDeepAnalytics(): Boolean = isPro()

    /** Maximum number of focus profiles for this tier. Free = 4, Pro = unlimited. */
    suspend fun maxProfiles(): Int = if (isPro()) Int.MAX_VALUE else 4

    /** Persist a new subscription state (called after Play Billing confirms purchase). */
    suspend fun activatePlan(plan: SubscriptionPlan, expiryEpochMs: Long = 0L, token: String = "") {
        subscriptionDao.upsertSubscription(
            SubscriptionEntity(
                planType = plan.name,
                expiryEpochMs = expiryEpochMs,
                purchaseToken = token,
                updatedAtEpochMs = System.currentTimeMillis()
            )
        )
    }

    /** Downgrade to free tier. */
    suspend fun downgradeToFree() {
        subscriptionDao.upsertSubscription(
            SubscriptionEntity(planType = SubscriptionPlan.FREE.name)
        )
    }

    private fun SubscriptionEntity.toSubscriptionPlan(): SubscriptionPlan {
        return SubscriptionPlan.values().firstOrNull { it.name == planType } ?: SubscriptionPlan.FREE
    }
}
