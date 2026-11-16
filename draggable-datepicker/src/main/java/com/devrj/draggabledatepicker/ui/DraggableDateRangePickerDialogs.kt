package com.devrj.draggabledatepicker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.devrj.draggabledatepicker.R
import com.devrj.draggabledatepicker.datePickerState.DraggableDateRangePickerState
import com.devrj.draggabledatepicker.datePickerState.rememberDraggableDateRangePickerState
import com.devrj.draggabledatepicker.datePickerState.selectedEndDate
import com.devrj.draggabledatepicker.datePickerState.selectedStartDate
import com.devrj.draggabledatepicker.model.DateRangePreset
import com.devrj.draggabledatepicker.utils.CalendarDefaults
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

/**
 * Shows the picker in a full-screen dialog, with preset shortcuts and confirm and cancel buttons.
 *
 * @param onDismissRequest Called when the user cancels or presses back.
 * @param onConfirm Called with the selected dates when the user confirms. The end date is null if
 * one date is selected. The dialog doesn't close itself, so hide it from this callback.
 * @param state Holds the selection.
 * @param presets The shortcuts to show. A preset outside [minDate] and [maxDate] is disabled.
 * @param minDate The earliest date a preset can select.
 * @param maxDate The latest date a preset can select.
 * @param pickerContent The picker to show. Pass your own to customize it, using the same [state].
 */
@Composable
fun DraggableDateRangePickerDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (start: LocalDate, end: LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
    state: DraggableDateRangePickerState = rememberDraggableDateRangePickerState(
        initialStartEpochDay = null,
    ),
    presets: List<DateRangePreset> = CalendarDefaults.presets(),
    minDate: LocalDate? = CalendarDefaults.today(),
    maxDate: LocalDate? = null,
    pickerContent: @Composable (Modifier) -> Unit = { pickerModifier ->
        DraggableDateRangePicker(
            modifier = pickerModifier,
            state = state,
            minDate = minDate,
            maxDate = maxDate,
        )
    },
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(modifier = modifier.fillMaxWidth().fillMaxHeight()) {
            PickerPanel(state, presets, minDate, maxDate, onDismissRequest, onConfirm, pickerContent)
        }
    }
}

/**
 * Shows the picker in a modal bottom sheet, with preset shortcuts and confirm and cancel buttons.
 * The parameters are the same as for [DraggableDateRangePickerDialog].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DraggableDateRangePickerBottomSheet(
    onDismissRequest: () -> Unit,
    onConfirm: (start: LocalDate, end: LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
    state: DraggableDateRangePickerState = rememberDraggableDateRangePickerState(
        initialStartEpochDay = null,
    ),
    presets: List<DateRangePreset> = CalendarDefaults.presets(),
    minDate: LocalDate? = CalendarDefaults.today(),
    maxDate: LocalDate? = null,
    pickerContent: @Composable (Modifier) -> Unit = { pickerModifier ->
        DraggableDateRangePicker(
            modifier = pickerModifier,
            state = state,
            minDate = minDate,
            maxDate = maxDate,
        )
    },
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        // A half-open sheet would leave too little room for a scrolling calendar.
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        PickerPanel(state, presets, minDate, maxDate, onDismissRequest, onConfirm, pickerContent)
    }
}

@Composable
private fun PickerPanel(
    state: DraggableDateRangePickerState,
    presets: List<DateRangePreset>,
    minDate: LocalDate?,
    maxDate: LocalDate?,
    onDismissRequest: () -> Unit,
    onConfirm: (LocalDate, LocalDate?) -> Unit,
    pickerContent: @Composable (Modifier) -> Unit,
) {
    Column(Modifier.fillMaxWidth().fillMaxHeight()) {
        if (presets.isNotEmpty()) {
            PresetRow(state, presets, minDate, maxDate)
        }
        pickerContent(Modifier.weight(1f).fillMaxWidth())
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.ddp_dismiss))
            }
            val start = state.selectedStartDate
            TextButton(
                onClick = { start?.let { onConfirm(it, state.selectedEndDate) } },
                enabled = start != null,
            ) {
                Text(stringResource(R.string.ddp_confirm))
            }
        }
    }
}

@Composable
private fun PresetRow(
    state: DraggableDateRangePickerState,
    presets: List<DateRangePreset>,
    minDate: LocalDate?,
    maxDate: LocalDate?,
) {
    val today = CalendarDefaults.today()
    val scope = rememberCoroutineScope()
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(presets, key = { it.label }) { preset ->
            val range = preset.range(today)
            val startEpoch = range.start.toEpochDay()
            val endEpoch = range.endInclusive.toEpochDay()
            val isSingleDay = startEpoch == endEpoch
            val isAllowed = (minDate == null || range.start >= minDate) &&
                (maxDate == null || range.endInclusive <= maxDate)
            FilterChip(
                selected = state.selectedStartEpochDay == startEpoch &&
                    (state.selectedEndEpochDay ?: startEpoch) == endEpoch,
                enabled = isAllowed,
                onClick = {
                    state.setSelection(startEpoch, endEpoch.takeUnless { isSingleDay })
                    scope.launch { state.scrollToMonth(YearMonth.from(range.start)) }
                },
                label = { Text(preset.label, style = MaterialTheme.typography.labelLarge) },
            )
        }
    }
}
