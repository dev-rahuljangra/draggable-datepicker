package com.devrj.draggabledatepicker.enums

/** How taps and drags change the selection in a date picker. */
enum class DateSelectionMode {
    /** The user picks a start and an end date, and can drag either end of the range. */
    RANGE,

    /** The user picks one date. Drag handles are not shown. */
    SINGLE
}
