package com.aegisfocus.app.domain

import com.aegisfocus.app.data.local.entity.FocusEventEntity
import com.aegisfocus.app.data.local.dao.FocusEventDao
import com.aegisfocus.app.domain.mindfulness.AdaptiveFrictionController
import com.aegisfocus.app.domain.mindfulness.MindfulReason
import com.aegisfocus.app.domain.mindfulness.MindfulnessEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

// ─── Fake DAO ────────────────────────────────────────────────────────────────

class FakeFocusEventDaoForMindfulness : FocusEventDao {
    val insertedEvents = mutableListOf<FocusEventEntity>()

    override suspend fun insertEvent(event: FocusEventEntity) { insertedEvents.add(event) }
    override suspend fun getEventsForSession(sessionId: Long): List<FocusEventEntity> =
        insertedEvents.filter { it.sessionId == sessionId }
    override fun getRecentEvents(sinceEpochMs: Long): Flow<List<FocusEventEntity>> =
        flowOf(insertedEvents.toList())
    override suspend fun countBlockAttemptsAfter(sinceEpochMs: Long): Int =
        insertedEvents.count { it.eventType == "BLOCK_ATTEMPT" }
    override suspend fun countEmergencyExitsAfter(sinceEpochMs: Long): Int =
        insertedEvents.count { it.eventType == "EMERGENCY_EXIT" }
    override suspend fun pruneOldEvents(beforeEpochMs: Long) {
        insertedEvents.removeAll { it.epochMs < beforeEpochMs }
    }
}

// ─── MindfulnessEngine Tests ─────────────────────────────────────────────────

class MindfulnessEngineTest {

    private lateinit var eventDao: FakeFocusEventDaoForMindfulness
    private lateinit var frictionController: AdaptiveFrictionController
    private lateinit var engine: MindfulnessEngine

    @Before
    fun setup() {
        eventDao = FakeFocusEventDaoForMindfulness()
        frictionController = AdaptiveFrictionController()
        engine = MindfulnessEngine(eventDao, frictionController)
    }

    @Test
    fun `initial pause state is inactive`() {
        assertFalse(engine.pauseState.value.isActive)
    }

    @Test
    fun `initial packageName is empty string`() {
        assertEquals("", engine.pauseState.value.packageName)
    }

    @Test
    fun `selectReasonAndComplete - logs MINDFUL_PAUSE event with correct type`() = runTest {
        // Call selectReasonAndComplete directly to test event logging path
        engine.selectReasonAndComplete(MindfulReason.BORED)
        assertEquals(1, eventDao.insertedEvents.size)
        assertEquals("MINDFUL_PAUSE", eventDao.insertedEvents[0].eventType)
    }

    @Test
    fun `selectReasonAndComplete - logs the selected reason`() = runTest {
        engine.selectReasonAndComplete(MindfulReason.HABIT)
        assertEquals(MindfulReason.HABIT.name, eventDao.insertedEvents[0].reason)
    }

    @Test
    fun `selectReasonAndComplete - clears active state`() = runTest {
        engine.selectReasonAndComplete(MindfulReason.IMPORTANT)
        assertFalse(engine.pauseState.value.isActive)
    }

    @Test
    fun `dismiss - clears active state and packageName`() {
        engine.dismiss()
        assertFalse(engine.pauseState.value.isActive)
        assertEquals("", engine.pauseState.value.packageName)
    }

    @Test
    fun `onSessionEnd - resets all friction counters`() {
        frictionController.recordAttemptAndGetDelay("com.instagram.android")
        frictionController.recordAttemptAndGetDelay("com.google.android.youtube")
        engine.onSessionEnd()
        assertEquals(0, frictionController.getAttemptCount("com.instagram.android"))
        assertEquals(0, frictionController.getAttemptCount("com.google.android.youtube"))
    }
}

// ─── AdaptiveFrictionController Tests ────────────────────────────────────────

class AdaptiveFrictionControllerTest {

    private lateinit var controller: AdaptiveFrictionController

    @Before
    fun setup() {
        controller = AdaptiveFrictionController()
    }

    @Test
    fun `first attempt returns 5 seconds`() {
        assertEquals(5, controller.recordAttemptAndGetDelay("com.instagram.android"))
    }

    @Test
    fun `second attempt returns 10 seconds`() {
        controller.recordAttemptAndGetDelay("com.instagram.android")
        assertEquals(10, controller.recordAttemptAndGetDelay("com.instagram.android"))
    }

    @Test
    fun `third attempt returns 20 seconds`() {
        repeat(2) { controller.recordAttemptAndGetDelay("com.instagram.android") }
        assertEquals(20, controller.recordAttemptAndGetDelay("com.instagram.android"))
    }

    @Test
    fun `fourth attempt returns 30 seconds`() {
        repeat(3) { controller.recordAttemptAndGetDelay("com.instagram.android") }
        assertEquals(30, controller.recordAttemptAndGetDelay("com.instagram.android"))
    }

    @Test
    fun `fifth and beyond are capped at 30 seconds`() {
        repeat(4) { controller.recordAttemptAndGetDelay("com.instagram.android") }
        assertEquals(30, controller.recordAttemptAndGetDelay("com.instagram.android"))
        assertEquals(30, controller.recordAttemptAndGetDelay("com.instagram.android"))
    }

    @Test
    fun `different packages have independent friction counters`() {
        controller.recordAttemptAndGetDelay("com.instagram.android")
        controller.recordAttemptAndGetDelay("com.instagram.android")
        // YouTube starts fresh at 5s
        assertEquals(5, controller.recordAttemptAndGetDelay("com.google.android.youtube"))
    }

    @Test
    fun `reset clears all package counters`() {
        controller.recordAttemptAndGetDelay("com.instagram.android")
        controller.recordAttemptAndGetDelay("com.twitter.android")
        controller.reset()
        assertEquals(0, controller.getAttemptCount("com.instagram.android"))
        assertEquals(0, controller.getAttemptCount("com.twitter.android"))
    }

    @Test
    fun `resetFor clears only the specified package`() {
        controller.recordAttemptAndGetDelay("com.instagram.android")
        controller.recordAttemptAndGetDelay("com.google.android.youtube")
        controller.resetFor("com.instagram.android")
        assertEquals(0, controller.getAttemptCount("com.instagram.android"))
        assertEquals(1, controller.getAttemptCount("com.google.android.youtube"))
    }

    @Test
    fun `getAttemptCount returns 0 for unknown package`() {
        assertEquals(0, controller.getAttemptCount("com.some.unknown.app"))
    }
}
