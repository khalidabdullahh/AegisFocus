package com.aegisfocus.app.domain

import com.aegisfocus.app.core.common.TimeUtils
import com.aegisfocus.app.domain.model.Schedule
import com.aegisfocus.app.domain.repository.ScheduleRepository
import com.aegisfocus.app.domain.usecase.EvaluateScheduleTriggerUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ScheduleCalculatorTest {

    @Test
    fun daysBitmask_correctlyIdentifiesActiveDays() {
        val monFriBitmask = 62 // Monday(2) + Tuesday(4) + Wednesday(8) + Thursday(16) + Friday(32)

        assertTrue(TimeUtils.isDaySelected(monFriBitmask, Calendar.MONDAY))
        assertTrue(TimeUtils.isDaySelected(monFriBitmask, Calendar.FRIDAY))
        assertFalse(TimeUtils.isDaySelected(monFriBitmask, Calendar.SUNDAY))
        assertFalse(TimeUtils.isDaySelected(monFriBitmask, Calendar.SATURDAY))

        val weekendsBitmask = 65 // Sunday(1) + Saturday(64)
        assertTrue(TimeUtils.isDaySelected(weekendsBitmask, Calendar.SUNDAY))
        assertTrue(TimeUtils.isDaySelected(weekendsBitmask, Calendar.SATURDAY))
        assertFalse(TimeUtils.isDaySelected(weekendsBitmask, Calendar.WEDNESDAY))
    }

    @Test
    fun durationCalculation_handlesRegularAndOvernightSchedules() {
        val regular = Schedule(
            id = 1,
            name = "Morning",
            startHour = 9,
            startMinute = 0,
            endHour = 13,
            endMinute = 30,
            daysOfWeekBitmask = 62
        )
        assertEquals(270, regular.durationMinutes) // 4 hours 30 min = 270 min

        val overnight = Schedule(
            id = 2,
            name = "Sleep",
            startHour = 23,
            startMinute = 0,
            endHour = 7,
            endMinute = 0,
            daysOfWeekBitmask = 127
        )
        assertEquals(480, overnight.durationMinutes) // 8 hours = 480 min
    }

    @Test
    fun evaluateScheduleTrigger_matchesActiveTimeWindow() = runBlocking {
        val fakeRepo = object : ScheduleRepository {
            override fun getAllSchedulesFlow(): Flow<List<Schedule>> = flowOf(emptyList())
            override suspend fun getEnabledSchedules(): List<Schedule> = listOf(
                Schedule(
                    id = 1,
                    name = "Work Hours",
                    startHour = 10,
                    startMinute = 0,
                    endHour = 12,
                    endMinute = 0,
                    daysOfWeekBitmask = 127, // Everyday
                    isEnabled = true
                )
            )
            override suspend fun getScheduleById(id: Long): Schedule? = null
            override suspend fun saveSchedule(schedule: Schedule): Long = 1L
            override suspend fun setScheduleEnabled(id: Long, isEnabled: Boolean) {}
            override suspend fun deleteSchedule(schedule: Schedule) {}
        }

        val useCase = EvaluateScheduleTriggerUseCase(fakeRepo)

        // 10:30 AM (Inside window)
        val calInside = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 30)
        }
        val matchingInside = useCase.getMatchingSchedulesForNow(calInside.timeInMillis)
        assertEquals(1, matchingInside.size)
        assertEquals("Work Hours", matchingInside.first().name)

        // 13:15 PM (Outside window)
        val calOutside = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 13)
            set(Calendar.MINUTE, 15)
        }
        val matchingOutside = useCase.getMatchingSchedulesForNow(calOutside.timeInMillis)
        assertTrue(matchingOutside.isEmpty())
    }
}
