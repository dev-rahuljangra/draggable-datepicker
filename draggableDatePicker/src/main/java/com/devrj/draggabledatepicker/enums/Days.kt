package com.inc.adv.draggabledaterangepicker.enums

internal enum class Days(val abbreviation: String, val value: String, val number: Int) {
    MONDAY("MON", "Monday", 1),
    TUESDAY("TUE", "Tuesday", 2),
    WEDNESDAY("WED", "Wednesday", 3),
    THURSDAY("THU", "Thursday", 4),
    FRIDAY("FRI", "Friday", 5),
    SATURDAY("SAT", "Saturday", 6),
    SUNDAY("SUN", "Sunday", 7);

    companion object {
        fun get(number: Int): Days {
            return when (number) {
                1 -> MONDAY
                2 -> TUESDAY
                3 -> WEDNESDAY
                4 -> THURSDAY
                5 -> FRIDAY
                6 -> SATURDAY
                else -> SUNDAY
            }
        }
    }
}