package com.aegisfocus.app.core

import com.aegisfocus.app.core.common.DurationInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DurationInputTest {

    @Test
    fun parsesPlainMinutes() {
        assertEquals(90, DurationInput.parse("90"))
        assertEquals(25, DurationInput.parse(" 25 "))
    }

    @Test
    fun parsesHoursAndMinutes() {
        assertEquals(90, DurationInput.parse("1h 30m"))
        assertEquals(120, DurationInput.parse("2h"))
        assertEquals(135, DurationInput.parse("2h15min"))
        assertEquals(90, DurationInput.parse("1:30"))
        assertEquals(90, DurationInput.parse("1.5h"))
        assertEquals(45, DurationInput.parse("45m"))
    }

    @Test
    fun rejectsInvalidText() {
        assertNull(DurationInput.parse(""))
        assertNull(DurationInput.parse("abc"))
        assertNull(DurationInput.parse("0"))
        assertNull(DurationInput.parse("1:75"))
        assertNull(DurationInput.parse("h"))
    }

    @Test
    fun formatsForDisplay() {
        assertEquals("45 min", DurationInput.format(45))
        assertEquals("2h", DurationInput.format(120))
        assertEquals("17h", DurationInput.format(1020))
        assertEquals("1h 30m", DurationInput.format(90))
    }
}
