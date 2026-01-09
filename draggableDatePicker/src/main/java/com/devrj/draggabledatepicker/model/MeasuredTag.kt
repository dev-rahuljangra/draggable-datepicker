package com.inc.adv.draggabledaterangepicker.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult
import com.inc.adv.draggabledaterangepicker.enums.TagAlignment

@Immutable
internal data class MeasuredTag(
    val textLayoutResult: TextLayoutResult,
    val alignment: TagAlignment,
    val backgroundColor : Color,
    val color: Color,
)