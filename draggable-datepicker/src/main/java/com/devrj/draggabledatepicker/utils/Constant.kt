package com.inc.adv.draggabledaterangepicker.utils

import androidx.compose.ui.unit.dp
import com.devrj.draggabledatepicker.enums.Days
import com.devrj.draggabledatepicker.enums.Days.FRIDAY
import com.devrj.draggabledatepicker.enums.Days.MONDAY
import com.devrj.draggabledatepicker.enums.Days.SATURDAY
import com.devrj.draggabledatepicker.enums.Days.SUNDAY
import com.devrj.draggabledatepicker.enums.Days.THURSDAY
import com.devrj.draggabledatepicker.enums.Days.TUESDAY
import com.devrj.draggabledatepicker.enums.Days.WEDNESDAY

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