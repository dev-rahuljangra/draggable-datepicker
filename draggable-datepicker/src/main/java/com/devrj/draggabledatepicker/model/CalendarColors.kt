package com.inc.adv.draggabledaterangepicker.model

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Immutable

@Immutable
data class CalendarColors(
    val containerColor: Color,
    val contentColor: Color,
    val cellBorderColor: Color,
    val selectedDateContainerColor: Color,
    val selectedDateContentColor: Color,
    val selectedRangeContainerColor: Color,
    val selectedRangeContentColor: Color,
    val disabledContainerColor: Color,
    val disabledContentColor: Color,
    val weekendContainerColor: Color,
    val weekendContentColor: Color,
    val tagContentColor : Color,
    val tagContainerColor : Color
)