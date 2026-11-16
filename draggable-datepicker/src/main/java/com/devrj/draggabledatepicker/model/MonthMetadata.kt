package com.devrj.draggabledatepicker.model

import androidx.compose.runtime.Immutable
import java.time.DayOfWeek
import java.time.YearMonth

/**
 * Grid facts for one month.
 *
 * @property leadingBlanks The empty cells before day 1, given the picker's first day of the week.
 */
@Immutable
internal data class MonthMetadata(
    val yearMonth: YearMonth,
    val daysInMonth: Int,
    val leadingBlanks: Int,
    val firstDayEpoch: Long,
) {
    val rows: Int get() = (leadingBlanks + daysInMonth + 6) / 7

    companion object {
        fun of(yearMonth: YearMonth, firstDayOfWeek: DayOfWeek): MonthMetadata {
            val first = yearMonth.atDay(1)
            return MonthMetadata(
                yearMonth = yearMonth,
                daysInMonth = yearMonth.lengthOfMonth(),
                leadingBlanks = (first.dayOfWeek.value - firstDayOfWeek.value + 7) % 7,
                firstDayEpoch = first.toEpochDay(),
            )
        }
    }
}
