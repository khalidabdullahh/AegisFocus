package com.aegisfocus.app.data.repository

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import com.aegisfocus.app.core.common.TimeUtils
import com.aegisfocus.app.data.local.dao.FocusSessionDao
import com.aegisfocus.app.data.local.dao.UsageSnapshotDao
import com.aegisfocus.app.data.local.entity.UsageSnapshotEntity
import com.aegisfocus.app.domain.model.AppUsageStat
import com.aegisfocus.app.domain.model.HourlyDistribution
import com.aegisfocus.app.domain.model.UsageStatsSummary
import com.aegisfocus.app.domain.repository.UsageStatsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * Usage statistics backed only by real data:
 *  - focus time comes from completed focus sessions stored on the device
 *  - screen time comes from Android UsageStatsManager (needs Usage Access permission)
 * When a source is unavailable the value is reported as unavailable, never invented.
 */
class UsageStatsRepositoryImpl(
    private val context: Context,
    private val sessionDao: FocusSessionDao,
    private val snapshotDao: UsageSnapshotDao
) : UsageStatsRepository {

    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

    private data class ScreenTimeResult(
        val totalMs: Long,
        val topApps: List<AppUsageStat>,
        val available: Boolean
    )

    /** Reads real foreground time for launchable user apps between [startMs] and [endMs]. */
    private fun queryScreenTime(startMs: Long, endMs: Long): ScreenTimeResult {
        val manager = usageStatsManager ?: return ScreenTimeResult(0L, emptyList(), false)

        val stats: List<UsageStats>? = try {
            manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startMs, endMs)
        } catch (e: Exception) {
            null
        }
        if (stats.isNullOrEmpty()) return ScreenTimeResult(0L, emptyList(), false)

        val pm = context.packageManager
        val launchablePackages: Set<String> = try {
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            pm.queryIntentActivities(mainIntent, 0).map { it.activityInfo.packageName }.toSet()
        } catch (e: Exception) {
            emptySet()
        }

        val appMap = stats
            .filter { usage ->
                usage.totalTimeInForeground > 1000L &&
                    usage.lastTimeUsed >= startMs &&
                    usage.packageName != context.packageName &&
                    (usage.packageName in launchablePackages || pm.getLaunchIntentForPackage(usage.packageName) != null)
            }
            .groupBy { it.packageName }
            .mapValues { (_, usages) -> usages.maxOf { s -> s.totalTimeInForeground } }

        val topApps = appMap.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { (pkg, usageMs) ->
                val appName = try {
                    pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
                } catch (e: Exception) {
                    pkg.substringAfterLast(".")
                }
                AppUsageStat(packageName = pkg, appName = appName, usageTimeMs = usageMs)
            }

        return ScreenTimeResult(appMap.values.sum(), topApps, true)
    }

    override suspend fun getTodayUsageSummary(): UsageStatsSummary = withContext(Dispatchers.IO) {
        val startOfDay = TimeUtils.getStartOfDay()
        val endOfDay = System.currentTimeMillis()
        val todayDate = TimeUtils.getTodayDateString()

        val todaySessions = sessionDao.getSessionsInRange(startOfDay, endOfDay)
        val completedSessions = todaySessions.filter { it.status == "COMPLETED" }
        val totalFocusMs = completedSessions.sumOf { it.durationMinutes * 60 * 1000L }

        val screen = queryScreenTime(startOfDay, endOfDay)
        val streak = calculateStreakDays()
        val hourlyDistribution = calculateHourlyDistribution(completedSessions.map { it.toDomain() })

        val summary = UsageStatsSummary(
            date = todayDate,
            totalScreenTimeMs = screen.totalMs,
            totalFocusTimeMs = totalFocusMs,
            sessionsCompleted = completedSessions.size,
            streakDays = streak,
            topDistractingApps = screen.topApps,
            hourlyFocusDistribution = hourlyDistribution,
            hasScreenTimeData = screen.available
        )

        snapshotDao.insertOrUpdateSnapshot(
            UsageSnapshotEntity(
                date = todayDate,
                totalScreenTimeMs = screen.totalMs,
                totalFocusTimeMs = totalFocusMs,
                sessionsCompleted = completedSessions.size,
                streakDays = streak
            )
        )

        summary
    }

    override suspend fun getWeeklyUsageHistory(): List<UsageStatsSummary> = withContext(Dispatchers.IO) {
        val result = mutableListOf<UsageStatsSummary>()
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())

        for (i in 6 downTo 0) {
            val dayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
            val dayStart = TimeUtils.getStartOfDay(dayCal.timeInMillis)
            val dayEnd = if (i == 0) System.currentTimeMillis() else TimeUtils.getEndOfDay(dayCal.timeInMillis)

            val sessions = sessionDao.getSessionsInRange(dayStart, dayEnd).filter { it.status == "COMPLETED" }
            val screen = queryScreenTime(dayStart, dayEnd)

            result.add(
                UsageStatsSummary(
                    date = sdf.format(dayCal.time),
                    totalScreenTimeMs = screen.totalMs,
                    totalFocusTimeMs = sessions.sumOf { it.durationMinutes * 60 * 1000L },
                    sessionsCompleted = sessions.size,
                    hasScreenTimeData = screen.available
                )
            )
        }
        result
    }

    override fun getDailyUsageFlow(): Flow<UsageStatsSummary> = flow {
        emit(getTodayUsageSummary())
    }.flowOn(Dispatchers.IO)

    override suspend fun recordFocusSessionCompleted(durationMinutes: Int) {
        // Reflected on the next summary query, which reads sessions from the database.
    }

    /**
     * Consecutive days with at least one completed focus session.
     * Today does not break the streak until the day is over.
     */
    private suspend fun calculateStreakDays(): Int {
        var streak = 0
        val cal = Calendar.getInstance()
        for (i in 0..365) {
            val dayStart = TimeUtils.getStartOfDay(cal.timeInMillis)
            val dayEnd = TimeUtils.getEndOfDay(cal.timeInMillis)
            val completed = sessionDao.getSessionsInRange(dayStart, dayEnd).any { it.status == "COMPLETED" }
            if (completed) {
                streak++
            } else if (i != 0) {
                break
            }
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return streak
    }

    private fun calculateHourlyDistribution(sessions: List<com.aegisfocus.app.domain.model.FocusSession>): List<HourlyDistribution> {
        val cal = Calendar.getInstance()
        val hourMap = mutableMapOf<Int, Int>()
        for (h in 0..23) hourMap[h] = 0
        sessions.forEach { session ->
            cal.timeInMillis = session.startEpochMs
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            hourMap[hour] = (hourMap[hour] ?: 0) + session.durationMinutes
        }
        return hourMap.map { HourlyDistribution(it.key, it.value) }
    }
}
