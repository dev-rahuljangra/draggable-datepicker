package com.inc.adv.draggabledaterangepicker.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.inc.adv.draggabledaterangepicker.enums.TagAlignment
import com.inc.adv.draggabledaterangepicker.enums.TagType

data class CalendarTag(
    val text: String,
    val alignment: TagAlignment = TagAlignment.BOTTOM_CENTER, // Default
    val fontSize: TextUnit = 8.sp,
    val type: TagType = TagType.NONE,
    val color: Color? = null,
    val backgroundColor: Color? = null,
)