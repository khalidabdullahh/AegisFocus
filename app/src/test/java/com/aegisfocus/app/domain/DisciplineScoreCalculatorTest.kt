package com.aegisfocus.app.domain

import com.aegisfocus.app.data.local.entity.AchievementEntity
import com.aegisfocus.app.data.local.entity.DailyGoalEntity
import com.aegisfocus.app.data.local.entity.FocusEventEntity
import com.aegisfocus.app.data.local.entity.UserXpEntity
import com.aegisfocus.app.data.local.dao.DailyGoalDao
import com.aegisfocus.app.data.local.dao.FocusEventDao
import com.aegisfocus.app.data.local.dao.UserXpDao
import com.aegisfocus.app.domain.analytics.DisciplineScoreCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// ─── Fakes ───────────────────────────────────────────────────────────────────

class FakeFocusEventDaoForScore : FocusEventDao {
    var blockAttempts: Int = 0
    var emergencyExits: Int = 0

    override suspend fun insertEvent(event: FocusEventEntity) {}
    override suspend fun getEventsForSession(sessionId: Long): List<FocusEventEntity> = emptyList()
    override fun getRecentEvents(sinceEpochMs: Long): Flow<List<FocusEventEntity>> = flowOf(emptyList())
    override suspend fun countBlockAttemptsAfter(sinceEpochMs: Long): Int = blockAttempts
    override suspend fun countEmergencyExitsAfter(sinceEpochMs: Long): Int = emergencyExits
    override suspend fun pruneOldEvents(beforeEpochMs: Long) {}
}

class FakeDailyGoalDao : DailyGoalDao {
    var goalForToday: DailyGoalEntity? = null

    override suspend fun upsertGoal(goal: DailyGoalEntity) { goalForToday = goal }
    override suspend fun getGoalForDate(dateKey: String): DailyGoalEntity? = goalForToday
    override fun getRecentGoals(): Flow<List<DailyGoalEntity>> = flowOf(emptyList())
    override fun getGoalsForMonth(yearMonthPrefix: String): Flow<List<DailyGoalEntity>> = flowOf(emptyList())
    override suspend fun getGoalsListForMonth(yearMonthPrefix: String): List<DailyGoalEntity> = emptyList()
    override suspend fun addFocusMinutes(dateKey: String, minutes: Int, nowMs: Long) {}
    override suspend fun incrementSessions(dateKey: String, nowMs: Long) {}
    override suspend fun markGoalMet(dateKey: String, nowMs: Long) {}
}

class FakeUserXpDaoForScore : UserXpDao {
    var xp: UserXpEntity? = null

    override suspend fun upsertXp(xp: UserXpEntity) { this.xp = xp }
    override fun observeXp(): Flow<UserXpEntity?> = flowOf(xp)
    override suspend fun getXp(): UserXpEntity? = xp
    override suspend fun addXp(xp: Int, nowMs: Long) {
        this.xp = (this.xp ?: UserXpEntity()).let { it.copy(totalXp = it.totalXp + xp) }
    }
    override suspend fun insertAchievement(achievement: AchievementEntity) {}
    override fun observeAchievements(): Flow<List<AchievementEntity>> = flowOf(emptyList())
    override suspend fun hasAchievement(id: String): Int = 0
}

// ─── Tests ───────────────────────────────────────────────────────────────────

class DisciplineScoreCalculatorTest {

    private lateinit var eventDao: FakeFocusEventDaoForScore
    private lateinit var goalDao: FakeDailyGoalDao
    private lateinit var xpDao: FakeUserXpDaoForScore
    private lateinit var calculator: DisciplineScoreCalculator

    @Before
    fun setup() {
        eventDao = FakeFocusEventDaoForScore()
        goalDao = FakeDailyGoalDao()
        xpDao = FakeUserXpDaoForScore()
        calculator = DisciplineScoreCalculator(eventDao, goalDao, xpDao)
    }

    @Test
    fun `clean slate - score equals base 80`() = runTest {
        val breakdown = calculator.calculateScore()
        assertEquals(80, breakdown.total)
    }

    @Test
    fun `10 block attempts - deducts 20 penalty points`() = runTest {
        eventDao.blockAttempts = 10
        val breakdown = calculator.calculateScore()
        assertEquals(20, breakdown.distractionPenalty)
        assertEquals(60, breakdown.total)
    }

    @Test
    fun `distraction penalty is hard capped at 20`() = runTest {
        eventDao.blockAttempts = 100
        val breakdown = calculator.calculateScore()
        assertEquals(20, breakdown.distractionPenalty)
    }

    @Test
    fun `5 emergency exits - deducts 20 max exit penalty`() = runTest {
        eventDao.emergencyExits = 5
        val breakdown = calculator.calculateScore()
        assertEquals(20, breakdown.emergencyExitPenalty)
        assertEquals(60, breakdown.total)
    }

    @Test
    fun `meeting daily goal adds 5 bonus points`() = runTest {
        goalDao.goalForToday = DailyGoalEntity(dateKey = "2025-09-08", isGoalMet = true)
        val breakdown = calculator.calculateScore()
        assertEquals(5, breakdown.goalBonus)
        assertEquals(85, breakdown.total)
    }

    @Test
    fun `14 day streak adds 5 streak bonus points`() = runTest {
        xpDao.xp = UserXpEntity(currentStreak = 14)
        val breakdown = calculator.calculateScore()
        assertEquals(5, breakdown.streakBonus)
        assertEquals(85, breakdown.total)
    }

    @Test
    fun `7 day streak adds 3 streak bonus points`() = runTest {
        xpDao.xp = UserXpEntity(currentStreak = 7)
        val breakdown = calculator.calculateScore()
        assertEquals(3, breakdown.streakBonus)
    }

    @Test
    fun `3 day streak adds 2 streak bonus points`() = runTest {
        xpDao.xp = UserXpEntity(currentStreak = 3)
        val breakdown = calculator.calculateScore()
        assertEquals(2, breakdown.streakBonus)
    }

    @Test
    fun `score always stays within 10 to 100 clamp range`() = runTest {
        eventDao.blockAttempts = 1000
        eventDao.emergencyExits = 1000
        val breakdown = calculator.calculateScore()
        assertTrue(breakdown.total >= 10)
        assertTrue(breakdown.total <= 100)
    }

    @Test
    fun `full bonus scenario - score clamped at 100`() = runTest {
        xpDao.xp = UserXpEntity(currentStreak = 30)
        goalDao.goalForToday = DailyGoalEntity(dateKey = "2025-09-08", isGoalMet = true)
        val breakdown = calculator.calculateScore()
        assertTrue("Score should not exceed 100", breakdown.total <= 100)
    }

    @Test
    fun `breakdown base is always 80`() = runTest {
        val breakdown = calculator.calculateScore()
        assertEquals(80, breakdown.base)
    }
}
