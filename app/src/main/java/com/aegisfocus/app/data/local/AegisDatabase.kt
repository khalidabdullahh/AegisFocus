package com.aegisfocus.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aegisfocus.app.data.local.dao.BlockedAppDao
import com.aegisfocus.app.data.local.dao.EmergencyExitDao
import com.aegisfocus.app.data.local.dao.FocusProfileDao
import com.aegisfocus.app.data.local.dao.FocusSessionDao
import com.aegisfocus.app.data.local.dao.ScheduleDao
import com.aegisfocus.app.data.local.dao.UsageSnapshotDao
import com.aegisfocus.app.data.local.entity.BlockedAppEntity
import com.aegisfocus.app.data.local.entity.EmergencyExitEntity
import com.aegisfocus.app.data.local.entity.FocusProfileEntity
import com.aegisfocus.app.data.local.entity.FocusSessionEntity
import com.aegisfocus.app.data.local.entity.ScheduleEntity
import com.aegisfocus.app.data.local.entity.UsageSnapshotEntity
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
        EmergencyExitEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AegisDatabase : RoomDatabase() {

    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun focusProfileDao(): FocusProfileDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun blockedAppDao(): BlockedAppDao
    abstract fun usageSnapshotDao(): UsageSnapshotDao
    abstract fun emergencyExitDao(): EmergencyExitDao

    companion object {
        @Volatile
        private var INSTANCE: AegisDatabase? = null

        fun getInstance(context: Context): AegisDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AegisDatabase::class.java,
                    "aegis_focus.db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Populate default starter profiles
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
                            }
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
