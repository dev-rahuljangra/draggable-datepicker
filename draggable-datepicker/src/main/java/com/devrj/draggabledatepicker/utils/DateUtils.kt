package com.inc.adv.draggabledaterangepicker.utils

import java.time.LocalDate


object DateUtils {
    // Converts Year/Month/Day to a single integer (Days since 1970-01-01)
    fun dateToEpochDay(year: Int, month: Int, day: Int): Long {
        // month is 1-based in java.time, but 0-based in your setup. Adjust accordingly.
        // Assuming your input month is 0-based (Jan = 0)
        return LocalDate.of(year, month + 1, day).toEpochDay()
    }

    fun epochDayToDate(epoch: Long): LocalDate {
        return LocalDate.ofEpochDay(epoch)
    }

    // Fast check if a day is within a range
    fun isDayInRange(currentEpoch: Long, startEpoch: Long?, endEpoch: Long?): Boolean {
        if (startEpoch == null || endEpoch == null) return false
        val min = minOf(startEpoch, endEpoch)
        val max = maxOf(startEpoch, endEpoch)
        return currentEpoch in min..max
    }
}