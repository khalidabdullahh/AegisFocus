package com.aegisfocus.app.service.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aegisfocus.app.core.di.ServiceLocator

/** Safety net that runs about every 15 minutes: re-arms routine alarms and starts a missed routine. */
class ScheduleCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        ServiceLocator.getContainer(applicationContext).scheduleCoordinator.syncAll()
        return Result.success()
    }
}
