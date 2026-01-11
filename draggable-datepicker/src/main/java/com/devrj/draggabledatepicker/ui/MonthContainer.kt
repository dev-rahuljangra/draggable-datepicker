package com.inc.adv.draggabledaterangepicker.ui

import android.util.Log
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.inc.adv.draggabledaterangepicker.datePickerState.DraggableDateRangePickerState
import com.inc.adv.draggabledaterangepicker.enums.TagAlignment
import com.inc.adv.draggabledaterangepicker.model.CalendarColors
import com.inc.adv.draggabledaterangepicker.model.CalendarTag
import com.inc.adv.draggabledaterangepicker.model.CalendarTextStyles
import com.inc.adv.draggabledaterangepicker.model.DayTextCache
import com.inc.adv.draggabledaterangepicker.model.MeasuredTag
import com.inc.adv.draggabledaterangepicker.model.MonthMetadata
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalTextApi::class)
@Composable
internal fun MonthContainer(
    state: DraggableDateRangePickerState,
    year: Int,
    monthIndex: Int,
    cellSize: Dp,
    tags: Map<Long, List<CalendarTag>>?,
    modifier: Modifier = Modifier,
    colors: CalendarColors,
    calendarStyles: CalendarTextStyles,
    weekendDays: List<DayOfWeek>,
    today: LocalDate = LocalDate.now()
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    // --- 1. PRE-CALCULATIONS (Outside Draw Loop) ---
    val monthState = remember(year, monthIndex) {
        val yearMonth = YearMonth.of(year, monthIndex + 1)
        val daysInMonth = yearMonth.lengthOfMonth()
        val firstDayOfMonth = yearMonth.atDay(1)
        // Subtract 1 from the ISO value (Mon=1 becomes 0, Sun=7 becomes 6)
        val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value - 1
        val firstDayEpoch = firstDayOfMonth.toEpochDay()
        val nowEpoch = today.toEpochDay()

        MonthMetadata(
            yearMonth = yearMonth,
            daysInMonth = daysInMonth,
            firstDayOfWeek = firstDayOfWeek,
            firstDayEpoch = firstDayEpoch,
            nowEpoch = nowEpoch,
            totalCells = daysInMonth + firstDayOfWeek,
            isPastMonth = yearMonth.atEndOfMonth().isBefore(today)
        )
    }

    // --- 2. TEXT CACHING ---
    // Pre-measure day numbers (1..31) so we don't measure on every frame of a drag
    val dayNumberLayouts = remember(
        calendarStyles.dayTextStyle,
        calendarStyles.weekendTextStyle,
        calendarStyles.selectedDayTextStyle,
        calendarStyles.disabledDayTextStyle
    ) {
        val standard = calendarStyles.dayTextStyle
        val weekend = calendarStyles.weekendTextStyle
        val selected = calendarStyles.selectedDayTextStyle
        val disabled = calendarStyles.disabledDayTextStyle

        (1..31).associateWith { day ->
            // We cache 4 variants per day to avoid style resolution during draw
            DayTextCache(
                standard = textMeasurer.measure(AnnotatedString(day.toString()), standard),
                weekend = textMeasurer.measure(AnnotatedString(day.toString()), weekend),
                selected = textMeasurer.measure(AnnotatedString(day.toString()), selected),
                disabled = textMeasurer.measure(AnnotatedString(day.toString()), disabled)
            )
        }
    }

    // Pre-measure tags if they exist.
    // Note: If tags change frequently, this key needs to be robust.
    val tagLayouts = remember(tags, density) {
        tags?.mapValues { (_, tagList) ->
            tagList.map { tag ->
                val layout = textMeasurer.measure(
                    text = AnnotatedString(tag.text),
                    style = TextStyle(
                        color = tag.color ?: colors.tagContentColor,
                        fontSize = tag.fontSize,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                MeasuredTag(
                    textLayoutResult = layout,
                    alignment = tag.alignment,
                    backgroundColor = tag.backgroundColor ?: colors.tagContainerColor,
                    color = tag.color ?: colors.tagContentColor
                )
            }
        }
    }

    // Layout Dimensions
    val cellHeightPx = with(density) { cellSize.toPx() }
    val numRows = (monthState.totalCells + 6) / 7
    val canvasHeight = cellSize * numRows

    // Drawing Constants (Converted to Px once)
    val strokeWidthPx = with(density) { 1.dp.toPx() }
    val barWidthPx = with(density) { 2.dp.toPx() }
    val barHeightPx = with(density) { 12.dp.toPx() }
    val barSpacingPx = with(density) { 4.dp.toPx() }
    val barPaddingPx = with(density) { 6.dp.toPx() }
    val cornerRadius = CornerRadius(10f, 10f)

    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(canvasHeight)
            .drawWithCache {
                val cellWidthPx = size.width / 7f
                onDrawBehind {
                    // --- 3. OPTIMIZED DRAW LOOP ---
                    for (day in 1..monthState.daysInMonth) {
                        val gridIndex = day + monthState.firstDayOfWeek - 1
                        val row = gridIndex / 7
                        val col = gridIndex % 7

                        val x = col * cellWidthPx
                        val y = row * cellHeightPx

                        // Math-only date calculation (No Allocations)
                        val currentEpoch = monthState.firstDayEpoch + (day - 1)
                        val isPastDate = currentEpoch < monthState.nowEpoch

                        // Range Logic
                        val isStart = !isPastDate && state.selectedStartDateMillis == currentEpoch
                        val isEnd = !isPastDate && state.selectedEndDateMillis == currentEpoch

                        val dayOfWeekVal = (gridIndex % 7) + 1 // 1=Mon ... 7=Sun
                        // Adjust logic if your config.weekendDays uses java.time.DayOfWeek
                        // Assuming config.weekendDays contains DayOfWeek objects:
                        val isWeekend =
                            !isPastDate && weekendDays.any { it.value == dayOfWeekVal }

                        val alpha = if (isPastDate) 0.3f else 1f

                        // --- Draw Backgrounds ---
                        drawCellBackground(
                            x = x,
                            y = y,
                            cellWidthPx = cellWidthPx,
                            cellHeightPx = cellHeightPx,
                            isPastDate = isPastDate,
                            isStart = isStart,
                            isEnd = isEnd,
                            isWeekend = isWeekend,
                            currentEpoch = currentEpoch,
                            weekendBackgroundColor = colors.weekendContainerColor,
                            selectedRangeBackgroundColor = colors.selectedRangeContainerColor,
                            selectedDateBackgroundColor = colors.selectedDateContainerColor,
                            cornerRadius = cornerRadius,
                            barHeightPx = barHeightPx,
                            state = state,
                            barPaddingPx = barPaddingPx,
                            barSpacingPx = barSpacingPx,
                            barWidthPx = barWidthPx,
                            disabledDateBackgroundColor = colors.disabledContainerColor,
                        )

                        // --- Draw Borders ---
                        drawCellBorder(
                            x = x,
                            y = y,
                            cellWidthPx = cellWidthPx,
                            cellHeightPx = cellHeightPx,
                            borderColor = colors.cellBorderColor,
                            strokeWidthPx = strokeWidthPx,
                        )

                        // --- Draw Tags ---
                        drawDayTags(
                            x = x,
                            y = y,
                            cellWidthPx = cellWidthPx,
                            cellHeightPx = cellHeightPx,
                            tagLayouts = tagLayouts,
                            currentEpoch = currentEpoch
                        )

                        // --- Draw Text (From Cache) ---
                        drawDayText(
                            x = x,
                            y = y,
                            cellWidthPx = cellWidthPx,
                            cellHeightPx = cellHeightPx,
                            dayNumberLayouts = dayNumberLayouts,
                            isPastDate = isPastDate,
                            isStart = isStart,
                            isEnd = isEnd,
                            isWeekend = isWeekend,
                            day = day
                        )

                    }
                }
            }
            // --- 4. TOUCH INPUT LOGIC ---
            // Attached only if not past month to save processing
            .monthGestures(
                monthState = monthState,
                cellHeightPx = cellHeightPx,
                state = state
            )
    )
}

@Composable
internal fun Modifier.monthGestures(
    monthState: MonthMetadata,
    cellHeightPx: Float,
    state: DraggableDateRangePickerState
): Modifier {


    // 2. Key only on the Month (Stable). Do NOT key on selection.
    return this.pointerInput(monthState.yearMonth) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)

            val day = getDayFromOffset(
                down.position,
                size.width.toFloat(),
                cellHeightPx,
                monthState.firstDayOfWeek
            )

            // --- HIT TEST ---
            var isHandleTouch = false
            var isDraggingStart = false
            var anchorEpoch: Long? = null

            if (day in 1..monthState.daysInMonth) {
                val touchedEpoch = monthState.firstDayEpoch + (day - 1)

                // Check handles using the CAPTURED state
                if (state.selectedStartDateMillis != null && (touchedEpoch == state.selectedStartDateMillis || touchedEpoch == state.selectedEndDateMillis)) {
                    isHandleTouch = true
                    isDraggingStart = (touchedEpoch == state.selectedStartDateMillis)

                    // Anchor logic: If dragging start, anchor is end.
                    // If dragging end, anchor is start.
                    // If end is null (single selection), anchor is start.
                    anchorEpoch = if (isDraggingStart) state.selectedEndDateMillis
                        ?: state.selectedStartDateMillis else state.selectedStartDateMillis

                    // CRITICAL: Consume down to prevent parent scrolling
                    down.consume()
                }
            }

            // --- DECISION: DRAG vs TAP ---

            if (isHandleTouch && anchorEpoch != null) {
                // CASE A: HANDLE DRAG
                // We stay in this loop until the user lifts their finger.
                // Because we used rememberUpdatedState, this loop survives
                // recompositions caused by currentUpdateDragSelection calls.
                drag(down.id) { change ->
                    val dragDay = getDayFromOffset(
                        change.position,
                        size.width.toFloat(),
                        cellHeightPx,
                        monthState.firstDayOfWeek
                    )

                    if (dragDay in 1..monthState.daysInMonth) {
                        val currentDragEpoch = monthState.firstDayEpoch + (dragDay - 1)

                        // Prevent dragging into the past
                        if (currentDragEpoch >= monthState.nowEpoch) {
                            change.consume() // Consume drag event

                            if (isDraggingStart) {
                                val newStart = currentDragEpoch.coerceAtMost(anchorEpoch)
                                state.updateDragSelection(newStart, anchorEpoch)
                            } else {
                                val newEnd = currentDragEpoch.coerceAtLeast(anchorEpoch)
                                state.updateDragSelection(anchorEpoch, newEnd)
                            }
                        }
                    }
                }
            } else {
                // CASE B: TAP DETECTION
                // If it wasn't a handle, wait to see if it's a valid tap.
                // We assume it's a tap if they lift the finger without moving out of bounds.
                // NOTE: We did NOT consume 'down', so parent can still scroll.

                val up = waitForUpOrCancellation()
                if (up != null) {
                    // It was a tap!
                    val upDay = getDayFromOffset(
                        up.position,
                        size.width.toFloat(),
                        cellHeightPx,
                        monthState.firstDayOfWeek
                    )

                    if (upDay in 1..monthState.daysInMonth && upDay == day) {
                        val epoch = monthState.firstDayEpoch + (upDay - 1)
                        if (epoch >= monthState.nowEpoch) {
                            state.onDateSelected(epoch)
                            up.consume()
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawCellBackground(
    x: Float,
    y: Float,
    cellHeightPx: Float,
    cellWidthPx: Float,
    isPastDate: Boolean,
    isStart: Boolean,
    isEnd: Boolean,
    isWeekend: Boolean,
    currentEpoch: Long,
    weekendBackgroundColor: Color,
    selectedRangeBackgroundColor: Color,
    selectedDateBackgroundColor: Color,
    disabledDateBackgroundColor: Color,
    cornerRadius: CornerRadius,
    barHeightPx: Float,
    state: DraggableDateRangePickerState,
    barPaddingPx: Float,
    barSpacingPx: Float,
    barWidthPx: Float,
) {
    // Manual range check is faster than DateUtils calls inside loop
    val isMiddleRange = !isPastDate && !isStart && !isEnd &&
            (state.selectedStartDateMillis != null && state.selectedEndDateMillis != null &&
                    currentEpoch > state.selectedStartDateMillis!! && currentEpoch < state.selectedEndDateMillis!!)

    if (isWeekend) {
        drawRect(
            color = weekendBackgroundColor,
            topLeft = Offset(x, y),
            size = Size(cellWidthPx, cellHeightPx)
        )
    }

    if (isMiddleRange) {
        drawRect(
            color = selectedRangeBackgroundColor,
            topLeft = Offset(x, y),
            size = Size(cellWidthPx, cellHeightPx),
        )
    } else if (isStart || isEnd) {
        drawRoundRect(
            color = selectedDateBackgroundColor,
            topLeft = Offset(x, y),
            size = Size(cellWidthPx, cellHeightPx),
            cornerRadius = cornerRadius
        )

        // Draw Vertical Bars (Selection Handles)
        val barCenterY = y + (cellHeightPx / 2) - (barHeightPx / 2)
        val barColor = Color.White

        if (isStart && state.selectedEndDateMillis != null && state.selectedStartDateMillis != state.selectedEndDateMillis) {
            val barStartX = x + barPaddingPx
            drawLine(
                barColor,
                Offset(barStartX, barCenterY),
                Offset(barStartX, barCenterY + barHeightPx),
                barWidthPx,
                StrokeCap.Round
            )
            drawLine(
                barColor,
                Offset(barStartX + barSpacingPx, barCenterY),
                Offset(barStartX + barSpacingPx, barCenterY + barHeightPx),
                barWidthPx,
                StrokeCap.Round
            )
        }

        if (isEnd && state.selectedStartDateMillis != state.selectedEndDateMillis) {
            val barStartX =
                x + cellWidthPx - barPaddingPx - barSpacingPx - barWidthPx
            drawLine(
                barColor,
                Offset(barStartX, barCenterY),
                Offset(barStartX, barCenterY + barHeightPx),
                barWidthPx,
                StrokeCap.Round
            )
            drawLine(
                barColor,
                Offset(barStartX + barSpacingPx, barCenterY),
                Offset(barStartX + barSpacingPx, barCenterY + barHeightPx),
                barWidthPx,
                StrokeCap.Round
            )
        }
    }
}

private fun DrawScope.drawCellBorder(
    borderColor: Color,
    x: Float,
    y: Float,
    cellHeightPx: Float,
    cellWidthPx: Float,
    strokeWidthPx: Float,
) {
    drawRect(
        color = borderColor,
        topLeft = Offset(x, y),
        size = Size(cellWidthPx, cellHeightPx),
        style = Stroke(width = strokeWidthPx)
    )
}

private fun DrawScope.drawDayTags(
    x: Float,
    y: Float,
    cellHeightPx: Float,
    cellWidthPx: Float,
    tagLayouts: Map<Long, List<MeasuredTag>>?,
    currentEpoch: Long
) {
    tagLayouts?.get(currentEpoch)?.forEach { (measuredTag, alignment) ->
        val tW = measuredTag.size.width
        val tH = measuredTag.size.height
        val tagPadding = 4.dp.toPx() // Keep simple padding logic

        val tagX = when (alignment) {
            TagAlignment.TOP_START, TagAlignment.CENTER_START, TagAlignment.BOTTOM_START -> x + tagPadding
            TagAlignment.TOP_CENTER, TagAlignment.CENTER, TagAlignment.BOTTOM_CENTER -> x + (cellWidthPx - tW) / 2
            else -> x + cellWidthPx - tW - tagPadding
        }

        val tagY = when (alignment) {
            TagAlignment.TOP_START, TagAlignment.TOP_CENTER, TagAlignment.TOP_END -> y + tagPadding
            TagAlignment.CENTER_START, TagAlignment.CENTER, TagAlignment.CENTER_END -> y + (cellHeightPx - tH) / 2
            else -> y + cellHeightPx - tH - tagPadding
        }

        drawText(measuredTag, topLeft = Offset(tagX, tagY))
    }
}

private fun DrawScope.drawDayText(
    x: Float,
    y: Float,
    cellHeightPx: Float,
    cellWidthPx: Float,
    dayNumberLayouts: Map<Int, DayTextCache>,
    day: Int,
    isPastDate: Boolean,
    isStart: Boolean,
    isEnd: Boolean,
    isWeekend: Boolean
) {
    val cachedDay = dayNumberLayouts[day]!!
    val textLayout = when {
        isPastDate -> cachedDay.disabled
        isStart || isEnd -> cachedDay.selected
        isWeekend -> cachedDay.weekend
        else -> cachedDay.standard
    }

    drawText(
        textLayoutResult = textLayout,
        topLeft = Offset(
            x + (cellWidthPx - textLayout.size.width) / 2,
            y + (cellHeightPx - textLayout.size.height) / 2
        )
    )
}

// --- Helper Classes ---


// Pure Math Helper
private fun getDayFromOffset(
    offset: Offset,
    viewWidth: Float,
    cellHeight: Float,
    firstDayOffset: Int
): Int {
    val col = (offset.x / (viewWidth / 7)).toInt()
    val row = (offset.y / cellHeight).toInt()
    Log.e("Rahul", "getDayFromOffset firstDayOfWeek $firstDayOffset , row $row , col $col")
    val gridIndex = row * 7 + col
    return gridIndex - firstDayOffset + 1
}