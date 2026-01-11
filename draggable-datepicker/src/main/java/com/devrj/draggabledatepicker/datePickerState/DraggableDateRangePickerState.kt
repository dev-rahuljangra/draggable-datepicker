package com.inc.adv.draggabledaterangepicker.datePickerState

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.inc.adv.draggabledaterangepicker.model.CalendarTag
import java.time.LocalDate

@Stable
interface DraggableDateRangePickerState {

    val selectedStartDateMillis: Long?
    val selectedEndDateMillis: Long?


    fun selectDate(dateMillis: Long)
    fun setTags(tags: Map<Long, List<CalendarTag>>)

    fun getDateTags(): Map<Long, List<CalendarTag>>
    fun onDateSelected(newSelectedDateMills: Long)


    fun updateDragSelection(
        startDateMillis: Long,
        endDateMillis: Long
    )

    fun clearSelection()
}

@Stable
internal class DraggableDateRangePickerStateImpl(
    initialStartDateMillis: Long?,
    initialEndDateMillis: Long?
) : DraggableDateRangePickerState {

    // In your State class
    private val _dayTags = mutableStateMapOf<Long, List<CalendarTag>>()


    override var selectedStartDateMillis by mutableStateOf(initialStartDateMillis)
        private set

    override var selectedEndDateMillis by mutableStateOf(initialEndDateMillis)
        private set

    override fun selectDate(dateMillis: Long) {
        when {
            selectedStartDateMillis == null -> {
                selectedStartDateMillis = dateMillis
                selectedEndDateMillis = null
            }

            selectedEndDateMillis == null -> {
                if (dateMillis < selectedStartDateMillis!!) {
                    selectedEndDateMillis = selectedStartDateMillis
                    selectedStartDateMillis = dateMillis
                } else {
                    selectedEndDateMillis = dateMillis
                }
            }

            else -> {
                selectedStartDateMillis = dateMillis
                selectedEndDateMillis = null
            }
        }
    }


    override fun getDateTags(): Map<Long, List<CalendarTag>> {
        return _dayTags
    }

    override fun setTags(tags: Map<Long, List<CalendarTag>>) {
        _dayTags.clear()
        _dayTags.putAll(tags)
    }

    // Helper to add a single tag easily
    fun addTag(dateEpoch: Long, tag: CalendarTag) {
        val current = _dayTags[dateEpoch] ?: emptyList()
        _dayTags[dateEpoch] = current + tag
    }

    // Updates the range based on a new selection
    override fun onDateSelected(newSelectedDateMills: Long) {
        val currentStart = selectedStartDateMillis
        val currentEnd = selectedEndDateMillis

        if (currentStart == null) {
            // Case 0: First interaction ever. Set Start.
            selectedStartDateMillis = newSelectedDateMills
            selectedEndDateMillis = null
        } else if (currentEnd == null) {
            // Case 1: Start exists, End is null. We are defining the range.
            if (newSelectedDateMills < currentStart) {
                // Clicked before start? Swap them.
                selectedStartDateMillis = newSelectedDateMills
                selectedEndDateMillis = currentStart
            } else {
                // Clicked after start? Set End.
                selectedEndDateMillis = newSelectedDateMills
            }
        } else {
            // Case 2: Both exist. User clicked again.
            // Requirement: "Reset the range to redraw" (Start new selection)
            selectedStartDateMillis = newSelectedDateMills
            selectedEndDateMillis = null
        }
    }


    override fun updateDragSelection(
        startDateMillis: Long,
        endDateMillis: Long
    ) {
        selectedStartDateMillis = startDateMillis
        selectedEndDateMillis = endDateMillis
    }

    override fun clearSelection() {
        selectedStartDateMillis = null
        selectedEndDateMillis = null
    }

    companion object {
        val Saver: Saver<DraggableDateRangePickerState, List<Any?>> =
            Saver(
                save = {
                    listOf(
                        it.selectedStartDateMillis,
                        it.selectedEndDateMillis,
                        (it as DraggableDateRangePickerStateImpl)._dayTags.toMap()
                    )
                },
                restore = {
                    DraggableDateRangePickerStateImpl(
                        initialStartDateMillis = it[0] as Long?,
                        initialEndDateMillis = it[1] as Long?
                    ).apply {
                        @Suppress("UNCHECKED_CAST")
                        _dayTags.putAll(it[2] as Map<Long, List<CalendarTag>>)
                    }
                }
            )
    }
}

@Composable
fun rememberDraggableDateRangePickerState(
    initialStartDateMillis: Long? = LocalDate.now().toEpochDay(),
    initialEndDateMillis: Long? = null
): DraggableDateRangePickerState {
    return rememberSaveable(
        saver = DraggableDateRangePickerStateImpl.Saver
    ) {
        DraggableDateRangePickerStateImpl(
            initialStartDateMillis,
            initialEndDateMillis
        )
    }
}
