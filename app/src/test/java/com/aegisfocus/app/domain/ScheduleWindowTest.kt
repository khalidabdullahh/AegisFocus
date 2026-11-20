package com.aegisfocus.app.domain

import com.aegisfocus.app.domain.model.Schedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ScheduleWindowTest {

    private val everyday = 127
    private val monToFri = 62
    private val fridayOnly = 32

    /** A moment on the given weekday at hh:mm. */
    private fun at(dayOfWeek: Int, hour: Int, minute: Int): Long {
        val c = Calendar.getInstance()
        c.set(2026, Calendar.NOVEMBER, 15, hour, minute, 0) // a Sunday
        c.set(Calendar.MILLISECOND, 0)
        while (c.get(Calendar.DAY_OF_WEEK) != dayOfWeek) c.add(Calendar.DAY_OF_YEAR, 1)
        return c.timeInMillis
    }

    private fun routine(sh: Int, sm: Int, eh: Int, em: Int, days: Int) = Schedule(
        name = "Daily Deep Work",
        startHour = sh, startMinute = sm, endHour = eh, endMinute = em,
        daysOfWeekBitmask = days,
        blockedPackageNames = listOf("com.discord"),
        isStrictMode = true
    )

    @Test
    fun daytimeWindow_reportsTimeLeftInsideTheWindow() {
        val r = routine(6, 0, 11, 0, everyday)
        assertEquals(300, r.remainingMinutesAt(at(Calendar.MONDAY, 6, 0)))
        assertEquals(130, r.remainingMinutesAt(at(Calendar.MONDAY, 8, 50)))
        assertEquals(1, r.remainingMinutesAt(at(Calendar.MONDAY, 10, 59)))
    }

    @Test
    fun daytimeWindow_isClosedBeforeStartAndAtEnd() {
        val r = routine(6, 0, 11, 0, everyday)
        assertEquals(0, r.remainingMinutesAt(at(Calendar.MONDAY, 5, 59)))
        assertEquals(0, r.remainingMinutesAt(at(Calendar.MONDAY, 11, 0)))
        assertFalse(r.isActiveAt(at(Calendar.MONDAY, 14, 0)))
    }

    @Test
    fun weekdayOnlyRoutine_doesNotRunOnWeekend() {
        val r = routine(6, 0, 11, 0, monToFri)
        assertTrue(r.isActiveAt(at(Calendar.WEDNESDAY, 8, 50)))
        assertFalse(r.isActiveAt(at(Calendar.SATURDAY, 8, 50)))
        assertFalse(r.isActiveAt(at(Calendar.SUNDAY, 8, 50)))
    }

    @Test
    fun overnightWindow_coversBothSidesOfMidnight() {
        val r = routine(23, 0, 7, 0, everyday)
        assertEquals(450, r.remainingMinutesAt(at(Calendar.MONDAY, 23, 30)))
        assertEquals(300, r.remainingMinutesAt(at(Calendar.TUESDAY, 2, 0)))
        assertEquals(0, r.remainingMinutesAt(at(Calendar.TUESDAY, 7, 0)))
        assertEquals(0, r.remainingMinutesAt(at(Calendar.TUESDAY, 12, 0)))
    }

    @Test
    fun overnightWindow_afterMidnightBelongsToTheDayItStarted() {
        val r = routine(23, 0, 7, 0, fridayOnly)
        assertTrue(r.isActiveAt(at(Calendar.FRIDAY, 23, 30)))
        assertTrue("Saturday 02:00 is the tail of Friday night", r.isActiveAt(at(Calendar.SATURDAY, 2, 0)))
        assertFalse("Friday 02:00 is the tail of Thursday night", r.isActiveAt(at(Calendar.FRIDAY, 2, 0)))
        assertFalse(r.isActiveAt(at(Calendar.SATURDAY, 23, 30)))
    }

    @Test
    fun overnightWindow_sundayMorningFollowsSaturdayNight() {
        val saturdayOnly = 64
        val r = routine(22, 0, 6, 0, saturdayOnly)
        assertTrue(r.isActiveAt(at(Calendar.SUNDAY, 1, 0)))
    }

    @Test
    fun zeroLengthWindow_neverRuns() {
        val r = routine(9, 0, 9, 0, everyday)
        assertEquals(0, r.remainingMinutesAt(at(Calendar.MONDAY, 9, 0)))
    }
}
