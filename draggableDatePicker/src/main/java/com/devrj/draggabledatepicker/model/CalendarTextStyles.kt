package com.inc.adv.draggabledaterangepicker.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.TextStyle

@Immutable
data class CalendarTextStyles(
    val monthTextStyle: TextStyle,
    val dayTextStyle: TextStyle,
    val selectedDayTextStyle: TextStyle,
    val weekendTextStyle: TextStyle,
    val disabledDayTextStyle: TextStyle,
    val tagTextStyle: TextStyle
) {
    /**
     * Merges these styles with the provided colors.
     * The color defined in [colors] is applied ONLY if the TextStyle doesn't already have a specific color.
     */
    fun resolve(colors: CalendarColors): CalendarTextStyles {
        return CalendarTextStyles(
            monthTextStyle = monthTextStyle.copy(
                color = monthTextStyle.color.takeOrElse { colors.contentColor }
            ),
            dayTextStyle = dayTextStyle.copy(
                color = dayTextStyle.color.takeOrElse { colors.contentColor }
            ),
            selectedDayTextStyle = selectedDayTextStyle.copy(
                color = selectedDayTextStyle.color.takeOrElse { colors.selectedDateContentColor }
            ),
            weekendTextStyle = weekendTextStyle.copy(
                color = weekendTextStyle.color.takeOrElse { colors.weekendContentColor }
            ),
            disabledDayTextStyle = disabledDayTextStyle.copy(
                color = disabledDayTextStyle.color.takeOrElse { colors.disabledContentColor }
            ),
            tagTextStyle = tagTextStyle.copy(tagTextStyle.color.takeOrElse { colors.tagContentColor })
        )
    }
}