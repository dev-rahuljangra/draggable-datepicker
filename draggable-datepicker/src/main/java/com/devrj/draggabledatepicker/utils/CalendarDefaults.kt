package com.inc.adv.draggabledaterangepicker.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.inc.adv.draggabledaterangepicker.model.CalendarColors
import com.inc.adv.draggabledaterangepicker.model.CalendarTextStyles
import com.inc.adv.draggabledaterangepicker.ui.theme.selectedDateBackgroundColor
import com.inc.adv.draggabledaterangepicker.ui.theme.selectedRangeBackgroundColor
import com.inc.adv.draggabledaterangepicker.ui.theme.tagColor
import com.inc.adv.draggabledaterangepicker.ui.theme.weekendBackgroundColor

object CalendarDefaults {
    @Composable
    fun colors(
        containerColor: Color = Color.White,
        contentColor: Color = Color.Black,
        selectedDateContainerColor: Color = selectedDateBackgroundColor,
        selectedDateContentColor: Color = Color.White,
        selectedRangeContainerColor: Color = selectedRangeBackgroundColor,
        selectedRangeContentColor: Color = Color.Black,
        disabledContainerColor: Color = Color.Gray,
        disabledContentColor: Color = disabledContainerColor,
        weekendContainerColor: Color = weekendBackgroundColor,
        weekendContentColor: Color = Color.Red,
        cellBorderColor: Color = Color.LightGray.copy(alpha = 0.5f)
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
        tagContentColor = tagColor,
        tagContainerColor = tagColor.copy(alpha = 0.4f)
    )

    @Composable
    fun textStyles(
        monthTextStyles: TextStyle = TextStyle(
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black
        ),
        dayTextStyles: TextStyle = TextStyle(
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            color = Color.Black
        ),
        selectedDayTextStyles: TextStyle = TextStyle(
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        ),
        weekendTextStyles: TextStyle = TextStyle(
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            color = Color.Red
        ),
        disabledDayTextStyles: TextStyle = TextStyle(
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            color = Color.Gray
        ),
        tagTextStyle: TextStyle = TextStyle(
            fontSize = 9.sp,
            fontWeight = FontWeight.Normal,
            color = Color.DarkGray
        )
    ): CalendarTextStyles = CalendarTextStyles(
        monthTextStyle = monthTextStyles,
        dayTextStyle = dayTextStyles,
        selectedDayTextStyle = selectedDayTextStyles,
        weekendTextStyle = weekendTextStyles,
        disabledDayTextStyle = disabledDayTextStyles,
        tagTextStyle =  tagTextStyle
    )
}