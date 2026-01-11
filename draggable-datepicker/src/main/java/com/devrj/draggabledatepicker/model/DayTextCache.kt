package com.inc.adv.draggabledaterangepicker.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextLayoutResult

@Immutable
internal data class DayTextCache(
    val standard: TextLayoutResult,
    val weekend: TextLayoutResult,
    val selected: TextLayoutResult,
    val disabled: TextLayoutResult
)