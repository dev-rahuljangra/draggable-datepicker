package com.devrj.draggabledatepicker.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inc.adv.draggabledaterangepicker.datePickerState.DraggableDateRangePickerState
import com.inc.adv.draggabledaterangepicker.model.CalendarColors
import com.inc.adv.draggabledaterangepicker.model.CalendarTextStyles
import com.inc.adv.draggabledaterangepicker.ui.MonthContainer
import com.inc.adv.draggabledaterangepicker.utils.CalendarDefaults
import com.inc.adv.draggabledaterangepicker.utils.Constant
import java.time.DayOfWeek

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DraggableDateRangePicker(
    modifier: Modifier = Modifier,
    state: DraggableDateRangePickerState,
    colors: CalendarColors = CalendarDefaults.colors(),
    calendarTextStyles: CalendarTextStyles = CalendarDefaults.textStyles(),
    weekendDays: List<DayOfWeek> = listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
    startYear: Int = 2023,
    endYear: Int = 2030,
    cellSize: Dp = Constant.CELL_SIZE
) {
    val finalStyles = remember(colors, calendarTextStyles) {
        calendarTextStyles.resolve(colors)
    }
    // Generate simple list of (Year, Month) pairs for the LazyColumn
    val monthsList = remember(startYear, endYear) {
        val list = mutableListOf<Pair<Int, Int>>()
        for (y in startYear..endYear) {
            for (m in 0..11) {
                list.add(y to m)
            }
        }
        list
    }
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(monthsList) { (year, month) ->
            // Header
            Text(
                text = "${Constant.getMonths()[month]} $year", // Or simpler format
                style = finalStyles.monthTextStyle.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp)
            )
            // Days Header (Su, Mo, Tu...) - Draw this once or inside the canvas?
            // Usually drawn once per month or stickied at top.
            // For now, drawing simpler row:
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .background(Color.White)
                    .padding(vertical = 6.dp, horizontal = 16.dp)
            ) {
                Constant.days.forEach { dayEnum ->
                    Text(
                        text = dayEnum.abbreviation,
                        modifier = Modifier
                            .weight(1f),
                        textAlign = TextAlign.Center,
                        style = finalStyles.dayTextStyle.copy(fontSize = 14.sp)
                    )
                }
            }
            // The Optimized Content
            MonthContainer(
                state = state,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                year = year,
                monthIndex = month,
                colors = colors,
                calendarStyles = finalStyles,
                tags = state.getDateTags(),
                cellSize = cellSize,
                weekendDays = weekendDays
            )
        }
    }

}

