package com.devrj.draggabledatepicker.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.devrj.draggabledatepicker.R
import com.devrj.draggabledatepicker.datePickerState.DraggableDateRangePickerState
import com.devrj.draggabledatepicker.enums.DateSelectionMode
import com.devrj.draggabledatepicker.enums.TagAlignment
import com.devrj.draggabledatepicker.enums.TagStyle
import com.devrj.draggabledatepicker.enums.TagType
import com.devrj.draggabledatepicker.model.CalendarColors
import com.devrj.draggabledatepicker.model.CalendarTag
import com.devrj.draggabledatepicker.model.CalendarTextStyles
import com.devrj.draggabledatepicker.model.DayCell
import com.devrj.draggabledatepicker.model.DayTextCache
import com.devrj.draggabledatepicker.model.DayTextVariant
import com.devrj.draggabledatepicker.model.MeasuredTag
import com.devrj.draggabledatepicker.model.MonthMetadata
import com.devrj.draggabledatepicker.selection.DAYS_PER_WEEK
import com.devrj.draggabledatepicker.selection.SelectionRules
import com.devrj.draggabledatepicker.selection.SelectionValue
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.roundToInt

private val TagPadding = 4.dp
private val PillHorizontalPadding = 3.dp
private val PillVerticalPadding = 1.dp
private val HandleWidth = 2.dp
private val HandleHeight = 12.dp
private val HandleSpacing = 4.dp
private val HandlePadding = 6.dp
private val SelectedCornerRadius = 10.dp
private val BorderWidth = 1.dp

private enum class CellSelection { NONE, START, END, BOTH, MIDDLE }

private fun selectionOf(state: DraggableDateRangePickerState, epoch: Long): CellSelection {
    val start = state.selectedStartEpochDay
    val end = state.selectedEndEpochDay
    return when {
        start == epoch && end == epoch -> CellSelection.BOTH
        start == epoch -> CellSelection.START
        end == epoch -> CellSelection.END
        start != null && end != null && epoch > start && epoch < end -> CellSelection.MIDDLE
        else -> CellSelection.NONE
    }
}

/**
 * One month's grid. The canvas draws the cells, so a drag only redraws instead of recomposing.
 * Each day also has a lightweight child node that carries its accessibility information and the
 * optional custom content.
 */
@Composable
internal fun MonthContainer(
    state: DraggableDateRangePickerState,
    month: MonthMetadata,
    cellSize: Dp,
    colors: CalendarColors,
    calendarStyles: CalendarTextStyles,
    textMeasurer: TextMeasurer,
    dayTextCache: DayTextCache,
    dayLabels: List<String>,
    rules: SelectionRules,
    weekendDays: List<DayOfWeek>,
    today: LocalDate,
    isRtl: Boolean,
    dateFormatter: DateTimeFormatter,
    registry: MonthGridRegistry,
    dayContent: (@Composable (DayCell) -> Unit)?,
    onDateTapped: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabledDays = remember(month, rules) {
        BooleanArray(month.daysInMonth) { rules.isEnabled(month.firstDayEpoch + it) }
    }
    val weekendFlags = remember(month, weekendDays) {
        val firstDay = month.yearMonth.atDay(1).dayOfWeek
        BooleanArray(month.daysInMonth) { firstDay.plus(it.toLong()) in weekendDays }
    }
    val showHandles = rules.mode == DateSelectionMode.RANGE
    val drawsDayContent = dayContent == null

    DisposableEffect(registry, month) {
        onDispose { registry.remove(month) }
    }

    Layout(
        modifier = modifier
            .onPlaced { registry.update(month, it) }
            .drawWithCache {
                val cellWidth = size.width / DAYS_PER_WEEK
                val cellHeight = size.height / month.rows
                val tags = if (drawsDayContent) {
                    measureMonthTags(
                        allTags = state.getDateTags(),
                        month = month,
                        cellWidthPx = cellWidth,
                        colors = colors,
                        styles = calendarStyles,
                        textMeasurer = textMeasurer,
                        density = this,
                    )
                } else {
                    emptyMap()
                }
                val border = Stroke(width = BorderWidth.toPx())
                val cornerRadius = CornerRadius(SelectedCornerRadius.toPx())

                onDrawBehind {
                    for (day in 1..month.daysInMonth) {
                        val gridIndex = day + month.leadingBlanks - 1
                        val column = gridIndex % DAYS_PER_WEEK
                        val visualColumn = if (isRtl) DAYS_PER_WEEK - 1 - column else column
                        val x = visualColumn * cellWidth
                        val y = (gridIndex / DAYS_PER_WEEK) * cellHeight
                        val epoch = month.firstDayEpoch + (day - 1)
                        val selection = selectionOf(state, epoch)
                        val isEnabled = enabledDays[day - 1]
                        val isWeekend = weekendFlags[day - 1]
                        val cellSizePx = Size(cellWidth, cellHeight)

                        if (!isEnabled) {
                            drawRect(colors.disabledContainerColor, Offset(x, y), cellSizePx)
                        } else if (isWeekend) {
                            drawRect(colors.weekendContainerColor, Offset(x, y), cellSizePx)
                        }

                        if (selection == CellSelection.MIDDLE) {
                            drawRect(colors.selectedRangeContainerColor, Offset(x, y), cellSizePx)
                        } else if (selection != CellSelection.NONE) {
                            drawRoundRect(
                                color = colors.selectedDateContainerColor,
                                topLeft = Offset(x, y),
                                size = cellSizePx,
                                cornerRadius = cornerRadius,
                            )
                            if (showHandles) {
                                drawRangeHandle(
                                    selection = selection,
                                    x = x,
                                    y = y,
                                    cellWidth = cellWidth,
                                    cellHeight = cellHeight,
                                    color = colors.selectedDateContentColor,
                                    isRtl = isRtl,
                                )
                            }
                        }

                        drawRect(
                            color = colors.cellBorderColor,
                            topLeft = Offset(x, y),
                            size = cellSizePx,
                            style = border,
                        )

                        if (drawsDayContent) {
                            tags[epoch]?.forEach { tag ->
                                drawTag(tag, x, y, cellWidth, cellHeight, isRtl)
                            }
                            val variant = when {
                                selection == CellSelection.MIDDLE -> DayTextVariant.IN_RANGE
                                selection != CellSelection.NONE -> DayTextVariant.SELECTED
                                !isEnabled -> DayTextVariant.DISABLED
                                isWeekend -> DayTextVariant.WEEKEND
                                else -> DayTextVariant.STANDARD
                            }
                            val text = dayTextCache.get(day, variant)
                            drawText(
                                textLayoutResult = text,
                                topLeft = Offset(
                                    x + (cellWidth - text.size.width) / 2,
                                    y + (cellHeight - text.size.height) / 2,
                                ),
                            )
                        }
                    }
                }
            },
        content = {
            for (day in 1..month.daysInMonth) {
                key(day) {
                    DayNode(
                        state = state,
                        date = month.yearMonth.atDay(day),
                        dayText = dayLabels[day - 1],
                        isEnabled = enabledDays[day - 1],
                        isWeekend = weekendFlags[day - 1],
                        today = today,
                        dateFormatter = dateFormatter,
                        dayContent = dayContent,
                        onDateTapped = onDateTapped,
                    )
                }
            }
        },
    ) { measurables, constraints ->
        val cellHeight = cellSize.roundToPx()
        val width = if (constraints.hasBoundedWidth) {
            constraints.maxWidth
        } else {
            cellHeight * DAYS_PER_WEEK
        }
        val cellWidth = width / DAYS_PER_WEEK.toFloat()
        val childConstraints = Constraints.fixed(ceil(cellWidth).toInt(), cellHeight)
        val placeables = measurables.map { it.measure(childConstraints) }

        layout(width, cellHeight * month.rows) {
            placeables.forEachIndexed { index, placeable ->
                val gridIndex = index + month.leadingBlanks
                // placeRelative mirrors the position in right-to-left layouts.
                placeable.placeRelative(
                    x = ((gridIndex % DAYS_PER_WEEK) * cellWidth).roundToInt(),
                    y = (gridIndex / DAYS_PER_WEEK) * cellHeight,
                )
            }
        }
    }
}

/**
 * The child node for one day. Deriving its selection means a drag only recomposes the days whose
 * status actually changed.
 */
@Composable
private fun DayNode(
    state: DraggableDateRangePickerState,
    date: LocalDate,
    dayText: String,
    isEnabled: Boolean,
    isWeekend: Boolean,
    today: LocalDate,
    dateFormatter: DateTimeFormatter,
    dayContent: (@Composable (DayCell) -> Unit)?,
    onDateTapped: (Long) -> Unit,
) {
    val epoch = date.toEpochDay()
    val selection by remember(state, epoch) {
        derivedStateOf { selectionOf(state, epoch) }
    }
    val tags by remember(state, epoch) {
        derivedStateOf { state.getDateTags()[epoch].orEmpty() }
    }
    val selectionDescription = when (selection) {
        CellSelection.NONE -> null
        CellSelection.START -> stringResource(R.string.ddp_state_range_start)
        CellSelection.END -> stringResource(R.string.ddp_state_range_end)
        CellSelection.BOTH -> stringResource(R.string.ddp_state_selected)
        CellSelection.MIDDLE -> stringResource(R.string.ddp_state_range_middle)
    }
    val unavailable = if (isEnabled) null else stringResource(R.string.ddp_state_unavailable)
    val selectLabel = stringResource(R.string.ddp_action_select)

    Box(
        modifier = Modifier.clearAndSetSemantics {
            contentDescription = buildString {
                append(dateFormatter.format(date))
                tags.forEach { append(", ").append(it.text) }
            }
            selected = selection != CellSelection.NONE
            listOfNotNull(selectionDescription, unavailable)
                .takeIf { it.isNotEmpty() }
                ?.let { stateDescription = it.joinToString(", ") }
            if (isEnabled) {
                onClick(label = selectLabel) {
                    onDateTapped(epoch)
                    true
                }
            } else {
                disabled()
            }
        },
    ) {
        if (dayContent != null) {
            val current = selection
            dayContent(
                DayCell(
                    date = date,
                    dayText = dayText,
                    isToday = date == today,
                    isWeekend = isWeekend,
                    isEnabled = isEnabled,
                    isRangeStart = current == CellSelection.START || current == CellSelection.BOTH,
                    isRangeEnd = current == CellSelection.END || current == CellSelection.BOTH,
                    isInRange = current == CellSelection.MIDDLE,
                    tags = tags,
                ),
            )
        }
    }
}

/** Measures only this month's tags, constrained to the cell so long labels ellipsize. */
private fun measureMonthTags(
    allTags: Map<Long, List<CalendarTag>>,
    month: MonthMetadata,
    cellWidthPx: Float,
    colors: CalendarColors,
    styles: CalendarTextStyles,
    textMeasurer: TextMeasurer,
    density: Density,
): Map<Long, List<MeasuredTag>> {
    if (allTags.isEmpty()) return emptyMap()
    val inset = with(density) { (TagPadding + PillHorizontalPadding).toPx() } * 2
    val maxWidth = (cellWidthPx - inset).toInt().coerceAtLeast(0)
    val result = HashMap<Long, List<MeasuredTag>>()
    for (offset in 0 until month.daysInMonth) {
        val epoch = month.firstDayEpoch + offset
        val dayTags = allTags[epoch] ?: continue
        result[epoch] = dayTags.map { tag ->
            val hasBackground = tag.style == TagStyle.PILL || tag.backgroundColor != null
            MeasuredTag(
                textLayoutResult = textMeasurer.measure(
                    text = AnnotatedString(tag.text),
                    style = styles.tagTextStyle.merge(
                        color = tag.color ?: Color.Unspecified,
                        fontSize = tag.fontSize,
                        fontWeight = if (tag.type == TagType.PRICE) FontWeight.SemiBold else null,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    constraints = Constraints(maxWidth = maxWidth),
                ),
                alignment = tag.alignment,
                backgroundColor = if (hasBackground) {
                    tag.backgroundColor ?: colors.tagContainerColor
                } else {
                    null
                },
            )
        }
    }
    return result
}

private fun DrawScope.drawRangeHandle(
    selection: CellSelection,
    x: Float,
    y: Float,
    cellWidth: Float,
    cellHeight: Float,
    color: Color,
    isRtl: Boolean,
) {
    if (selection != CellSelection.START && selection != CellSelection.END) return
    val barWidth = HandleWidth.toPx()
    val barHeight = HandleHeight.toPx()
    val spacing = HandleSpacing.toPx()
    val padding = HandlePadding.toPx()
    val top = y + (cellHeight - barHeight) / 2
    // The handle sits on the range's outer edge, which is the left of the start date in LTR.
    val onLeft = (selection == CellSelection.START) != isRtl
    val firstBar = if (onLeft) x + padding else x + cellWidth - padding - spacing - barWidth
    for (barX in listOf(firstBar, firstBar + spacing)) {
        drawLine(color, Offset(barX, top), Offset(barX, top + barHeight), barWidth, StrokeCap.Round)
    }
}

private fun DrawScope.drawTag(
    tag: MeasuredTag,
    x: Float,
    y: Float,
    cellWidth: Float,
    cellHeight: Float,
    isRtl: Boolean,
) {
    val layout = tag.textLayoutResult
    val width = layout.size.width
    val height = layout.size.height
    val padding = TagPadding.toPx()
    val pillX = PillHorizontalPadding.toPx()
    val pillY = PillVerticalPadding.toPx()

    val horizontal = when (tag.alignment) {
        TagAlignment.TOP_START, TagAlignment.CENTER_START, TagAlignment.BOTTOM_START -> -1
        TagAlignment.TOP_END, TagAlignment.CENTER_END, TagAlignment.BOTTOM_END -> 1
        else -> 0
    }.let { if (isRtl) -it else it }
    val tagX = when (horizontal) {
        -1 -> x + padding + pillX
        1 -> x + cellWidth - width - padding - pillX
        else -> x + (cellWidth - width) / 2
    }
    val tagY = when (tag.alignment) {
        TagAlignment.TOP_START, TagAlignment.TOP_CENTER, TagAlignment.TOP_END -> y + padding
        TagAlignment.CENTER_START, TagAlignment.CENTER, TagAlignment.CENTER_END ->
            y + (cellHeight - height) / 2
        else -> y + cellHeight - height - padding
    }

    tag.backgroundColor?.let { background ->
        drawRoundRect(
            color = background,
            topLeft = Offset(tagX - pillX, tagY - pillY),
            size = Size(width + pillX * 2, height + pillY * 2),
            cornerRadius = CornerRadius((height + pillY * 2) / 2),
        )
    }
    drawText(layout, topLeft = Offset(tagX, tagY))
}

/** Returns the selection as a value the rules can work with. */
internal fun DraggableDateRangePickerState.selectionValue(): SelectionValue =
    SelectionValue(selectedStartEpochDay, selectedEndEpochDay)
