package com.devrj.draggabledatepicker.selection

import com.devrj.draggabledatepicker.model.MonthMetadata

internal const val DAYS_PER_WEEK = 7

/**
 * Returns the day of the month at a position inside a month grid. Positions outside the grid
 * resolve to the nearest cell, so a finger slightly past the edge never lands on another row.
 *
 * @param snapToMonth What to do for the empty cells before day 1 and after the last day: return
 * the nearest day when true, as a drag needs, or null when false, as a tap needs.
 */
internal fun dayAtPosition(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    month: MonthMetadata,
    isRtl: Boolean,
    snapToMonth: Boolean = false,
): Int? {
    if (width <= 0f || height <= 0f) return null
    val visualColumn = ((x / width) * DAYS_PER_WEEK).toInt().coerceIn(0, DAYS_PER_WEEK - 1)
    val column = if (isRtl) DAYS_PER_WEEK - 1 - visualColumn else visualColumn
    val row = ((y / height) * month.rows).toInt().coerceIn(0, month.rows - 1)
    val day = row * DAYS_PER_WEEK + column - month.leadingBlanks + 1
    return if (snapToMonth) day.coerceIn(1, month.daysInMonth) else day.takeIf { it in 1..month.daysInMonth }
}
