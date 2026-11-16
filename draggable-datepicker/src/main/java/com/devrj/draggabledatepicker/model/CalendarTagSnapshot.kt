package com.devrj.draggabledatepicker.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import com.devrj.draggabledatepicker.enums.TagAlignment
import com.devrj.draggabledatepicker.enums.TagStyle
import com.devrj.draggabledatepicker.enums.TagType
import java.io.Serializable

/**
 * A saved form of [CalendarTag]. It must stay [Serializable] so saved instance state can be
 * written to a Bundle when the app goes to the background.
 */
@Immutable
data class CalendarTagSnapshot(
    val text: String,
    val alignment: Int,
    val fontSizeSp: Float?,
    val type: Int,
    val colorArgb: Int?,
    val backgroundColorArgb: Int?,
    val style: Int = TagStyle.TEXT.ordinal,
) : Serializable

fun CalendarTag.toSnapshot(): CalendarTagSnapshot =
    CalendarTagSnapshot(
        text = text,
        alignment = alignment.ordinal,
        fontSizeSp = if (fontSize.isSpecified) fontSize.value else null,
        type = type.ordinal,
        colorArgb = color?.toArgb(),
        backgroundColorArgb = backgroundColor?.toArgb(),
        style = style.ordinal,
    )

fun CalendarTagSnapshot.toTag(): CalendarTag =
    CalendarTag(
        text = text,
        alignment = TagAlignment.entries[alignment],
        fontSize = fontSizeSp?.sp ?: TextUnit.Unspecified,
        type = TagType.entries[type],
        color = colorArgb?.let { Color(it) },
        backgroundColor = backgroundColorArgb?.let { Color(it) },
        style = TagStyle.entries[style],
    )
