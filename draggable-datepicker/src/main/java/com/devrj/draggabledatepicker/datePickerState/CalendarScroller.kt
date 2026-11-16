package com.devrj.draggabledatepicker.datePickerState

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.time.YearMonth

private val AutoScrollEdge = 72.dp
private val AutoScrollMaxSpeedPerSecond = 1400.dp
private const val PAGE_TURN_DWELL_MILLIS = 450L
private const val PAGE_TURN_POLL_MILLIS = 50L

/** Connects the picker state to whichever scrolling container is on screen. */
internal abstract class CalendarScroller(
    private val firstMonth: YearMonth,
    private val monthCount: Int,
) {
    abstract val isScrolling: Boolean
    protected abstract val firstVisibleIndex: Int

    val displayedMonth: YearMonth
        get() = firstMonth.plusMonths(firstVisibleIndex.toLong())

    protected abstract suspend fun scrollToIndex(index: Int, animated: Boolean)

    suspend fun scrollTo(month: YearMonth, animated: Boolean) {
        val offset = (month.year - firstMonth.year) * 12 + (month.monthValue - firstMonth.monthValue)
        scrollToIndex(offset.coerceIn(0, monthCount - 1), animated)
    }

    /**
     * Scrolls while the pointer stays near an edge of the container, calling [onScrolled] so the
     * selection can follow. Runs until cancelled.
     */
    abstract suspend fun autoScroll(
        density: Density,
        isRtl: Boolean,
        pointer: () -> Offset,
        containerSize: () -> IntSize,
        onScrolled: () -> Unit,
    )
}

internal class ListCalendarScroller(
    private val listState: LazyListState,
    firstMonth: YearMonth,
    monthCount: Int,
) : CalendarScroller(firstMonth, monthCount) {
    override val isScrolling: Boolean get() = listState.isScrollInProgress
    override val firstVisibleIndex: Int get() = listState.firstVisibleItemIndex

    override suspend fun scrollToIndex(index: Int, animated: Boolean) {
        if (animated) listState.animateScrollToItem(index) else listState.scrollToItem(index)
    }

    override suspend fun autoScroll(
        density: Density,
        isRtl: Boolean,
        pointer: () -> Offset,
        containerSize: () -> IntSize,
        onScrolled: () -> Unit,
    ) {
        val edge = with(density) { AutoScrollEdge.toPx() }
        val maxSpeed = with(density) { AutoScrollMaxSpeedPerSecond.toPx() }
        var lastFrame = withFrameNanos { it }
        while (true) {
            val frame = withFrameNanos { it }
            val seconds = (frame - lastFrame) / 1_000_000_000f
            lastFrame = frame

            val y = pointer().y
            val height = containerSize().height.toFloat()
            val depth = when {
                y < edge -> -((edge - y) / edge)
                y > height - edge -> (y - (height - edge)) / edge
                else -> 0f
            }.coerceIn(-1f, 1f)

            if (depth != 0f) {
                // Squaring the depth gives fine control near the start of the edge zone.
                val speed = depth * kotlin.math.abs(depth) * maxSpeed
                if (listState.scrollBy(speed * seconds) != 0f) onScrolled()
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
internal class PagerCalendarScroller(
    private val pagerState: PagerState,
    firstMonth: YearMonth,
    monthCount: Int,
) : CalendarScroller(firstMonth, monthCount) {
    override val isScrolling: Boolean get() = pagerState.isScrollInProgress
    override val firstVisibleIndex: Int get() = pagerState.currentPage

    override suspend fun scrollToIndex(index: Int, animated: Boolean) {
        if (animated) pagerState.animateScrollToPage(index) else pagerState.scrollToPage(index)
    }

    override suspend fun autoScroll(
        density: Density,
        isRtl: Boolean,
        pointer: () -> Offset,
        containerSize: () -> IntSize,
        onScrolled: () -> Unit,
    ) {
        val edge = with(density) { AutoScrollEdge.toPx() }
        var dwell = 0L
        var dwellDirection = 0
        while (true) {
            delay(PAGE_TURN_POLL_MILLIS)
            val x = pointer().x
            val visual = when {
                x < edge -> -1
                x > containerSize().width - edge -> 1
                else -> 0
            }
            val direction = if (isRtl) -visual else visual
            if (direction == 0 || direction != dwellDirection) {
                dwell = 0L
                dwellDirection = direction
                continue
            }
            dwell += PAGE_TURN_POLL_MILLIS
            if (dwell >= PAGE_TURN_DWELL_MILLIS) {
                dwell = 0L
                val target = pagerState.currentPage + direction
                if (target in 0 until pagerState.pageCount) {
                    pagerState.animateScrollToPage(target)
                    onScrolled()
                }
            }
        }
    }
}
