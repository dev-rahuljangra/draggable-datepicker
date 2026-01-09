package com.inc.adv.draggabledaterangepicker.utils

import androidx.compose.ui.unit.dp
import com.inc.adv.draggabledaterangepicker.enums.Days
import com.inc.adv.draggabledaterangepicker.enums.Days.FRIDAY
import com.inc.adv.draggabledaterangepicker.enums.Days.MONDAY
import com.inc.adv.draggabledaterangepicker.enums.Days.SATURDAY
import com.inc.adv.draggabledaterangepicker.enums.Days.SUNDAY
import com.inc.adv.draggabledaterangepicker.enums.Days.THURSDAY
import com.inc.adv.draggabledaterangepicker.enums.Days.TUESDAY
import com.inc.adv.draggabledaterangepicker.enums.Days.WEDNESDAY

import java.util.Calendar

internal object Constant {
    private const val repeatCount: Int = 1
    const val GRID_SIZE = 7
    val CELL_SIZE = 56.dp

    val days = listOf(
        MONDAY,
        TUESDAY,
        WEDNESDAY,
        THURSDAY,
        FRIDAY,
        SATURDAY,
        SUNDAY
    )

    private val monthNames = listOf(
        "January",
        "February",
        "March",
        "April",
        "May",
        "June",
        "July",
        "August",
        "September",
        "October",
        "November",
        "December",
    )

    fun getMonths(): List<String> {
        val list = mutableListOf<String>()
        for (i in 1..repeatCount) {
            list.addAll(monthNames)
        }
        return list
    }

    fun getMiddleOfMonth(): Int {
        return 12 * (repeatCount / 2)
    }


    private fun getFirstDayOfMonth(month: Int, year: Int): Days {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1)
        return Days.get(calendar[Calendar.DAY_OF_WEEK])
    }

    val years = List(repeatCount) { it + Calendar.getInstance()[Calendar.YEAR] - 100 }
}