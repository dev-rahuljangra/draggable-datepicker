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
import java.time.YearMonth


/**
 * Interface representing the state of the [DraggableDateRangePicker].
 * It manages the selected date range and any associated tags for specific dates.
 */
@Stable
interface DraggableDateRangePickerState {

    /** The start date of the selection as an epoch day. Null if nothing is selected. */
    val selectedStartEpochDay: Long?

    /** The end date of the selection as an epoch day. Null if no range is defined. */
    val selectedEndEpochDay: Long?

    /**
     * Logic to select a single date. Handles toggling between start and end selection.
     * @param epochDay The date clicked, as an epoch day.
     */
    fun selectDate(epochDay: Long)

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
     * @param epochDay The date selected, as an epoch day.
     */
    fun onDateSelected(epochDay: Long)

    /**
     * Directly updates the selected range. Useful for drag gestures.
     * @param startEpochDay The start of the range, as an epoch day.
     * @param endEpochDay The end of the range, as an epoch day.
     */
    fun updateDragSelection(
        startEpochDay: Long,
        endEpochDay: Long
    )

    /**
     * Adds a single [CalendarTag] to a specific date without clearing existing tags.
     * @param epochDay The epoch day to attach the tag to.
     * @param tag The tag data to be added.
     */
    fun addTag(epochDay: Long, tag: CalendarTag)

    /** Resets the selection, clearing both start and end dates. */
    fun clearSelection()

    /**
     * Sets the selection directly, swapping the dates if the end is before the start.
     * The picker's date limits and range rules apply to user input only, not to this call.
     * @param startEpochDay The start date as an epoch day, or null to clear the selection.
     * @param endEpochDay The end date as an epoch day, or null for a single date.
     */
    fun setSelection(startEpochDay: Long?, endEpochDay: Long?)

    /** The first month currently on screen, or null when no picker is showing this state. */
    val displayedMonth: YearMonth?

    /**
     * Scrolls the picker to [month], or to the nearest month it shows. If no picker is showing
     * this state yet, the next one opens on that month.
     */
    suspend fun scrollToMonth(month: YearMonth, animated: Boolean = true)
}

/** The selected start date, or null if nothing is selected. */
val DraggableDateRangePickerState.selectedStartDate: LocalDate?
    get() = selectedStartEpochDay?.let(LocalDate::ofEpochDay)

/** The selected end date, or null if no range is defined. */
val DraggableDateRangePickerState.selectedEndDate: LocalDate?
    get() = selectedEndEpochDay?.let(LocalDate::ofEpochDay)

/**
 * Implementation of [DraggableDateRangePickerState].
 */
@Stable
internal class DraggableDateRangePickerStateImpl(
    initialStartEpochDay: Long?,
    initialEndEpochDay: Long?
) : DraggableDateRangePickerState {

    private val _dayTags = mutableStateOf(mapOf<Long, List<CalendarTag>>())

    override var selectedStartEpochDay by mutableStateOf(initialStartEpochDay)
        private set

    override var selectedEndEpochDay by mutableStateOf(initialEndEpochDay)
        private set

    private var scroller by mutableStateOf<CalendarScroller?>(null)
    private var pendingMonth: YearMonth? = null

    override val displayedMonth: YearMonth?
        get() = scroller?.displayedMonth

    override fun selectDate(epochDay: Long) = onDateSelected(epochDay)

    override fun setSelection(startEpochDay: Long?, endEpochDay: Long?) {
        if (startEpochDay != null && endEpochDay != null) {
            selectedStartEpochDay = minOf(startEpochDay, endEpochDay)
            selectedEndEpochDay = maxOf(startEpochDay, endEpochDay)
        } else {
            selectedStartEpochDay = startEpochDay ?: endEpochDay
            selectedEndEpochDay = null
        }
    }

    override suspend fun scrollToMonth(month: YearMonth, animated: Boolean) {
        val current = scroller
        if (current != null) current.scrollTo(month, animated) else pendingMonth = month
    }

    fun attachScroller(newScroller: CalendarScroller) {
        scroller = newScroller
    }

    fun detachScroller(oldScroller: CalendarScroller) {
        if (scroller === oldScroller) scroller = null
    }

    fun consumePendingMonth(): YearMonth? = pendingMonth.also { pendingMonth = null }

    override fun getDateTags(): Map<Long, List<CalendarTag>> = _dayTags.value

    override fun setTags(tags: Map<Long, List<CalendarTag>>) {
        _dayTags.value = tags
    }

    /**
     * Adds a single [CalendarTag] to a specific date without clearing existing tags.
     * @param epochDay The epoch day to attach the tag to.
     * @param tag The tag data to be added.
     */
    override fun addTag(epochDay: Long, tag: CalendarTag) {
        val currentMap = _dayTags.value
        val currentList = currentMap[epochDay] ?: emptyList()
        _dayTags.value = currentMap + (epochDay to (currentList + tag))
    }

    override fun onDateSelected(epochDay: Long) {
        val currentStart = selectedStartEpochDay
        val currentEnd = selectedEndEpochDay

        if (currentStart == null) {
            // Case 0: First interaction ever. Set Start.
            selectedStartEpochDay = epochDay
            selectedEndEpochDay = null
        } else if (currentEnd == null) {
            // Case 1: Start exists, End is null. We are defining the range.
            if (epochDay < currentStart) {
                // Clicked before start? Swap them.
                selectedStartEpochDay = epochDay
                selectedEndEpochDay = currentStart
            } else {
                // Clicked after start? Set End.
                selectedEndEpochDay = epochDay
            }
        } else {
            // Case 2: Both exist. User clicked again.
            // Requirement: "Reset the range to redraw" (Start new selection)
            selectedStartEpochDay = epochDay
            selectedEndEpochDay = null
        }
    }

    override fun updateDragSelection(startEpochDay: Long, endEpochDay: Long) {
        selectedStartEpochDay = startEpochDay
        selectedEndEpochDay = endEpochDay
    }

    override fun clearSelection() {
        selectedStartEpochDay = null
        selectedEndEpochDay = null
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
                        it.selectedStartEpochDay,
                        it.selectedEndEpochDay,
                        // Convert tags to snapshots for saving
                        (it as DraggableDateRangePickerStateImpl)._dayTags.value.mapValues { entry ->
                            entry.value.map { cal -> cal.toSnapshot() }
                        }
                    )
                },
                restore = { savedList ->
                    if (savedList.isEmpty()) return@Saver null

                    DraggableDateRangePickerStateImpl(
                        initialStartEpochDay = savedList[0] as? Long,
                        initialEndEpochDay = savedList[1] as? Long
                    ).apply {
                        @Suppress("UNCHECKED_CAST")
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
 * * @param initialStartEpochDay The start date to select initially, as an epoch day (defaults to today).
 * @param initialEndEpochDay The end date to select initially, as an epoch day (defaults to null).
 * @return A state object that will be saved across activity recreation.
 */
@Composable
fun rememberDraggableDateRangePickerState(
    initialStartEpochDay: Long? = LocalDate.now().toEpochDay(),
    initialEndEpochDay: Long? = null
): DraggableDateRangePickerState {
    return rememberSaveable(
        saver = DraggableDateRangePickerStateImpl.Saver
    ) {
        DraggableDateRangePickerStateImpl(
            initialStartEpochDay,
            initialEndEpochDay
        )
    }
}