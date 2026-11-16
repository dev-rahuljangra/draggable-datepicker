package com.devrj.draggabledatepicker

import com.devrj.draggabledatepicker.enums.DateSelectionMode
import com.devrj.draggabledatepicker.selection.SelectionRules
import com.devrj.draggabledatepicker.selection.SelectionValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SelectionRulesTest {
    private val day10 = LocalDate.of(2030, 1, 10).toEpochDay()
    private fun day(offset: Int) = day10 + offset

    @Test
    fun `second tap completes a range and third starts over`() {
        val rules = SelectionRules()
        val first = rules.resolveTap(SelectionValue(null, null), day(0))
        assertEquals(SelectionValue(day(0), null), first)
        val second = rules.resolveTap(first, day(3))
        assertEquals(SelectionValue(day(0), day(3)), second)
        assertEquals(SelectionValue(day(5), null), rules.resolveTap(second, day(5)))
    }

    @Test
    fun `tapping before the start swaps the dates`() {
        val result = SelectionRules().resolveTap(SelectionValue(day(5), null), day(1))
        assertEquals(SelectionValue(day(1), day(5)), result)
    }

    @Test
    fun `single mode never creates a range`() {
        val rules = SelectionRules(mode = DateSelectionMode.SINGLE)
        assertEquals(SelectionValue(day(4), null), rules.resolveTap(SelectionValue(day(1), null), day(4)))
    }

    @Test
    fun `dates outside the limits and blocked dates are disabled`() {
        val rules = SelectionRules(
            minEpoch = day(0),
            maxEpoch = day(10),
            isDateSelectable = { it.toEpochDay() != day(5) },
        )
        assertFalse(rules.isEnabled(day(-1)))
        assertFalse(rules.isEnabled(day(11)))
        assertFalse(rules.isEnabled(day(5)))
        assertTrue(rules.isEnabled(day(4)))
        val current = SelectionValue(day(1), null)
        assertEquals(current, rules.resolveTap(current, day(5)))
    }

    @Test
    fun `a tap across a blocked date starts a new selection unless allowed`() {
        val blocked = SelectionRules(isDateSelectable = { it.toEpochDay() != day(2) })
        val current = SelectionValue(day(0), null)
        assertEquals(SelectionValue(day(4), null), blocked.resolveTap(current, day(4)))
        val allowed = blocked.copy(allowUnselectableDatesInRange = true)
        assertEquals(SelectionValue(day(0), day(4)), allowed.resolveTap(current, day(4)))
    }

    @Test
    fun `tap respects minimum and maximum range length`() {
        val rules = SelectionRules(minRangeDays = 3, maxRangeDays = 5)
        val current = SelectionValue(day(0), null)
        assertEquals(SelectionValue(day(1), null), rules.resolveTap(current, day(1)))
        assertEquals(SelectionValue(day(0), day(2)), rules.resolveTap(current, day(2)))
        assertEquals(SelectionValue(day(0), day(4)), rules.resolveTap(current, day(4)))
        assertEquals(SelectionValue(day(5), null), rules.resolveTap(current, day(5)))
    }

    @Test
    fun `drag from a single date extends in both directions`() {
        val rules = SelectionRules()
        assertEquals(SelectionValue(day(0), day(3)), rules.resolveDrag(day(0), day(3), day(0)))
        assertEquals(SelectionValue(day(-3), day(0)), rules.resolveDrag(day(0), day(-3), day(0)))
    }

    @Test
    fun `drag stops before a blocked date unless allowed`() {
        val rules = SelectionRules(isDateSelectable = { it.toEpochDay() != day(3) })
        assertEquals(SelectionValue(day(0), day(2)), rules.resolveDrag(day(0), day(6), day(0)))
        val allowed = rules.copy(allowUnselectableDatesInRange = true)
        assertEquals(SelectionValue(day(0), day(6)), allowed.resolveDrag(day(0), day(6), day(0)))
        // The end of a range must still be selectable.
        assertEquals(SelectionValue(day(0), day(2)), allowed.resolveDrag(day(0), day(3), day(0)))
    }

    @Test
    fun `drag stops at the date limits and maximum length`() {
        val limited = SelectionRules(minEpoch = day(-2), maxEpoch = day(4))
        assertEquals(SelectionValue(day(0), day(4)), limited.resolveDrag(day(0), day(20), day(0)))
        assertEquals(SelectionValue(day(-2), day(0)), limited.resolveDrag(day(0), day(-20), day(0)))
        val capped = SelectionRules(maxRangeDays = 3)
        assertEquals(SelectionValue(day(0), day(2)), capped.resolveDrag(day(0), day(20), day(0)))
    }

    @Test
    fun `drag holds the minimum length and keeps its direction on the anchor`() {
        val rules = SelectionRules(minRangeDays = 3)
        assertEquals(SelectionValue(day(0), day(2)), rules.resolveDrag(day(0), day(1), day(5)))
        assertEquals(SelectionValue(day(0), day(2)), rules.resolveDrag(day(0), day(0), day(5)))
        assertEquals(SelectionValue(day(-2), day(0)), rules.resolveDrag(day(0), day(0), day(-5)))
    }

    @Test
    fun `drag returns null when the minimum length cannot fit`() {
        val rules = SelectionRules(minRangeDays = 3, maxEpoch = day(1))
        assertNull(rules.resolveDrag(day(0), day(5), day(0)))
    }
}
