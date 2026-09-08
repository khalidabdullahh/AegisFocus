package com.aegisfocus.app.domain.usecase

import com.aegisfocus.app.domain.model.UsageStatsSummary
import com.aegisfocus.app.domain.repository.UsageStatsRepository

class GetUsageAnalyticsUseCase(
    private val usageStatsRepository: UsageStatsRepository
) {
    suspend fun getTodaySummary(): UsageStatsSummary {
        return usageStatsRepository.getTodayUsageSummary()
    }

    suspend fun getWeeklyHistory(): List<UsageStatsSummary> {
        return usageStatsRepository.getWeeklyUsageHistory()
    }
}
