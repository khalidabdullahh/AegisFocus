package com.aegisfocus.app.engine

import android.content.Context
import android.content.Intent
import com.aegisfocus.app.domain.model.EmergencyExitRecord
import com.aegisfocus.app.domain.model.FocusSession
import com.aegisfocus.app.domain.model.SessionMode
import com.aegisfocus.app.domain.model.SessionStatus
import com.aegisfocus.app.domain.usecase.EmergencyExitUseCase
import com.aegisfocus.app.domain.usecase.EndFocusSessionUseCase
import com.aegisfocus.app.domain.usecase.RestoreActiveSessionUseCase
import com.aegisfocus.app.domain.usecase.StartFocusSessionUseCase
import com.aegisfocus.app.service.AegisForegroundTimerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FocusSessionManager(
    private val context: Context,
    private val startFocusSessionUseCase: StartFocusSessionUseCase,
    private val endFocusSessionUseCase: EndFocusSessionUseCase,
    private val emergencyExitUseCase: EmergencyExitUseCase,
    private val restoreActiveSessionUseCase: RestoreActiveSessionUseCase,
    private val blockingRuleRepository: BlockingRuleRepository,
    private val foregroundAppMonitor: ForegroundAppMonitor,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private val _activeSession = MutableStateFlow<FocusSession?>(null)
    val activeSession: StateFlow<FocusSession?> = _activeSession.asStateFlow()

    private val _remainingMillis = MutableStateFlow(0L)
    val remainingMillis: StateFlow<Long> = _remainingMillis.asStateFlow()

    private var timerJob: Job? = null

    init {
        scope.launch {
            restoreActiveSession()
        }
    }

    suspend fun restoreActiveSession(): FocusSession? {
        val restored = restoreActiveSessionUseCase()
        if (restored != null) {
            _activeSession.value = restored
            blockingRuleRepository.updateFromSession(restored)
            startTicker(restored)
            startForegroundService(restored)
            foregroundAppMonitor.startFallbackPolling()
        } else {
            _activeSession.value = null
            _remainingMillis.value = 0L
            blockingRuleRepository.updateFromSession(null)
            stopForegroundService()
            foregroundAppMonitor.stopFallbackPolling()
        }
        return restored
    }

    suspend fun startSession(
        name: String = "Focus Session",
        durationMinutes: Int = 25,
        mode: SessionMode = SessionMode.NORMAL,
        blockedPackageNames: List<String>? = null,
        profileId: Long? = null
    ): Result<FocusSession> {
        val result = startFocusSessionUseCase(
            name = name,
            durationMinutes = durationMinutes,
            mode = mode,
            blockedPackageNames = blockedPackageNames,
            profileId = profileId
        )

        result.onSuccess { session ->
            _activeSession.value = session
            blockingRuleRepository.updateFromSession(session)
            startTicker(session)
            startForegroundService(session)
            foregroundAppMonitor.startFallbackPolling()
        }

        return result
    }

    suspend fun cancelSession(): Result<Unit> {
        val current = _activeSession.value ?: return Result.failure(IllegalStateException("No active session"))
        val result = endFocusSessionUseCase(current.id, isNormalCancel = true)

        result.onSuccess {
            stopSessionInternal()
        }

        return result
    }

    suspend fun triggerEmergencyExit(reasonNote: String? = null): Result<EmergencyExitRecord> {
        val current = _activeSession.value ?: return Result.failure(IllegalStateException("No active session"))
        val result = emergencyExitUseCase(current.id, reasonNote)

        result.onSuccess {
            stopSessionInternal()
        }

        return result
    }

    private fun startTicker(session: FocusSession) {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive) {
                val remaining = session.endEpochMs - System.currentTimeMillis()
                if (remaining <= 0) {
                    _remainingMillis.value = 0L
                    handleSessionNaturalCompletion(session.id)
                    break
                } else {
                    _remainingMillis.value = remaining
                }
                delay(1000L)
            }
        }
    }

    private suspend fun handleSessionNaturalCompletion(sessionId: Long) {
        endFocusSessionUseCase(sessionId, isNormalCancel = false)
        stopSessionInternal()
    }

    private fun stopSessionInternal() {
        timerJob?.cancel()
        timerJob = null
        _activeSession.value = null
        _remainingMillis.value = 0L
        blockingRuleRepository.updateFromSession(null)
        stopForegroundService()
        foregroundAppMonitor.stopFallbackPolling()
    }

    private fun startForegroundService(session: FocusSession) {
        val serviceIntent = Intent(context, AegisForegroundTimerService::class.java).apply {
            action = AegisForegroundTimerService.ACTION_START_TIMER
            putExtra(AegisForegroundTimerService.EXTRA_SESSION_NAME, session.name)
            putExtra(AegisForegroundTimerService.EXTRA_END_EPOCH_MS, session.endEpochMs)
            putExtra(AegisForegroundTimerService.EXTRA_IS_STRICT, session.mode == SessionMode.STRICT)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    private fun stopForegroundService() {
        val serviceIntent = Intent(context, AegisForegroundTimerService::class.java).apply {
            action = AegisForegroundTimerService.ACTION_STOP_TIMER
        }
        context.startService(serviceIntent)
    }
}
