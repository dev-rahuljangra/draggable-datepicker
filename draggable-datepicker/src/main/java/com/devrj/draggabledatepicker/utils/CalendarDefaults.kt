package com.devrj.draggabledatepicker.utils

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.os.ConfigurationCompat
import com.devrj.draggabledatepicker.R
import com.devrj.draggabledatepicker.model.CalendarColors
import com.devrj.draggabledatepicker.model.CalendarTextStyles
import com.devrj.draggabledatepicker.model.DateRangePreset
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters
import java.util.Locale

object CalendarDefaults {
    /** Colors from the current [MaterialTheme], so light, dark and dynamic themes all work. */
    @Composable
    fun colors(
        containerColor: Color = MaterialTheme.colorScheme.surface,
        contentColor: Color = MaterialTheme.colorScheme.onSurface,
        selectedDateContainerColor: Color = MaterialTheme.colorScheme.primary,
        selectedDateContentColor: Color = MaterialTheme.colorScheme.onPrimary,
        selectedRangeContainerColor: Color = MaterialTheme.colorScheme.primaryContainer,
        selectedRangeContentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
        disabledContainerColor: Color = Color.Transparent,
        disabledContentColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        weekendContainerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        weekendContentColor: Color = MaterialTheme.colorScheme.error,
        cellBorderColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        tagContentColor: Color = MaterialTheme.colorScheme.onTertiaryContainer,
        tagContainerColor: Color = MaterialTheme.colorScheme.tertiaryContainer,
    ): CalendarColors = CalendarColors(
        containerColor = containerColor,
        contentColor = contentColor,
        selectedDateContainerColor = selectedDateContainerColor,
        selectedDateContentColor = selectedDateContentColor,
        selectedRangeContainerColor = selectedRangeContainerColor,
        selectedRangeContentColor = selectedRangeContentColor,
        disabledContainerColor = disabledContainerColor,
        disabledContentColor = disabledContentColor,
        weekendContainerColor = weekendContainerColor,
        weekendContentColor = weekendContentColor,
        cellBorderColor = cellBorderColor,
        tagContentColor = tagContentColor,
        tagContainerColor = tagContainerColor,
    )

    /**
     * Text styles without a color, so each one takes its color from [CalendarColors]. Set a color
     * on a style to override that.
     */
    @Composable
    fun textStyles(
        monthTextStyles: TextStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
        dayTextStyles: TextStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal),
        selectedDayTextStyles: TextStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
        weekendTextStyles: TextStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal),
        disabledDayTextStyles: TextStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal),
        tagTextStyle: TextStyle = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Medium),
    ): CalendarTextStyles = CalendarTextStyles(
        monthTextStyle = monthTextStyles,
        dayTextStyle = dayTextStyles,
        selectedDayTextStyle = selectedDayTextStyles,
        weekendTextStyle = weekendTextStyles,
        disabledDayTextStyle = disabledDayTextStyles,
        tagTextStyle = tagTextStyle,
    )

    /** The locale from the current configuration. */
    @Composable
    fun locale(): Locale =
        ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: Locale.getDefault()

    /** Today's date, which updates at midnight while the picker is on screen. */
    @Composable
    fun today(): LocalDate {
        var today by remember { mutableStateOf(LocalDate.now()) }
        LaunchedEffect(Unit) {
            while (true) {
                val now = LocalDateTime.now()
                val midnight = now.toLocalDate().plusDays(1).atStartOfDay()
                delay(Duration.between(now, midnight).toMillis() + 1_000L)
                today = LocalDate.now()
            }
        }
        return today
    }

    /** Shortcuts for the dialog and bottom sheet: today, the next 7 and 30 days, and this month. */
    @Composable
    fun presets(): List<DateRangePreset> {
        val todayLabel = stringResource(R.string.ddp_preset_today)
        val next7Label = stringResource(R.string.ddp_preset_next_7_days)
        val next30Label = stringResource(R.string.ddp_preset_next_30_days)
        val monthLabel = stringResource(R.string.ddp_preset_rest_of_month)
        return remember(todayLabel, next7Label, next30Label, monthLabel) {
            listOf(
                DateRangePreset(todayLabel) { it..it },
                DateRangePreset(next7Label) { it..it.plusDays(6) },
                DateRangePreset(next30Label) { it..it.plusDays(29) },
                DateRangePreset(monthLabel) { it..it.with(TemporalAdjusters.lastDayOfMonth()) },
            )
        }
    }
}
