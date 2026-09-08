package com.aegisfocus.app.service.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aegisfocus.app.core.di.ServiceLocator
import com.aegisfocus.app.domain.model.SessionMode

class ScheduleCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val container = ServiceLocator.getContainer(applicationContext)
        val matching = container.evaluateScheduleTriggerUseCase.getMatchingSchedulesForNow()

        if (matching.isNotEmpty()) {
            val activeSession = container.focusSessionRepository.getActiveSession()
            if (activeSession == null || activeSession.isExpired) {
                val schedule = matching.first()
                container.focusSessionManager.startSession(
                    name = schedule.name,
                    durationMinutes = schedule.durationMinutes,
                    mode = if (schedule.isStrictMode) SessionMode.STRICT else SessionMode.NORMAL,
                    blockedPackageNames = schedule.blockedPackageNames,
                    profileId = schedule.profileId
                )
            }
        }

        return Result.success()
    }
}
