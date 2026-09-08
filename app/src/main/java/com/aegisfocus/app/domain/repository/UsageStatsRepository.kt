package com.aegisfocus.app.domain.repository

import com.aegisfocus.app.domain.model.UsageStatsSummary
import kotlinx.coroutines.flow.Flow

interface UsageStatsRepository {
    suspend fun getTodayUsageSummary(): UsageStatsSummary
    suspend fun getWeeklyUsageHistory(): List<UsageStatsSummary>
    fun getDailyUsageFlow(): Flow<UsageStatsSummary>
    suspend fun recordFocusSessionCompleted(durationMinutes: Int)
}
