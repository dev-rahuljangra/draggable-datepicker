package com.devrj.draggabledatepicker.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import com.devrj.draggabledatepicker.enums.TagAlignment
import com.devrj.draggabledatepicker.enums.TagStyle
import com.devrj.draggabledatepicker.enums.TagType

/**
 * A short label drawn inside a day cell.
 *
 * @param text The label. It is ellipsized if it doesn't fit the cell.
 * @param alignment Where the label sits in the cell.
 * @param fontSize Overrides the size from [CalendarTextStyles.tagTextStyle] when specified.
 * @param type What the tag represents. [TagType.PRICE] tags are drawn in a heavier weight.
 * @param color Text color. Defaults to [CalendarColors.tagContentColor].
 * @param backgroundColor Background color. Setting it draws a background for any [style].
 * @param style Whether the label has a background. [TagStyle.PILL] falls back to
 * [CalendarColors.tagContainerColor] when [backgroundColor] is null.
 */
data class CalendarTag(
    val text: String,
    val alignment: TagAlignment = TagAlignment.BOTTOM_CENTER,
    val fontSize: TextUnit = TextUnit.Unspecified,
    val type: TagType = TagType.NONE,
    val color: Color? = null,
    val backgroundColor: Color? = null,
    val style: TagStyle = TagStyle.TEXT,
) {
    companion object {
        /** Creates a label on a rounded background. */
        fun pill(
            text: String,
            alignment: TagAlignment = TagAlignment.BOTTOM_CENTER,
            color: Color? = null,
            backgroundColor: Color? = null,
        ): CalendarTag = CalendarTag(
            text = text,
            alignment = alignment,
            color = color,
            backgroundColor = backgroundColor,
            style = TagStyle.PILL,
        )

        /** Creates a price label, such as `$120`, at the bottom of the cell. */
        fun price(text: String, color: Color? = null): CalendarTag = CalendarTag(
            text = text,
            type = TagType.PRICE,
            color = color,
        )
    }
}
