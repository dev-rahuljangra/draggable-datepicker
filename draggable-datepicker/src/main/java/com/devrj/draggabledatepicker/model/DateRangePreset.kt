package com.devrj.draggabledatepicker.model

import androidx.compose.runtime.Immutable
import java.time.LocalDate

/**
 * A one-tap shortcut shown by the picker dialog and bottom sheet.
 *
 * @property label The text on the chip.
 * @property range Returns the dates to select, given today's date.
 */
@Immutable
class DateRangePreset(
    val label: String,
    val range: (today: LocalDate) -> ClosedRange<LocalDate>,
)
