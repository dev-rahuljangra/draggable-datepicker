package com.devrj.draggabledatepicker.model

import androidx.compose.runtime.Immutable
import java.time.LocalDate

/**
 * Describes one day for a custom `dayContent` slot.
 *
 * @property date The day this cell shows.
 * @property dayText The day number, formatted for the picker's locale.
 * @property isToday Whether [date] is today.
 * @property isWeekend Whether [date] falls on one of the picker's weekend days.
 * @property isEnabled Whether the user can select [date].
 * @property isRangeStart Whether [date] is the selected start date.
 * @property isRangeEnd Whether [date] is the selected end date.
 * @property isInRange Whether [date] is strictly between the start and end dates.
 * @property tags The tags attached to [date].
 */
@Immutable
data class DayCell(
    val date: LocalDate,
    val dayText: String,
    val isToday: Boolean,
    val isWeekend: Boolean,
    val isEnabled: Boolean,
    val isRangeStart: Boolean,
    val isRangeEnd: Boolean,
    val isInRange: Boolean,
    val tags: List<CalendarTag>,
)
