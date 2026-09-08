package com.aegisfocus.app.engine

import android.content.Context
import android.content.Intent
import com.aegisfocus.app.domain.repository.FocusSessionRepository
import com.aegisfocus.app.presentation.blocker.BlockInterventionActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class BlockInterventionController(
    private val context: Context,
    private val sessionRepository: FocusSessionRepository,
    private val blockingRuleRepository: BlockingRuleRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private val lastInterventionMap = ConcurrentHashMap<String, Long>()
    private val interventionCooldownMs = 1500L

    fun onForegroundAppDetected(packageName: String) {
        if (!blockingRuleRepository.isBlocked(packageName, context.packageName)) {
            return
        }

        val now = System.currentTimeMillis()
        val lastTime = lastInterventionMap[packageName] ?: 0L
        if (now - lastTime < interventionCooldownMs) {
            return // Debounce rapid accessibility window triggers
        }
        lastInterventionMap[packageName] = now

        scope.launch {
            val activeSession = sessionRepository.getActiveSession()
            if (activeSession != null && !activeSession.isExpired) {
                sessionRepository.recordIntervention(activeSession.id)

                val intent = Intent(context, BlockInterventionActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(BlockInterventionActivity.EXTRA_BLOCKED_PACKAGE, packageName)
                    putExtra(BlockInterventionActivity.EXTRA_SESSION_NAME, activeSession.name)
                    putExtra(BlockInterventionActivity.EXTRA_IS_STRICT, activeSession.mode.name)
                    putExtra(BlockInterventionActivity.EXTRA_REMAINING_MILLIS, activeSession.remainingMillis)
                }
                context.startActivity(intent)
            }
        }
    }
}
