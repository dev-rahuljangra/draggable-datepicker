package com.devrj.draggabledatepicker.ui

import android.text.format.DateFormat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devrj.draggabledatepicker.datePickerState.CalendarScroller
import com.devrj.draggabledatepicker.datePickerState.DraggableDateRangePickerState
import com.devrj.draggabledatepicker.datePickerState.DraggableDateRangePickerStateImpl
import com.devrj.draggabledatepicker.datePickerState.ListCalendarScroller
import com.devrj.draggabledatepicker.datePickerState.PagerCalendarScroller
import com.devrj.draggabledatepicker.enums.CalendarOrientation
import com.devrj.draggabledatepicker.enums.DateSelectionMode
import com.devrj.draggabledatepicker.model.CalendarColors
import com.devrj.draggabledatepicker.model.CalendarTextStyles
import com.devrj.draggabledatepicker.model.DayCell
import com.devrj.draggabledatepicker.model.DayTextCache
import com.devrj.draggabledatepicker.model.MonthMetadata
import com.devrj.draggabledatepicker.selection.SelectionRules
import com.devrj.draggabledatepicker.utils.CalendarDefaults
import com.devrj.draggabledatepicker.utils.Constant
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.WeekFields
import java.util.Locale
import java.time.format.TextStyle as JavaTextStyle

private const val MONTHS_PER_YEAR = 12
private val HorizontalPadding = 16.dp

/**
 * A date picker for Jetpack Compose. Users tap to choose dates and drag either end of a range,
 * including across months.
 *
 * @param state Holds the selection and tags. See [DraggableDateRangePickerState].
 * @param startYear The first year shown. Set this as well as [minDate] to show past dates.
 * @param endYear The last year shown.
 * @param cellSize The height of each day cell.
 * @param minDate The earliest selectable date, or null for no limit. Defaults to today.
 * @param maxDate The latest selectable date, or null for no limit.
 * @param isDateSelectable Blocks individual dates. It runs for every visible day, so keep it fast.
 * @param allowUnselectableDatesInRange Whether a range can span blocked dates. When false, a drag
 * stops at the last date before a blocked one.
 * @param selectionMode Whether the user picks a range or a single date.
 * @param minRangeDays The shortest allowed range in days, counting both ends.
 * @param maxRangeDays The longest allowed range in days, counting both ends, or null for no limit.
 * @param onRangeChange Called when the selection changes, including during a drag, but not for
 * the initial selection. Saved selections are restored without calling it.
 * @param orientation Whether months scroll vertically or page horizontally.
 * @param stickyWeekdayHeader Whether one weekday row stays above the months. When false, each month
 * shows its own.
 * @param hapticsEnabled Whether dragging a range gives haptic feedback for each day it crosses.
 * @param locale The locale for month names, weekday names and numbers.
 * @param firstDayOfWeek The first column of the grid. Defaults to the first day for [locale].
 * @param initialDisplayedMonth The month to open on. Defaults to the selected month, or else the
 * current month.
 * @param dayContent Replaces the drawn day number and tags in each cell. The selection background
 * is still drawn behind it. Don't consume touches here, or selection stops working for that cell.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DraggableDateRangePicker(
    modifier: Modifier = Modifier,
    state: DraggableDateRangePickerState,
    colors: CalendarColors = CalendarDefaults.colors(),
    calendarTextStyles: CalendarTextStyles = CalendarDefaults.textStyles(),
    weekendDays: List<DayOfWeek> = listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
    startYear: Int = CalendarDefaults.today().year,
    endYear: Int = startYear + 10,
    cellSize: Dp = Constant.CELL_SIZE,
    minDate: LocalDate? = CalendarDefaults.today(),
    maxDate: LocalDate? = null,
    isDateSelectable: (LocalDate) -> Boolean = { true },
    allowUnselectableDatesInRange: Boolean = false,
    selectionMode: DateSelectionMode = DateSelectionMode.RANGE,
    minRangeDays: Int = 1,
    maxRangeDays: Int? = null,
    onRangeChange: ((start: LocalDate?, end: LocalDate?) -> Unit)? = null,
    orientation: CalendarOrientation = CalendarOrientation.VERTICAL,
    stickyWeekdayHeader: Boolean = true,
    hapticsEnabled: Boolean = true,
    locale: Locale = CalendarDefaults.locale(),
    firstDayOfWeek: DayOfWeek = WeekFields.of(locale).firstDayOfWeek,
    initialDisplayedMonth: YearMonth? = null,
    dayContent: (@Composable (DayCell) -> Unit)? = null,
) {
    require(endYear >= startYear) { "endYear must not be before startYear" }
    require(minRangeDays >= 1) { "minRangeDays must be at least 1" }
    require(maxRangeDays == null || maxRangeDays >= minRangeDays) {
        "maxRangeDays must not be less than minRangeDays"
    }

    val today = CalendarDefaults.today()
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val styles = remember(colors, calendarTextStyles) { calendarTextStyles.resolve(colors) }

    val rules = remember(
        selectionMode, minDate, maxDate, isDateSelectable,
        allowUnselectableDatesInRange, minRangeDays, maxRangeDays,
    ) {
        SelectionRules(
            mode = selectionMode,
            minEpoch = minDate?.toEpochDay(),
            maxEpoch = maxDate?.toEpochDay(),
            isDateSelectable = isDateSelectable,
            allowUnselectableDatesInRange = allowUnselectableDatesInRange,
            minRangeDays = minRangeDays,
            maxRangeDays = maxRangeDays,
        )
    }
    val currentRules = rememberUpdatedState(rules)

    // --- Text, shared by every month ---
    val dayLabels = remember(locale) {
        val format = NumberFormat.getIntegerInstance(locale).apply { isGroupingUsed = false }
        (1..31).map { format.format(it.toLong()) }
    }
    val textMeasurer = rememberTextMeasurer(cacheSize = 0)
    val dayTextCache = remember(textMeasurer, styles, dayLabels) {
        DayTextCache(
            textMeasurer = textMeasurer,
            dayLabels = dayLabels,
            standard = styles.dayTextStyle,
            weekend = styles.weekendTextStyle,
            selected = styles.selectedDayTextStyle,
            inRange = styles.inRangeDayTextStyle,
            disabled = styles.disabledDayTextStyle,
        )
    }
    val monthTitleFormatter = remember(locale) {
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "MMMMy"), locale)
    }
    val dateFormatter = remember(locale) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)
    }
    val weekdayLabels = remember(locale, firstDayOfWeek) {
        (0L until 7L).map { firstDayOfWeek.plus(it).getDisplayName(JavaTextStyle.SHORT, locale) }
    }

    // --- Months and scrolling ---
    val firstMonth = remember(startYear) { YearMonth.of(startYear, 1) }
    val monthCount = (endYear - startYear + 1) * MONTHS_PER_YEAR
    val initialIndex = remember {
        val selected = state.selectedStartEpochDay?.let(LocalDate::ofEpochDay)
        val opening = initialDisplayedMonth
            ?: selected?.let(YearMonth::from)
            ?: YearMonth.from(today)
        val offset = (opening.year - startYear) * MONTHS_PER_YEAR + (opening.monthValue - 1)
        offset.coerceIn(0, monthCount - 1)
    }
    val monthAt = remember(firstMonth, firstDayOfWeek) {
        { index: Int ->
            MonthMetadata.of(firstMonth.plusMonths(index.toLong()), firstDayOfWeek)
        }
    }
    val monthKey = remember(startYear) {
        { index: Int -> startYear * MONTHS_PER_YEAR + index }
    }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val pagerState = rememberPagerState(initialPage = initialIndex) { monthCount }
    val scroller: CalendarScroller = remember(orientation, firstMonth, monthCount) {
        when (orientation) {
            CalendarOrientation.VERTICAL -> ListCalendarScroller(listState, firstMonth, monthCount)
            CalendarOrientation.HORIZONTAL -> PagerCalendarScroller(pagerState, firstMonth, monthCount)
        }
    }

    val stateImpl = state as? DraggableDateRangePickerStateImpl
    if (stateImpl != null) {
        DisposableEffect(stateImpl, scroller) {
            stateImpl.attachScroller(scroller)
            onDispose { stateImpl.detachScroller(scroller) }
        }
        LaunchedEffect(stateImpl, scroller) {
            stateImpl.consumePendingMonth()?.let { scroller.scrollTo(it, animated = false) }
        }
    }

    // --- Callbacks ---
    val currentOnRangeChange = rememberUpdatedState(onRangeChange)
    LaunchedEffect(state) {
        snapshotFlow { state.selectedStartEpochDay to state.selectedEndEpochDay }
            .distinctUntilChanged()
            .drop(1)
            .collect { (start, end) ->
                currentOnRangeChange.value?.invoke(
                    start?.let(LocalDate::ofEpochDay),
                    end?.let(LocalDate::ofEpochDay),
                )
            }
    }

    val haptics = LocalHapticFeedback.current
    val currentHapticsEnabled = rememberUpdatedState(hapticsEnabled)
    val registry = remember { MonthGridRegistry() }

    val weekdayHeader: @Composable () -> Unit = {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = HorizontalPadding),
        ) {
            weekdayLabels.forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = styles.dayTextStyle.copy(fontSize = 14.sp),
                )
            }
        }
    }

    val monthContent: @Composable (Int) -> Unit = { index ->
        val month = remember(index, monthAt) { monthAt(index) }
        Column(Modifier.fillMaxWidth()) {
            Text(
                text = monthTitleFormatter.format(month.yearMonth),
                style = styles.monthTextStyle.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier
                    .padding(horizontal = HorizontalPadding, vertical = 8.dp)
                    .semantics { heading() },
            )
            if (!stickyWeekdayHeader) weekdayHeader()
            MonthContainer(
                state = state,
                month = month,
                cellSize = cellSize,
                colors = colors,
                calendarStyles = styles,
                textMeasurer = textMeasurer,
                dayTextCache = dayTextCache,
                dayLabels = dayLabels,
                rules = rules,
                weekendDays = weekendDays,
                today = today,
                isRtl = isRtl,
                dateFormatter = dateFormatter,
                registry = registry,
                dayContent = dayContent,
                onDateTapped = { epoch ->
                    val next = currentRules.value.resolveTap(state.selectionValue(), epoch)
                    state.setSelection(next.start, next.end)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = HorizontalPadding),
            )
        }
    }

    Column(modifier.background(colors.containerColor)) {
        if (stickyWeekdayHeader) weekdayHeader()
        Box(
            Modifier
                .weight(1f)
                .onPlaced { registry.containerCoordinates = it }
                .calendarGestures(
                    state = state,
                    rules = currentRules,
                    registry = registry,
                    scroller = scroller,
                    isRtl = isRtl,
                    onDragStep = {
                        if (currentHapticsEnabled.value) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    },
                ),
        ) {
            when (orientation) {
                CalendarOrientation.VERTICAL -> LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(
                        count = monthCount,
                        key = monthKey,
                        contentType = { "month" },
                    ) { index -> monthContent(index) }
                }

                CalendarOrientation.HORIZONTAL -> HorizontalPager(
                    state = pagerState,
                    key = monthKey,
                ) { index -> monthContent(index) }
            }
        }
    }
}
