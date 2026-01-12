package com.devrj.draggabledatepicker.datePickerState

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.devrj.draggabledatepicker.model.CalendarTag
import com.devrj.draggabledatepicker.model.CalendarTagSnapshot
import com.devrj.draggabledatepicker.model.toSnapshot
import com.devrj.draggabledatepicker.model.toTag
import java.time.LocalDate


/**
 * Interface representing the state of the [DraggableDateRangePicker].
 * It manages the selected date range and any associated tags for specific dates.
 */
@Stable
interface DraggableDateRangePickerState {

    /** The start date of the selection in milliseconds. Null if nothing is selected. */
    val selectedStartDateMillis: Long?

    /** The end date of the selection in milliseconds. Null if no range is defined. */
    val selectedEndDateMillis: Long?

    /**
     * Logic to select a single date. Handles toggling between start and end selection.
     * @param dateMillis The date clicked in milliseconds.
     */
    fun selectDate(dateMillis: Long)

    /**
     * Replaces the current set of [CalendarTag]s with the provided map.
     * @param tags A map where the key is the epoch day and the value is a list of tags.
     */
    fun setTags(tags: Map<Long, List<CalendarTag>>)

    /**
     * Retrieves all current date tags.
     * @return A map of epoch day to its list of associated tags.
     */
    fun getDateTags(): Map<Long, List<CalendarTag>>

    /**
     * Updates the selection state based on a user tap.
     * If a range exists, it resets to a new start date.
     * @param newSelectedDateMills The date selected in milliseconds.
     */
    fun onDateSelected(newSelectedDateMills: Long)

    /**
     * Directly updates the selected range. Useful for drag gestures.
     * @param startDateMillis The starting point of the drag.
     * @param endDateMillis The ending point of the drag.
     */
    fun updateDragSelection(
        startDateMillis: Long,
        endDateMillis: Long
    )

    /**
     * Adds a single [CalendarTag] to a specific date without clearing existing tags.
     * @param dateEpoch The epoch day to attach the tag to.
     * @param tag The tag data to be added.
     */
    fun addTag(dateEpoch: Long, tag: CalendarTag)

    /** Resets the selection, clearing both start and end dates. */
    fun clearSelection()
}

/**
 * Implementation of [DraggableDateRangePickerState].
 */
@Stable
internal class DraggableDateRangePickerStateImpl(
    initialStartDateMillis: Long?,
    initialEndDateMillis: Long?
) : DraggableDateRangePickerState {

    private val _dayTags = mutableStateOf(mapOf<Long, List<CalendarTag>>())

    override var selectedStartDateMillis by mutableStateOf(initialStartDateMillis)
        private set

    override var selectedEndDateMillis by mutableStateOf(initialEndDateMillis)
        private set

    override fun selectDate(dateMillis: Long) {
        // Logic implementation...
    }

    override fun getDateTags(): Map<Long, List<CalendarTag>> = _dayTags.value

    override fun setTags(tags: Map<Long, List<CalendarTag>>) {
        _dayTags.value = tags
    }

    /**
     * Adds a single [CalendarTag] to a specific date without clearing existing tags.
     * @param dateEpoch The epoch day to attach the tag to.
     * @param tag The tag data to be added.
     */
    override fun addTag(dateEpoch: Long, tag: CalendarTag) {
        val currentMap = _dayTags.value
        val currentList = currentMap[dateEpoch] ?: emptyList()
        _dayTags.value = currentMap + (dateEpoch to (currentList + tag))
    }

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

    override fun updateDragSelection(startDateMillis: Long, endDateMillis: Long) {
        selectedStartDateMillis = startDateMillis
        selectedEndDateMillis = endDateMillis
    }

    override fun clearSelection() {
        selectedStartDateMillis = null
        selectedEndDateMillis = null
    }

    companion object {
        /**
         * A [Saver] implementation to allow [DraggableDateRangePickerState] to survive
         * process death or configuration changes.
         */
        val Saver: Saver<DraggableDateRangePickerState, List<Any?>> =
            Saver(
                save = {
                    listOf(
                        it.selectedStartDateMillis,
                        it.selectedEndDateMillis,
                        // Convert tags to snapshots for saving
                        (it as DraggableDateRangePickerStateImpl)._dayTags.value.mapValues { entry ->
                            entry.value.map { cal -> cal.toSnapshot() }
                        }
                    )
                },
                restore = { savedList ->
                    // SAFETY: Check if the list exists and has the expected size
                    if (savedList.isEmpty()) return@Saver null

                    DraggableDateRangePickerStateImpl(
                        initialStartDateMillis = savedList[0] as? Long,
                        initialEndDateMillis = savedList[1] as? Long
                    ).apply {
                        // SAFETY: Use safe casting (as?) to prevent "Source must not be null"
                        val savedMap = savedList[2] as? Map<Long, List<CalendarTagSnapshot>>
                        if (savedMap != null) {
                            _dayTags.value = savedMap.mapValues { entry ->
                                entry.value.map { snap -> snap.toTag() }
                            }
                        }
                    }
                }
            )
    }
}

/**
 * Creates and remembers a [DraggableDateRangePickerState].
 * * @param initialStartDateMillis The start date to be initially selected (defaults to today).
 * @param initialEndDateMillis The end date to be initially selected (defaults to null).
 * @return A state object that will be saved across activity recreation.
 */
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