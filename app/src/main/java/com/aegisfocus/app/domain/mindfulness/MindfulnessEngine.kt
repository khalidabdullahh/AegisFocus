package com.aegisfocus.app.domain.mindfulness

import com.aegisfocus.app.data.local.dao.FocusEventDao
import com.aegisfocus.app.data.local.entity.FocusEventEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MindfulPauseState(
    val isActive: Boolean = false,
    val sessionId: Long = -1L,
    val packageName: String = "",
    val countdownSeconds: Int = 5,
    val waitSeconds: Int = 5,
    val selectedReason: MindfulReason? = null
)

/**
 * Manages the mindful pause flow:
 *  1. 5-second breathing countdown.
 *  2. Reason selection prompt (Why are you opening this?).
 *  3. Logs a MINDFUL_PAUSE event.
 */
class MindfulnessEngine(
    private val focusEventDao: FocusEventDao,
    private val adaptiveFrictionController: AdaptiveFrictionController
) {

    private val _pauseState = MutableStateFlow(MindfulPauseState())
    val pauseState: StateFlow<MindfulPauseState> = _pauseState.asStateFlow()

    /**
     * Begin a mindful pause for [packageName] in [sessionId].
     * Emits countdown ticks via [pauseState].
     * @param isPro whether adaptive friction applies (Pro feature).
     */
    suspend fun beginPause(sessionId: Long, packageName: String, isPro: Boolean) {
        val delaySec = if (isPro) {
            adaptiveFrictionController.recordAttemptAndGetDelay(packageName)
        } else {
            5 // Free tier: fixed 5s
        }

        _pauseState.value = MindfulPauseState(
            isActive = true,
            sessionId = sessionId,
            packageName = packageName,
            countdownSeconds = delaySec,
            waitSeconds = delaySec
        )

        // Tick down
        for (remaining in (delaySec - 1) downTo 0) {
            delay(1_000L)
            _pauseState.value = _pauseState.value.copy(countdownSeconds = remaining)
        }
    }

    /** Called when the user selects a mindful reason. Logs the event and clears state. */
    suspend fun selectReasonAndComplete(reason: MindfulReason) {
        val state = _pauseState.value
        _pauseState.value = state.copy(selectedReason = reason)

        // Persist to analytics
        focusEventDao.insertEvent(
            FocusEventEntity(
                sessionId = state.sessionId,
                eventType = "MINDFUL_PAUSE",
                packageName = state.packageName,
                reason = reason.name
            )
        )
        _pauseState.value = MindfulPauseState() // clear
    }

    /** Dismiss without reason (user backed out). */
    fun dismiss() {
        _pauseState.value = MindfulPauseState()
    }

    /** Reset friction on session end. */
    fun onSessionEnd() = adaptiveFrictionController.reset()
}
