package com.devrj.draggabledatepicker

import com.devrj.draggabledatepicker.model.MonthMetadata
import com.devrj.draggabledatepicker.selection.dayAtPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.YearMonth

class MonthGeometryTest {
    // September 2030 starts on a Sunday and needs six rows when weeks start on Monday.
    private val month = MonthMetadata.of(YearMonth.of(2030, 9), DayOfWeek.MONDAY)
    private val width = 700f
    private val height = 600f

    private fun dayAt(x: Float, y: Float, rtl: Boolean = false) =
        dayAtPosition(x, y, width, height, month, rtl)

    @Test
    fun `metadata follows the first day of the week`() {
        assertEquals(6, month.leadingBlanks)
        assertEquals(6, month.rows)
        val sundayFirst = MonthMetadata.of(YearMonth.of(2030, 9), DayOfWeek.SUNDAY)
        assertEquals(0, sundayFirst.leadingBlanks)
        assertEquals(5, sundayFirst.rows)
    }

    @Test
    fun `maps positions to days`() {
        assertEquals(1, dayAt(650f, 50f))
        assertEquals(2, dayAt(50f, 150f))
    }

    @Test
    fun `leading and trailing blanks have no day`() {
        assertNull(dayAt(50f, 50f))
        assertNull(dayAt(650f, 550f))
    }

    @Test
    fun `empty cells snap to the nearest day during a drag`() {
        assertEquals(1, dayAtPosition(50f, 50f, width, height, month, false, snapToMonth = true))
        assertEquals(30, dayAtPosition(650f, 550f, width, height, month, false, snapToMonth = true))
    }

    @Test
    fun `positions past the edges stay on the same row`() {
        assertEquals(8, dayAt(width + 500f, 150f))
        assertEquals(2, dayAt(-500f, 150f))
        assertEquals(1, dayAt(650f, -50f))
    }

    @Test
    fun `right-to-left mirrors the columns`() {
        assertEquals(1, dayAt(50f, 50f, rtl = true))
        assertEquals(8, dayAt(50f, 150f, rtl = true))
        assertEquals(2, dayAt(650f, 150f, rtl = true))
    }
}
