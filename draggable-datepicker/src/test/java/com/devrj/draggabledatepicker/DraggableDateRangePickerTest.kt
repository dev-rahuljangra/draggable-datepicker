package com.devrj.draggabledatepicker

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.devrj.draggabledatepicker.datePickerState.DraggableDateRangePickerState
import com.devrj.draggabledatepicker.datePickerState.rememberDraggableDateRangePickerState
import com.devrj.draggabledatepicker.datePickerState.selectedEndDate
import com.devrj.draggabledatepicker.datePickerState.selectedStartDate
import com.devrj.draggabledatepicker.enums.CalendarOrientation
import com.devrj.draggabledatepicker.enums.DateSelectionMode
import com.devrj.draggabledatepicker.model.CalendarTag
import com.devrj.draggabledatepicker.ui.DraggableDateRangePicker
import com.devrj.draggabledatepicker.ui.DraggableDateRangePickerDialog
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h800dp")
class DraggableDateRangePickerTest {
    @get:Rule
    val rule = createComposeRule()

    private lateinit var state: DraggableDateRangePickerState
    private val formatter =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(Locale.US)

    private fun date(month: Int, day: Int) = LocalDate.of(2030, month, day)

    private fun setPicker(
        initial: LocalDate? = null,
        opening: YearMonth = YearMonth.of(2030, 1),
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        minDate: LocalDate? = null,
        maxDate: LocalDate? = null,
        isDateSelectable: (LocalDate) -> Boolean = { true },
        selectionMode: DateSelectionMode = DateSelectionMode.RANGE,
        orientation: CalendarOrientation = CalendarOrientation.VERTICAL,
        locale: Locale = Locale.US,
        onRangeChange: ((LocalDate?, LocalDate?) -> Unit)? = null,
    ) {
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                state = rememberDraggableDateRangePickerState(
                    initialStartEpochDay = initial?.toEpochDay(),
                )
                DraggableDateRangePicker(
                    modifier = Modifier.fillMaxSize(),
                    state = state,
                    startYear = 2030,
                    endYear = 2031,
                    cellSize = 40.dp,
                    minDate = minDate,
                    maxDate = maxDate,
                    isDateSelectable = isDateSelectable,
                    selectionMode = selectionMode,
                    orientation = orientation,
                    locale = locale,
                    firstDayOfWeek = DayOfWeek.MONDAY,
                    initialDisplayedMonth = opening,
                    onRangeChange = onRangeChange,
                )
            }
        }
        rule.waitForIdle()
    }

    private fun cell(date: LocalDate): SemanticsNodeInteraction =
        rule.onNode(hasContentDescription(formatter.format(date), substring = true))

    private fun SemanticsNodeInteraction.center(): Offset =
        fetchSemanticsNode().boundsInRoot.center

    private fun tap(date: LocalDate) {
        val position = cell(date).center()
        rule.onRoot().performTouchInput { click(position) }
        rule.waitForIdle()
    }

    private fun drag(from: LocalDate, to: LocalDate) {
        val start = cell(from).center()
        val end = cell(to).center()
        rule.onRoot().performTouchInput {
            down(start)
            moveTo(end)
            up()
        }
        rule.waitForIdle()
    }

    @Test
    fun `tapping two dates selects a range`() {
        setPicker()
        tap(date(1, 8))
        assertEquals(date(1, 8), state.selectedStartDate)
        assertNull(state.selectedEndDate)
        tap(date(1, 15))
        assertEquals(date(1, 15), state.selectedEndDate)
    }

    @Test
    fun `a single date can be dragged forward`() {
        setPicker(initial = date(1, 8))
        drag(date(1, 8), date(1, 15))
        assertEquals(date(1, 8), state.selectedStartDate)
        assertEquals(date(1, 15), state.selectedEndDate)
    }

    @Test
    fun `a drag continues into the next month`() {
        setPicker(initial = date(1, 28))
        drag(date(1, 28), date(2, 6))
        assertEquals(date(1, 28), state.selectedStartDate)
        assertEquals(date(2, 6), state.selectedEndDate)
    }

    @Test
    fun `holding a drag at the bottom edge scrolls and extends the range`() {
        setPicker(initial = date(1, 8))
        val start = cell(date(1, 8)).center()
        rule.mainClock.autoAdvance = false
        rule.onRoot().performTouchInput {
            down(start)
            moveTo(Offset(start.x, height - 2f))
        }
        rule.mainClock.advanceTimeBy(3_000)
        rule.onRoot().performTouchInput { up() }
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()

        val end = state.selectedEndDate
        assertTrue("Expected the range to reach a later month, was $end", end!! > date(2, 28))
    }

    @Test
    fun `dragging past the anchor flips the range`() {
        setPicker(initial = date(1, 15))
        drag(date(1, 15), date(1, 8))
        assertEquals(date(1, 8), state.selectedStartDate)
        assertEquals(date(1, 15), state.selectedEndDate)
    }

    @Test
    fun `a drag from an unselected date scrolls instead of selecting`() {
        setPicker(initial = date(1, 8))
        val first = cell(date(1, 22)).center()
        rule.onRoot().performTouchInput {
            down(first)
            moveBy(Offset(0f, -300f))
            moveBy(Offset(0f, -300f))
            up()
        }
        rule.waitForIdle()
        assertEquals(date(1, 8), state.selectedStartDate)
        assertNull(state.selectedEndDate)
        assertTrue(state.displayedMonth!! > YearMonth.of(2030, 1))
    }

    @Test
    fun `dates outside the limits and blocked dates cannot be tapped`() {
        setPicker(
            minDate = date(1, 10),
            maxDate = date(1, 20),
            isDateSelectable = { it != date(1, 15) },
        )
        tap(date(1, 8))
        tap(date(1, 15))
        tap(date(1, 25))
        assertNull(state.selectedStartDate)
        tap(date(1, 12))
        assertEquals(date(1, 12), state.selectedStartDate)
    }

    @Test
    fun `single mode replaces the date and does not drag`() {
        setPicker(initial = date(1, 8), selectionMode = DateSelectionMode.SINGLE)
        tap(date(1, 15))
        assertEquals(date(1, 15), state.selectedStartDate)
        assertNull(state.selectedEndDate)
    }

    @Test
    fun `range changes are reported but the initial selection is not`() {
        val changes = mutableListOf<Pair<LocalDate?, LocalDate?>>()
        setPicker(initial = date(1, 8), onRangeChange = { start, end -> changes += start to end })
        assertTrue(changes.isEmpty())
        tap(date(1, 15))
        assertEquals(listOf<Pair<LocalDate?, LocalDate?>>(date(1, 8) to date(1, 15)), changes)
    }

    @Test
    fun `opens on the requested month and scrolls to another`() {
        lateinit var scope: kotlinx.coroutines.CoroutineScope
        rule.setContent {
            scope = rememberCoroutineScope()
            state = rememberDraggableDateRangePickerState(initialStartEpochDay = null)
            DraggableDateRangePicker(
                modifier = Modifier.fillMaxSize(),
                state = state,
                startYear = 2030,
                endYear = 2031,
                minDate = null,
                locale = Locale.US,
                initialDisplayedMonth = YearMonth.of(2030, 6),
            )
        }
        rule.onNodeWithText("June 2030").assertIsDisplayed()
        assertEquals(YearMonth.of(2030, 6), state.displayedMonth)

        scope.launch { state.scrollToMonth(YearMonth.of(2031, 3), animated = false) }
        rule.waitForIdle()
        assertEquals(YearMonth.of(2031, 3), state.displayedMonth)
        rule.onNodeWithText("March 2031").assertIsDisplayed()
    }

    @Test
    fun `months and accessibility text follow the locale`() {
        setPicker(locale = Locale.FRANCE)
        rule.onNodeWithText("janvier 2030").assertIsDisplayed()
        rule.onNode(hasContentDescription("mardi 8 janvier 2030", substring = true)).assertExists()
    }

    @Test
    fun `cells expose accessibility state and a click action`() {
        setPicker(minDate = date(1, 10))
        cell(date(1, 8)).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Disabled))
        cell(date(1, 15)).performSemanticsAction(SemanticsActions.OnClick)
        rule.waitForIdle()
        assertEquals(date(1, 15), state.selectedStartDate)
        cell(date(1, 15)).assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Start of selected range")
        )
        cell(date(1, 15)).assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
    }

    @Test
    fun `tags are read out with the date`() {
        setPicker()
        rule.runOnIdle { state.addTag(date(1, 8).toEpochDay(), CalendarTag.price("$120")) }
        rule.waitForIdle()
        cell(date(1, 8)).assert(
            hasContentDescription("$120", substring = true)
        )
    }

    @Test
    fun `right-to-left layouts mirror taps`() {
        setPicker(layoutDirection = LayoutDirection.Rtl)
        val tuesday = cell(date(1, 8)).center()
        val wednesday = cell(date(1, 9)).center()
        assertTrue(wednesday.x < tuesday.x)
        tap(date(1, 8))
        assertEquals(date(1, 8), state.selectedStartDate)
    }

    @Test
    fun `horizontal pages select dates and turn at the edge during a drag`() {
        setPicker(initial = date(1, 28), orientation = CalendarOrientation.HORIZONTAL)
        assertEquals(YearMonth.of(2030, 1), state.displayedMonth)
        tap(date(1, 8))
        assertEquals(date(1, 8), state.selectedStartDate)
        assertEquals(date(1, 28), state.selectedEndDate)

        val end = cell(date(1, 28)).center()
        rule.mainClock.autoAdvance = false
        rule.onRoot().performTouchInput {
            down(end)
            moveTo(Offset(width - 2f, end.y))
        }
        rule.mainClock.advanceTimeBy(1_500)
        rule.onRoot().performTouchInput { up() }
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        assertTrue(state.displayedMonth!! > YearMonth.of(2030, 1))
        assertTrue(state.selectedEndDate!! > date(1, 31))
    }

    @Test
    fun `custom day content replaces the drawn cell`() {
        rule.setContent {
            state = rememberDraggableDateRangePickerState(initialStartEpochDay = date(1, 8).toEpochDay())
            DraggableDateRangePicker(
                modifier = Modifier.fillMaxSize(),
                state = state,
                startYear = 2030,
                endYear = 2030,
                minDate = null,
                locale = Locale.US,
                firstDayOfWeek = DayOfWeek.MONDAY,
                initialDisplayedMonth = YearMonth.of(2030, 1),
                dayContent = { cell ->
                    if (cell.isRangeStart) Text("start:${cell.dayText}")
                    if (cell.isEnabled && cell.date == date(1, 9)) Text("custom:${cell.dayText}")
                },
            )
        }
        rule.onNode(hasContentDescription(formatter.format(date(1, 8)), substring = true)).assertExists()
        rule.onNodeWithText("start:8", useUnmergedTree = true).assertExists()
        // The cell's own semantics replace the slot's, so screen readers hear the full date.
        rule.onNodeWithText("start:8").assertDoesNotExist()
        assertEquals(date(1, 8), state.selectedStartDate)
    }

    @Test
    fun `dialog presets select dates and confirm returns them`() {
        var confirmed: Pair<LocalDate, LocalDate?>? = null
        rule.setContent {
            DraggableDateRangePickerDialog(
                onDismissRequest = {},
                onConfirm = { start, end -> confirmed = start to end },
            )
        }
        rule.onNodeWithText("Next 7 days").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("OK").performClick()
        rule.waitForIdle()

        val today = LocalDate.now()
        assertEquals(today to today.plusDays(6), confirmed)
    }

    @Test
    fun `saved state with tags can be restored`() {
        val restoration = androidx.compose.ui.test.junit4.StateRestorationTester(rule)
        restoration.setContent {
            state = rememberDraggableDateRangePickerState(initialStartEpochDay = date(1, 8).toEpochDay())
        }
        rule.runOnIdle { state.addTag(date(1, 8).toEpochDay(), CalendarTag.pill("Holiday")) }
        restoration.emulateSavedInstanceStateRestore()
        rule.runOnIdle {
            assertEquals(date(1, 8), state.selectedStartDate)
            assertEquals("Holiday", state.getDateTags().getValue(date(1, 8).toEpochDay()).single().text)
        }
    }
}
