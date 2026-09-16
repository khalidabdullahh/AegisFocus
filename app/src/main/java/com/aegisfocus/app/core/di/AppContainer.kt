package com.aegisfocus.app.core.di

import android.content.Context
import com.aegisfocus.app.data.local.AegisDatabase
import com.aegisfocus.app.data.repository.AppDiscoveryRepositoryImpl
import com.aegisfocus.app.data.repository.FocusSessionRepositoryImpl
import com.aegisfocus.app.data.repository.ProfileRepositoryImpl
import com.aegisfocus.app.data.repository.ScheduleRepositoryImpl
import com.aegisfocus.app.data.repository.SettingsRepositoryImpl
import com.aegisfocus.app.data.repository.UsageStatsRepositoryImpl
import com.aegisfocus.app.domain.analytics.DisciplineScoreCalculator
import com.aegisfocus.app.domain.mindfulness.AdaptiveFrictionController
import com.aegisfocus.app.domain.mindfulness.MindfulnessEngine
import com.aegisfocus.app.domain.motivation.XpManager
import com.aegisfocus.app.domain.repository.AppDiscoveryRepository
import com.aegisfocus.app.domain.repository.FocusSessionRepository
import com.aegisfocus.app.domain.repository.ProfileRepository
import com.aegisfocus.app.domain.repository.ScheduleRepository
import com.aegisfocus.app.domain.repository.SettingsRepository
import com.aegisfocus.app.domain.repository.UsageStatsRepository
import com.aegisfocus.app.domain.subscription.EntitlementManager
import com.aegisfocus.app.domain.usecase.EmergencyExitUseCase
import com.aegisfocus.app.domain.usecase.EndFocusSessionUseCase
import com.aegisfocus.app.domain.usecase.EvaluateScheduleTriggerUseCase
import com.aegisfocus.app.domain.usecase.GetUsageAnalyticsUseCase
import com.aegisfocus.app.domain.usecase.RestoreActiveSessionUseCase
import com.aegisfocus.app.domain.usecase.StartFocusSessionUseCase
import com.aegisfocus.app.domain.usecase.StrictModeCommitUseCase
import com.aegisfocus.app.engine.BlockInterventionController
import com.aegisfocus.app.engine.BlockedAppMatcher
import com.aegisfocus.app.engine.BlockingRuleRepository
import com.aegisfocus.app.engine.FocusSessionManager
import com.aegisfocus.app.engine.ForegroundAppMonitor
import com.aegisfocus.app.engine.SessionPersistenceManager

class AppContainer(private val context: Context) {

    // ─── Database & DAOs ────────────────────────────────────────────────────
    val database: AegisDatabase by lazy { AegisDatabase.getInstance(context) }

    // v1 DAOs
    val focusSessionDao by lazy { database.focusSessionDao() }
    val focusProfileDao by lazy { database.focusProfileDao() }
    val scheduleDao by lazy { database.scheduleDao() }
    val blockedAppDao by lazy { database.blockedAppDao() }
    val usageSnapshotDao by lazy { database.usageSnapshotDao() }
    val emergencyExitDao by lazy { database.emergencyExitDao() }

    // v2 DAOs
    val focusEventDao by lazy { database.focusEventDao() }
    val dailyGoalDao by lazy { database.dailyGoalDao() }
    val userXpDao by lazy { database.userXpDao() }
    val subscriptionDao by lazy { database.subscriptionDao() }
    val websiteRuleDao by lazy { database.websiteRuleDao() }

    // ─── Repositories ────────────────────────────────────────────────────────
    val focusSessionRepository: FocusSessionRepository by lazy {
        FocusSessionRepositoryImpl(focusSessionDao, emergencyExitDao)
    }
    val profileRepository: ProfileRepository by lazy {
        ProfileRepositoryImpl(focusProfileDao)
    }
    val scheduleRepository: ScheduleRepository by lazy {
        ScheduleRepositoryImpl(scheduleDao)
    }
    val appDiscoveryRepository: AppDiscoveryRepository by lazy {
        AppDiscoveryRepositoryImpl(context, blockedAppDao)
    }
    val usageStatsRepository: UsageStatsRepository by lazy {
        UsageStatsRepositoryImpl(context, focusSessionDao, usageSnapshotDao)
    }
    val settingsRepository: SettingsRepository by lazy {
        SettingsRepositoryImpl(context)
    }

    // ─── Domain Engines (v2) ─────────────────────────────────────────────────
    val entitlementManager by lazy { EntitlementManager(subscriptionDao) }
    val playBillingManager by lazy { com.aegisfocus.app.data.billing.PlayBillingManager(context, entitlementManager) }
    val storeBillingManager by lazy { com.aegisfocus.app.data.billing.StoreBillingManager(context, entitlementManager, playBillingManager) }
    val supabaseAuthService by lazy { com.aegisfocus.app.data.remote.SupabaseAuthService(context) }
    val supabasePaymentService by lazy { com.aegisfocus.app.data.remote.SupabasePaymentService(supabaseAuthService) }
    val supabaseSyncService by lazy { com.aegisfocus.app.data.remote.SupabaseSyncService(supabaseAuthService, userXpDao, subscriptionDao) }

    val adaptiveFrictionController by lazy { AdaptiveFrictionController() }

    val mindfulnessEngine by lazy {
        MindfulnessEngine(focusEventDao, adaptiveFrictionController)
    }

    val disciplineScoreCalculator by lazy {
        DisciplineScoreCalculator(focusEventDao, dailyGoalDao, userXpDao)
    }

    val xpManager by lazy { XpManager(userXpDao) }

    // ─── Use Cases ───────────────────────────────────────────────────────────
    val startFocusSessionUseCase by lazy {
        StartFocusSessionUseCase(focusSessionRepository, appDiscoveryRepository)
    }
    val endFocusSessionUseCase by lazy {
        EndFocusSessionUseCase(focusSessionRepository, usageStatsRepository)
    }
    val emergencyExitUseCase by lazy {
        EmergencyExitUseCase(focusSessionRepository)
    }
    val restoreActiveSessionUseCase by lazy {
        RestoreActiveSessionUseCase(focusSessionRepository, usageStatsRepository)
    }
    val getUsageAnalyticsUseCase by lazy {
        GetUsageAnalyticsUseCase(usageStatsRepository)
    }
    val evaluateScheduleTriggerUseCase by lazy {
        EvaluateScheduleTriggerUseCase(scheduleRepository)
    }
    val strictModeCommitUseCase by lazy {
        StrictModeCommitUseCase(entitlementManager)
    }
    val exportDataUseCase by lazy {
        com.aegisfocus.app.domain.usecase.ExportDataUseCase(focusSessionDao, focusEventDao, userXpDao)
    }

    // ─── Engine Components ───────────────────────────────────────────────────
    val blockedAppMatcher by lazy { BlockedAppMatcher() }
    val blockingRuleRepository by lazy { BlockingRuleRepository(blockedAppMatcher) }
    val blockInterventionController by lazy {
        BlockInterventionController(context, focusSessionRepository, blockingRuleRepository)
    }
    val foregroundAppMonitor by lazy {
        ForegroundAppMonitor(context, blockInterventionController, blockingRuleRepository)
    }
    val sessionPersistenceManager by lazy {
        SessionPersistenceManager(restoreActiveSessionUseCase)
    }
    val focusSessionManager by lazy {
        FocusSessionManager(
            context = context,
            startFocusSessionUseCase = startFocusSessionUseCase,
            endFocusSessionUseCase = endFocusSessionUseCase,
            emergencyExitUseCase = emergencyExitUseCase,
            restoreActiveSessionUseCase = restoreActiveSessionUseCase,
            blockingRuleRepository = blockingRuleRepository,
            foregroundAppMonitor = foregroundAppMonitor
        )
    }
}
