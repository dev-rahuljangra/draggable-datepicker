package com.devrj.datepicker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devrj.datepicker.ui.theme.DatePickerTheme
import com.devrj.draggabledatepicker.datePickerState.rememberDraggableDateRangePickerState
import com.devrj.draggabledatepicker.enums.CalendarOrientation
import com.devrj.draggabledatepicker.enums.DateSelectionMode
import com.devrj.draggabledatepicker.model.CalendarTag
import com.devrj.draggabledatepicker.model.DayCell
import com.devrj.draggabledatepicker.ui.DraggableDateRangePicker
import com.devrj.draggabledatepicker.ui.DraggableDateRangePickerBottomSheet
import com.devrj.draggabledatepicker.ui.DraggableDateRangePickerDialog
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DatePickerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PickerShowcase(Modifier.padding(innerPadding))
                }
            }
        }
    }
}

/** Shows each picker feature behind a chip, so they can be tried together. */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun PickerShowcase(modifier: Modifier = Modifier) {
    val state = rememberDraggableDateRangePickerState()
    val scope = rememberCoroutineScope()
    val today = LocalDate.now()

    var horizontal by rememberSaveable { mutableStateOf(false) }
    var singleDate by rememberSaveable { mutableStateOf(false) }
    var allowPast by rememberSaveable { mutableStateOf(false) }
    var blockWeekends by rememberSaveable { mutableStateOf(false) }
    var limitLength by rememberSaveable { mutableStateOf(false) }
    var customCells by rememberSaveable { mutableStateOf(false) }
    var perMonthHeader by rememberSaveable { mutableStateOf(false) }
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var showSheet by rememberSaveable { mutableStateOf(false) }
    var summary by rememberSaveable { mutableStateOf("Tap a date, then drag either end") }

    LaunchedEffect(Unit) {
        state.setTags(
            mapOf(
                today.plusDays(2).toEpochDay() to listOf(CalendarTag.pill("Sale")),
                today.plusDays(3).toEpochDay() to listOf(CalendarTag.price("$120")),
                today.plusDays(4).toEpochDay() to listOf(CalendarTag.price("$95")),
                today.plusDays(9).toEpochDay() to listOf(CalendarTag("A very long holiday name")),
            ),
        )
    }

    Column(modifier.fillMaxSize()) {
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(horizontal, { horizontal = !horizontal }, { Text("Horizontal") })
            FilterChip(singleDate, { singleDate = !singleDate }, { Text("Single date") })
            FilterChip(allowPast, { allowPast = !allowPast }, { Text("Past dates") })
            FilterChip(blockWeekends, { blockWeekends = !blockWeekends }, { Text("Block weekends") })
            FilterChip(limitLength, { limitLength = !limitLength }, { Text("2-7 days") })
            FilterChip(customCells, { customCells = !customCells }, { Text("Custom cells") })
            FilterChip(perMonthHeader, { perMonthHeader = !perMonthHeader }, { Text("Weekdays per month") })
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = {
                scope.launch { state.scrollToMonth(YearMonth.from(LocalDate.now())) }
            }) { Text("Today") }
            OutlinedButton(onClick = { state.clearSelection() }) { Text("Clear") }
            OutlinedButton(onClick = { showDialog = true }) { Text("Dialog") }
            OutlinedButton(onClick = { showSheet = true }) { Text("Sheet") }
        }
        Text(
            text = summary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        DraggableDateRangePicker(
            modifier = Modifier.weight(1f),
            state = state,
            startYear = today.year - 1,
            endYear = today.year + 3,
            minDate = if (allowPast) null else today,
            isDateSelectable = if (blockWeekends) {
                { it.dayOfWeek != DayOfWeek.SATURDAY && it.dayOfWeek != DayOfWeek.SUNDAY }
            } else {
                { true }
            },
            selectionMode = if (singleDate) DateSelectionMode.SINGLE else DateSelectionMode.RANGE,
            minRangeDays = if (limitLength) 2 else 1,
            maxRangeDays = if (limitLength) 7 else null,
            orientation = if (horizontal) {
                CalendarOrientation.HORIZONTAL
            } else {
                CalendarOrientation.VERTICAL
            },
            stickyWeekdayHeader = !perMonthHeader,
            onRangeChange = { start, end -> summary = describe(start, end) },
            dayContent = if (customCells) {
                { cell -> CustomDay(cell) }
            } else {
                null
            },
        )
    }

    if (showDialog) {
        DraggableDateRangePickerDialog(
            onDismissRequest = { showDialog = false },
            onConfirm = { start, end ->
                summary = "Dialog: ${describe(start, end)}"
                showDialog = false
            },
        )
    }
    if (showSheet) {
        DraggableDateRangePickerBottomSheet(
            onDismissRequest = { showSheet = false },
            onConfirm = { start, end ->
                summary = "Sheet: ${describe(start, end)}"
                showSheet = false
            },
        )
    }
}

@Composable
private fun CustomDay(cell: DayCell) {
    val isSelected = cell.isRangeStart || cell.isRangeEnd
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = cell.dayText,
            fontWeight = if (cell.isToday) FontWeight.Bold else FontWeight.Normal,
            color = when {
                isSelected -> MaterialTheme.colorScheme.onPrimary
                cell.isEnabled -> MaterialTheme.colorScheme.onSurface
                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            },
        )
        if (cell.tags.isNotEmpty()) {
            Text(
                text = "•",
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

private fun describe(start: LocalDate?, end: LocalDate?): String = when {
    start == null -> "Nothing selected"
    end == null -> "$start"
    else -> "$start to $end"
}
