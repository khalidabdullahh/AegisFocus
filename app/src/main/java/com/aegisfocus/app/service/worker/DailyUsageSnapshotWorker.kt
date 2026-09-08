package com.aegisfocus.app.service.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aegisfocus.app.core.di.ServiceLocator

class DailyUsageSnapshotWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val container = ServiceLocator.getContainer(applicationContext)
        container.usageStatsRepository.getTodayUsageSummary()
        return Result.success()
    }
}
