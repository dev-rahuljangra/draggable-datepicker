package com.inc.adv.draggabledaterangepicker.model

import com.inc.adv.draggabledaterangepicker.enums.Days

internal data class Month(
    val name: String,
    val numberOfDays: Int,
    val firstDayOfMonth: Days,
    val number: Int
)