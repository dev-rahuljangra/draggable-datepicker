package com.devrj.draggabledatepicker.model

import com.devrj.draggabledatepicker.enums.Days

internal data class Month(
    val name: String,
    val numberOfDays: Int,
    val firstDayOfMonth: Days,
    val number: Int
)