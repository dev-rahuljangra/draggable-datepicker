package com.inc.adv.draggabledaterangepicker.model

import androidx.compose.runtime.Immutable
import java.time.YearMonth

@Immutable
internal data class MonthMetadata(
    val yearMonth: YearMonth,
    val daysInMonth: Int,
    val firstDayOfWeek: Int,
    val firstDayEpoch: Long,
    val nowEpoch: Long,
    val totalCells: Int,
    val isPastMonth: Boolean
)