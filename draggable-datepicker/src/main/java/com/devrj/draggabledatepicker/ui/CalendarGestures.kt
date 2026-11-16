package com.devrj.draggabledatepicker.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.IntSize
import com.devrj.draggabledatepicker.datePickerState.CalendarScroller
import com.devrj.draggabledatepicker.datePickerState.DraggableDateRangePickerState
import com.devrj.draggabledatepicker.enums.DateSelectionMode
import com.devrj.draggabledatepicker.model.MonthMetadata
import com.devrj.draggabledatepicker.selection.SelectionRules
import com.devrj.draggabledatepicker.selection.dayAtPosition
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

/** Tracks where each composed month grid is, so one gesture can move between months. */
internal class MonthGridRegistry {
    private val grids = HashMap<MonthMetadata, LayoutCoordinates>()
    var containerCoordinates: LayoutCoordinates? = null

    fun update(month: MonthMetadata, coordinates: LayoutCoordinates) {
        grids[month] = coordinates
    }

    fun remove(month: MonthMetadata) {
        grids.remove(month)
    }

    /**
     * Returns the epoch day at [position] in the container. When [nearest] is set, as it is for
     * drags, a position in the side padding or an empty cell resolves to the closest day.
     */
    fun epochAt(position: Offset, isRtl: Boolean, nearest: Boolean): Long? {
        val container = containerCoordinates?.takeIf { it.isAttached } ?: return null
        var best: MonthMetadata? = null
        var bestLocal = Offset.Zero
        var bestSize = IntSize.Zero
        var bestDistance = Float.MAX_VALUE

        for ((month, grid) in grids) {
            if (!grid.isAttached) continue
            val local = grid.localPositionOf(container, position)
            if (local.y < 0f || local.y >= grid.size.height) continue
            val distance = when {
                local.x < 0f -> -local.x
                local.x >= grid.size.width -> local.x - grid.size.width
                else -> 0f
            }
            if (abs(distance) < bestDistance) {
                best = month
                bestLocal = local
                bestSize = grid.size
                bestDistance = distance
            }
        }

        val month = best ?: return null
        if (bestDistance > 0f && !nearest) return null
        val day = dayAtPosition(
            x = bestLocal.x,
            y = bestLocal.y,
            width = bestSize.width.toFloat(),
            height = bestSize.height.toFloat(),
            month = month,
            isRtl = isRtl,
            snapToMonth = nearest,
        ) ?: return null
        return month.firstDayEpoch + (day - 1)
    }
}

/**
 * Handles every touch on the calendar from one place above the scrolling container, because a
 * drag can continue into other months. Handle drags claim their events in the initial pass, before
 * the scrolling container sees them. All other touches are left for it to scroll.
 */
internal fun Modifier.calendarGestures(
    state: DraggableDateRangePickerState,
    rules: State<SelectionRules>,
    registry: MonthGridRegistry,
    scroller: CalendarScroller,
    isRtl: Boolean,
    onDragStep: () -> Unit,
): Modifier = pointerInput(state, registry, scroller, isRtl) {
    coroutineScope {
        val gestureScope = this
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val wasScrolling = scroller.isScrolling
            val downEpoch = registry.epochAt(down.position, isRtl, nearest = false)
            val current = rules.value
            val start = state.selectedStartEpochDay
            val end = state.selectedEndEpochDay

            val isHandle = current.mode == DateSelectionMode.RANGE &&
                downEpoch != null && current.isEnabled(downEpoch) &&
                (downEpoch == start || downEpoch == end)

            if (isHandle) {
                // Holding the far end fixed means the range can flip when dragged past it.
                val anchor = if (downEpoch == start) end ?: downEpoch else start ?: downEpoch
                var edge: Long = downEpoch
                var lastTarget: Long = downEpoch
                var moved = false
                var pointer = down.position
                var released = false
                down.consume()

                fun followPointer() {
                    val clamped = Offset(
                        pointer.x.coerceIn(0f, size.width - 1f),
                        pointer.y.coerceIn(0f, size.height - 1f),
                    )
                    val target = registry.epochAt(clamped, isRtl, nearest = true)
                    if (target == lastTarget) return
                    moved = true
                    // Between months there's no date, so the range holds until the next one.
                    if (target == null) return
                    lastTarget = target
                    val next = rules.value.resolveDrag(anchor, target, edge) ?: return
                    if (next == state.selectionValue()) return
                    val newStart = next.start ?: return
                    val newEnd = next.end ?: return
                    edge = if (newStart == anchor) newEnd else newStart
                    state.updateDragSelection(newStart, newEnd)
                    onDragStep()
                }

                val autoScroll = gestureScope.launch {
                    scroller.autoScroll(
                        density = this@pointerInput,
                        isRtl = isRtl,
                        pointer = { pointer },
                        containerSize = { size },
                        onScrolled = ::followPointer,
                    )
                }
                try {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        change.consume()
                        if (!change.pressed) {
                            released = true
                            break
                        }
                        pointer = change.position
                        followPointer()
                    }
                } finally {
                    autoScroll.cancel()
                }

                // A press that never left its cell is a tap on the handle.
                if (released && !moved) {
                    val tapped = current.resolveTap(state.selectionValue(), downEpoch)
                    state.setSelection(tapped.start, tapped.end)
                }
            } else {
                // The scrolling container consumes this gesture if it becomes a scroll.
                val up = waitForUpOrCancellation()
                if (up != null && !wasScrolling && downEpoch != null) {
                    val upEpoch = registry.epochAt(up.position, isRtl, nearest = false)
                    if (upEpoch == downEpoch) {
                        val tapped = rules.value.resolveTap(state.selectionValue(), downEpoch)
                        state.setSelection(tapped.start, tapped.end)
                        up.consume()
                    }
                }
            }
        }
    }
}
