package com.aegisfocus.app.domain

import com.aegisfocus.app.data.local.entity.AchievementEntity
import com.aegisfocus.app.data.local.entity.UserXpEntity
import com.aegisfocus.app.data.local.dao.UserXpDao
import com.aegisfocus.app.domain.motivation.Achievement
import com.aegisfocus.app.domain.motivation.DisciplineLevel
import com.aegisfocus.app.domain.motivation.XpManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

// ─── Fake DAO ────────────────────────────────────────────────────────────────

class FakeUserXpDao : UserXpDao {
    private val xpFlow = MutableStateFlow<UserXpEntity?>(null)
    val achievements = mutableListOf<AchievementEntity>()

    override suspend fun upsertXp(xp: UserXpEntity) { xpFlow.value = xp }
    override fun observeXp(): Flow<UserXpEntity?> = xpFlow
    override suspend fun getXp(): UserXpEntity? = xpFlow.value
    override suspend fun addXp(xp: Int, nowMs: Long) {
        val current = xpFlow.value ?: UserXpEntity()
        xpFlow.value = current.copy(totalXp = current.totalXp + xp)
    }
    override suspend fun insertAchievement(achievement: AchievementEntity) {
        achievements.add(achievement)
    }
    override fun observeAchievements(): Flow<List<AchievementEntity>> =
        MutableStateFlow(achievements.toList())
    override suspend fun hasAchievement(id: String): Int =
        if (achievements.any { it.achievementId == id }) 1 else 0
}

// ─── DisciplineLevel Tests ────────────────────────────────────────────────────

class DisciplineLevelTest {

    @Test
    fun `0 XP maps to NEW_STARTER`() {
        assertEquals(DisciplineLevel.NEW_STARTER, DisciplineLevel.fromXp(0))
    }

    @Test
    fun `499 XP maps to NEW_STARTER`() {
        assertEquals(DisciplineLevel.NEW_STARTER, DisciplineLevel.fromXp(499))
    }

    @Test
    fun `500 XP maps to CONSISTENT`() {
        assertEquals(DisciplineLevel.CONSISTENT, DisciplineLevel.fromXp(500))
    }

    @Test
    fun `1500 XP maps to FOCUSED`() {
        assertEquals(DisciplineLevel.FOCUSED, DisciplineLevel.fromXp(1500))
    }

    @Test
    fun `3500 XP maps to DISCIPLINED`() {
        assertEquals(DisciplineLevel.DISCIPLINED, DisciplineLevel.fromXp(3500))
    }

    @Test
    fun `7500 XP maps to IRON_WILL`() {
        assertEquals(DisciplineLevel.IRON_WILL, DisciplineLevel.fromXp(7500))
    }

    @Test
    fun `10000 XP maps to IRON_WILL (stays at top level)`() {
        assertEquals(DisciplineLevel.IRON_WILL, DisciplineLevel.fromXp(10_000))
    }

    @Test
    fun `xpToNextLevel from 0 is 500`() {
        assertEquals(500, DisciplineLevel.xpToNextLevel(0))
    }

    @Test
    fun `xpToNextLevel from 499 is 1`() {
        assertEquals(1, DisciplineLevel.xpToNextLevel(499))
    }

    @Test
    fun `xpToNextLevel from 500 is 1000 (to FOCUSED)`() {
        assertEquals(1000, DisciplineLevel.xpToNextLevel(500))
    }

    @Test
    fun `xpToNextLevel from IRON_WILL is null`() {
        assertNull(DisciplineLevel.xpToNextLevel(8000))
    }
}

// ─── XpManager Tests ──────────────────────────────────────────────────────────

class XpMotivationTest {

    private lateinit var userXpDao: FakeUserXpDao
    private lateinit var xpManager: XpManager

    @Before
    fun setup() {
        userXpDao = FakeUserXpDao()
        xpManager = XpManager(userXpDao)
    }

    @Test
    fun `normal 25-min session awards 50 base XP`() = runTest {
        userXpDao.upsertXp(UserXpEntity())
        xpManager.awardSessionXp(durationMinutes = 25, wasStrictMode = false, blockAttempts = 5)
        // 25 * 2 = 50
        assertEquals(50, userXpDao.getXp()!!.totalXp)
    }

    @Test
    fun `strict mode session adds 50 XP bonus`() = runTest {
        userXpDao.upsertXp(UserXpEntity())
        xpManager.awardSessionXp(durationMinutes = 25, wasStrictMode = true, blockAttempts = 5)
        // 50 base + 50 strict = 100
        assertEquals(100, userXpDao.getXp()!!.totalXp)
    }

    @Test
    fun `clean session (zero block attempts) adds 25 XP clean bonus`() = runTest {
        userXpDao.upsertXp(UserXpEntity())
        xpManager.awardSessionXp(durationMinutes = 25, wasStrictMode = false, blockAttempts = 0)
        // 50 + 25 = 75
        assertEquals(75, userXpDao.getXp()!!.totalXp)
    }

    @Test
    fun `strict + clean session awards all three bonuses`() = runTest {
        userXpDao.upsertXp(UserXpEntity())
        xpManager.awardSessionXp(durationMinutes = 25, wasStrictMode = true, blockAttempts = 0)
        // 50 + 50 + 25 = 125
        assertEquals(125, userXpDao.getXp()!!.totalXp)
    }

    @Test
    fun `base XP is capped at 200 regardless of session length`() = runTest {
        userXpDao.upsertXp(UserXpEntity())
        // 300 min * 2 = 600 but capped at 200
        xpManager.awardSessionXp(durationMinutes = 300, wasStrictMode = false, blockAttempts = 5)
        assertEquals(200, userXpDao.getXp()!!.totalXp)
    }

    @Test
    fun `awardGoalXp gives 50 XP`() = runTest {
        userXpDao.upsertXp(UserXpEntity())
        xpManager.awardGoalXp()
        assertEquals(50, userXpDao.getXp()!!.totalXp)
    }

    @Test
    fun `unlockAchievementIfNew - grants XP reward and records achievement`() = runTest {
        userXpDao.upsertXp(UserXpEntity())
        xpManager.unlockAchievementIfNew(Achievement.FIRST_FOCUS)
        assertEquals(1, userXpDao.achievements.size)
        assertEquals(Achievement.FIRST_FOCUS.achievementId, userXpDao.achievements[0].achievementId)
        assertEquals(Achievement.FIRST_FOCUS.xpReward, userXpDao.getXp()!!.totalXp)
    }

    @Test
    fun `unlockAchievementIfNew - does not double-grant the same achievement`() = runTest {
        userXpDao.upsertXp(UserXpEntity())
        xpManager.unlockAchievementIfNew(Achievement.FIRST_FOCUS)
        xpManager.unlockAchievementIfNew(Achievement.FIRST_FOCUS) // second call
        assertEquals(1, userXpDao.achievements.size)
        assertEquals(Achievement.FIRST_FOCUS.xpReward, userXpDao.getXp()!!.totalXp)
    }

    @Test
    fun `unlockAchievementIfNew - different achievements both unlock`() = runTest {
        userXpDao.upsertXp(UserXpEntity())
        xpManager.unlockAchievementIfNew(Achievement.FIRST_FOCUS)
        xpManager.unlockAchievementIfNew(Achievement.FIRST_STRICT)
        assertEquals(2, userXpDao.achievements.size)
        val totalExpected = Achievement.FIRST_FOCUS.xpReward + Achievement.FIRST_STRICT.xpReward
        assertEquals(totalExpected, userXpDao.getXp()!!.totalXp)
    }
}
