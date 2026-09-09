package com.aegisfocus.app.domain

import com.aegisfocus.app.data.local.entity.SubscriptionEntity
import com.aegisfocus.app.domain.subscription.EntitlementManager
import com.aegisfocus.app.domain.subscription.SubscriptionPlan
import com.aegisfocus.app.data.local.dao.SubscriptionDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** In-memory fake DAO for EntitlementManager tests. */
class FakeSubscriptionDao : SubscriptionDao {
    private var entity: SubscriptionEntity = SubscriptionEntity(planType = "FREE")

    override suspend fun upsertSubscription(sub: SubscriptionEntity) { entity = sub }
    override fun observeSubscription(): Flow<SubscriptionEntity?> = flowOf(entity)
    override suspend fun getSubscription(): SubscriptionEntity = entity
    override suspend fun getPlanType(): String = entity.planType
}

class EntitlementManagerTest {

    private lateinit var dao: FakeSubscriptionDao
    private lateinit var manager: EntitlementManager

    @Before
    fun setup() {
        dao = FakeSubscriptionDao()
        manager = EntitlementManager(dao)
    }

    @Test
    fun `free tier - isPro returns false`() = runTest {
        assertFalse(manager.isPro())
    }

    @Test
    fun `free tier - maxStrictDurationMinutes is 180`() = runTest {
        assertEquals(180, manager.maxStrictDurationMinutes())
    }

    @Test
    fun `free tier - canCommitStrictSession 180 minutes returns true`() = runTest {
        assertTrue(manager.canCommitStrictSession(180))
    }

    @Test
    fun `free tier - canCommitStrictSession 181 minutes returns false`() = runTest {
        assertFalse(manager.canCommitStrictSession(181))
    }

    @Test
    fun `free tier - website blocking disabled`() = runTest {
        assertFalse(manager.canUseWebsiteBlocking())
    }

    @Test
    fun `free tier - notification shield disabled`() = runTest {
        assertFalse(manager.canUseNotificationShield())
    }

    @Test
    fun `lifetime plan - isPro returns true`() = runTest {
        manager.activatePlan(SubscriptionPlan.LIFETIME)
        assertTrue(manager.isPro())
    }

    @Test
    fun `lifetime plan - maxStrictDurationMinutes is unlimited`() = runTest {
        manager.activatePlan(SubscriptionPlan.LIFETIME)
        assertEquals(Int.MAX_VALUE, manager.maxStrictDurationMinutes())
    }

    @Test
    fun `lifetime plan - all pro features enabled`() = runTest {
        manager.activatePlan(SubscriptionPlan.LIFETIME)
        assertTrue(manager.canUseWebsiteBlocking())
        assertTrue(manager.canUseNotificationShield())
        assertTrue(manager.canUseAdaptiveFriction())
        assertTrue(manager.canCreateUnlimitedProfiles())
        assertTrue(manager.canUseAdvancedSchedules())
    }

    @Test
    fun `expired pro plan - isPro returns false`() = runTest {
        val expiredTime = System.currentTimeMillis() - 1000
        manager.activatePlan(SubscriptionPlan.PRO_MONTHLY, expiryEpochMs = expiredTime)
        assertFalse(manager.isPro())
    }

    @Test
    fun `active monthly plan - isPro returns true`() = runTest {
        val futureExpiry = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000
        manager.activatePlan(SubscriptionPlan.PRO_MONTHLY, expiryEpochMs = futureExpiry)
        assertTrue(manager.isPro())
    }

    @Test
    fun `downgradeToFree - isPro becomes false`() = runTest {
        manager.activatePlan(SubscriptionPlan.LIFETIME)
        assertTrue(manager.isPro())
        manager.downgradeToFree()
        assertFalse(manager.isPro())
    }

    @Test
    fun `free tier - maxProfiles is 4`() = runTest {
        assertEquals(4, manager.maxProfiles())
    }

    @Test
    fun `pro tier - maxProfiles is unlimited`() = runTest {
        manager.activatePlan(SubscriptionPlan.LIFETIME)
        assertEquals(Int.MAX_VALUE, manager.maxProfiles())
    }
}
