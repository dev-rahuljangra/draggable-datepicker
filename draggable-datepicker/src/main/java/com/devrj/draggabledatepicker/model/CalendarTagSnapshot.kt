package com.devrj.draggabledatepicker.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.sp
import com.devrj.draggabledatepicker.enums.TagAlignment
import com.devrj.draggabledatepicker.enums.TagType

@Immutable
data class CalendarTagSnapshot(
    val text: String,
    val alignment: Int,
    val fontSizeSp: Float,
    val type: Int,
    val colorArgb: Int?,
    val backgroundColorArgb: Int?
)
fun CalendarTag.toSnapshot(): CalendarTagSnapshot =
    CalendarTagSnapshot(
        text = text,
        alignment = alignment.ordinal,
        fontSizeSp = fontSize.value,
        type = type.ordinal,
        colorArgb = color?.toArgb(),
        backgroundColorArgb = backgroundColor?.toArgb()
    )

fun CalendarTagSnapshot.toTag(): CalendarTag =
    CalendarTag(
        text = text,
        alignment = TagAlignment.entries[alignment],
        fontSize = fontSizeSp.sp,
        type = TagType.entries[type],
        color = colorArgb?.let { Color(it) },
        backgroundColor = backgroundColorArgb?.let { Color(it) }
    )
