package com.aegisfocus.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aegisfocus.app.data.local.dao.BlockedAppDao
import com.aegisfocus.app.data.local.dao.DailyGoalDao
import com.aegisfocus.app.data.local.dao.EmergencyExitDao
import com.aegisfocus.app.data.local.dao.FocusEventDao
import com.aegisfocus.app.data.local.dao.FocusProfileDao
import com.aegisfocus.app.data.local.dao.FocusSessionDao
import com.aegisfocus.app.data.local.dao.ScheduleDao
import com.aegisfocus.app.data.local.dao.SubscriptionDao
import com.aegisfocus.app.data.local.dao.UsageSnapshotDao
import com.aegisfocus.app.data.local.dao.UserXpDao
import com.aegisfocus.app.data.local.dao.WebsiteRuleDao
import com.aegisfocus.app.data.local.entity.AchievementEntity
import com.aegisfocus.app.data.local.entity.BlockedAppEntity
import com.aegisfocus.app.data.local.entity.DailyGoalEntity
import com.aegisfocus.app.data.local.entity.EmergencyExitEntity
import com.aegisfocus.app.data.local.entity.FocusEventEntity
import com.aegisfocus.app.data.local.entity.FocusProfileEntity
import com.aegisfocus.app.data.local.entity.FocusSessionEntity
import com.aegisfocus.app.data.local.entity.ScheduleEntity
import com.aegisfocus.app.data.local.entity.SubscriptionEntity
import com.aegisfocus.app.data.local.entity.UsageSnapshotEntity
import com.aegisfocus.app.data.local.entity.UserXpEntity
import com.aegisfocus.app.data.local.entity.WebsiteRuleEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        FocusSessionEntity::class,
        FocusProfileEntity::class,
        ScheduleEntity::class,
        BlockedAppEntity::class,
        UsageSnapshotEntity::class,
        EmergencyExitEntity::class,
        // v2 additions
        FocusEventEntity::class,
        DailyGoalEntity::class,
        UserXpEntity::class,
        AchievementEntity::class,
        SubscriptionEntity::class,
        WebsiteRuleEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AegisDatabase : RoomDatabase() {

    // v1 DAOs
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun focusProfileDao(): FocusProfileDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun blockedAppDao(): BlockedAppDao
    abstract fun usageSnapshotDao(): UsageSnapshotDao
    abstract fun emergencyExitDao(): EmergencyExitDao

    // v2 DAOs
    abstract fun focusEventDao(): FocusEventDao
    abstract fun dailyGoalDao(): DailyGoalDao
    abstract fun userXpDao(): UserXpDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun websiteRuleDao(): WebsiteRuleDao

    companion object {
        @Volatile
        private var INSTANCE: AegisDatabase? = null

        /** Migration from schema v1 → v2: adds 5 new tables + achievements table. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `focus_events` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `sessionId` INTEGER NOT NULL,
                        `eventType` TEXT NOT NULL,
                        `packageName` TEXT,
                        `reason` TEXT,
                        `epochMs` INTEGER NOT NULL DEFAULT 0
                    )"""
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `daily_goals` (
                        `dateKey` TEXT NOT NULL PRIMARY KEY,
                        `targetFocusMinutes` INTEGER NOT NULL DEFAULT 120,
                        `targetSessions` INTEGER NOT NULL DEFAULT 3,
                        `achievedFocusMinutes` INTEGER NOT NULL DEFAULT 0,
                        `achievedSessions` INTEGER NOT NULL DEFAULT 0,
                        `isGoalMet` INTEGER NOT NULL DEFAULT 0,
                        `updatedAtEpochMs` INTEGER NOT NULL DEFAULT 0
                    )"""
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `user_xp` (
                        `id` INTEGER NOT NULL PRIMARY KEY,
                        `totalXp` INTEGER NOT NULL DEFAULT 0,
                        `currentLevel` TEXT NOT NULL DEFAULT 'NEW_STARTER',
                        `currentStreak` INTEGER NOT NULL DEFAULT 0,
                        `longestStreak` INTEGER NOT NULL DEFAULT 0,
                        `lastActivityDateKey` TEXT NOT NULL DEFAULT '',
                        `updatedAtEpochMs` INTEGER NOT NULL DEFAULT 0
                    )"""
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `achievements` (
                        `achievementId` TEXT NOT NULL PRIMARY KEY,
                        `name` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `xpReward` INTEGER NOT NULL,
                        `earnedAtEpochMs` INTEGER NOT NULL DEFAULT 0
                    )"""
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `subscription_state` (
                        `id` INTEGER NOT NULL PRIMARY KEY,
                        `planType` TEXT NOT NULL DEFAULT 'FREE',
                        `expiryEpochMs` INTEGER NOT NULL DEFAULT 0,
                        `purchaseToken` TEXT NOT NULL DEFAULT '',
                        `updatedAtEpochMs` INTEGER NOT NULL DEFAULT 0
                    )"""
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `website_rules` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `domain` TEXT NOT NULL,
                        `ruleType` TEXT NOT NULL DEFAULT 'DOMAIN',
                        `isEnabled` INTEGER NOT NULL DEFAULT 1,
                        `profileId` INTEGER,
                        `createdAtEpochMs` INTEGER NOT NULL DEFAULT 0
                    )"""
                )
            }
        }

        fun getInstance(context: Context): AegisDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AegisDatabase::class.java,
                    "aegis_focus.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Populate default starter profiles and seed subscription row
                            CoroutineScope(Dispatchers.IO).launch {
                                val database = getInstance(context)
                                database.focusProfileDao().insertDefaultProfiles(
                                    listOf(
                                        FocusProfileEntity(
                                            id = 1,
                                            name = "Deep Work",
                                            iconName = "shield",
                                            defaultDurationMinutes = 50,
                                            isStrictModeDefault = true,
                                            blockedPackageNames = "com.google.android.youtube,com.instagram.android,com.twitter.android,com.facebook.katana,com.zhiliaoapp.musically,com.reddit.frontpage",
                                            isDefault = true
                                        ),
                                        FocusProfileEntity(
                                            id = 2,
                                            name = "Study",
                                            iconName = "book",
                                            defaultDurationMinutes = 25,
                                            isStrictModeDefault = false,
                                            blockedPackageNames = "com.google.android.youtube,com.instagram.android,com.twitter.android,com.snapchat.android,com.discord",
                                            isDefault = true
                                        ),
                                        FocusProfileEntity(
                                            id = 3,
                                            name = "Sleep Calm",
                                            iconName = "moon",
                                            defaultDurationMinutes = 480,
                                            isStrictModeDefault = true,
                                            blockedPackageNames = "com.google.android.youtube,com.instagram.android,com.twitter.android,com.facebook.katana,com.netflix.mediaclient",
                                            isDefault = true
                                        ),
                                        FocusProfileEntity(
                                            id = 4,
                                            name = "Quick Reset",
                                            iconName = "zap",
                                            defaultDurationMinutes = 15,
                                            isStrictModeDefault = false,
                                            blockedPackageNames = "",
                                            isDefault = true
                                        )
                                    )
                                )
                                // Seed Free subscription state
                                database.subscriptionDao().upsertSubscription(
                                    SubscriptionEntity(planType = "FREE")
                                )
                                // Seed XP row
                                database.userXpDao().upsertXp(UserXpEntity())
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
