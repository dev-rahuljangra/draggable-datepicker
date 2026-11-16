package com.devrj.draggabledatepicker.model

import androidx.compose.runtime.Stable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle

internal enum class DayTextVariant { STANDARD, WEEKEND, SELECTED, IN_RANGE, DISABLED }

/**
 * Day-number layouts shared by every month and measured the first time each one is drawn, so
 * scrolling a new month into view doesn't measure any text again.
 */
@Stable
internal class DayTextCache(
    private val textMeasurer: TextMeasurer,
    private val dayLabels: List<String>,
    private val standard: TextStyle,
    private val weekend: TextStyle,
    private val selected: TextStyle,
    private val inRange: TextStyle,
    private val disabled: TextStyle,
) {
    private val layouts =
        arrayOfNulls<TextLayoutResult>(dayLabels.size * DayTextVariant.entries.size)

    fun get(day: Int, variant: DayTextVariant): TextLayoutResult {
        val index = (day - 1) * DayTextVariant.entries.size + variant.ordinal
        return layouts[index] ?: textMeasurer.measure(
            text = AnnotatedString(dayLabels[day - 1]),
            style = styleFor(variant),
        ).also { layouts[index] = it }
    }

    private fun styleFor(variant: DayTextVariant): TextStyle = when (variant) {
        DayTextVariant.STANDARD -> standard
        DayTextVariant.WEEKEND -> weekend
        DayTextVariant.SELECTED -> selected
        DayTextVariant.IN_RANGE -> inRange
        DayTextVariant.DISABLED -> disabled
    }
}
