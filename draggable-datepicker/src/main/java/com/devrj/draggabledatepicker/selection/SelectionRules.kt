package com.devrj.draggabledatepicker.selection

import com.devrj.draggabledatepicker.enums.DateSelectionMode
import java.time.LocalDate
import kotlin.math.abs

/** A selection as epoch days. [end] is null while only one date has been chosen. */
internal data class SelectionValue(val start: Long?, val end: Long?)

/**
 * Decides what a tap or drag does to the selection. Every date is an epoch day and range lengths
 * count both ends, so a one-day range has a length of 1.
 */
internal data class SelectionRules(
    val mode: DateSelectionMode = DateSelectionMode.RANGE,
    val minEpoch: Long? = null,
    val maxEpoch: Long? = null,
    val isDateSelectable: (LocalDate) -> Boolean = { true },
    val allowUnselectableDatesInRange: Boolean = false,
    val minRangeDays: Int = 1,
    val maxRangeDays: Int? = null,
) {
    private fun isWithinLimits(epoch: Long): Boolean =
        (minEpoch == null || epoch >= minEpoch) && (maxEpoch == null || epoch <= maxEpoch)

    fun isEnabled(epoch: Long): Boolean =
        isWithinLimits(epoch) && isDateSelectable(LocalDate.ofEpochDay(epoch))

    /** Returns the selection after tapping [tapped]. A disabled date changes nothing. */
    fun resolveTap(current: SelectionValue, tapped: Long): SelectionValue {
        if (!isEnabled(tapped)) return current
        if (mode == DateSelectionMode.SINGLE) return SelectionValue(tapped, null)

        val start = current.start
        if (start == null || current.end != null) return SelectionValue(tapped, null)

        val low = minOf(start, tapped)
        val high = maxOf(start, tapped)
        // A second tap that can't complete a valid range starts a new selection instead.
        return if (isValidRange(low, high)) SelectionValue(low, high) else SelectionValue(tapped, null)
    }

    /**
     * Returns the range for a drag that holds [anchor] fixed while the other end follows
     * [target], moving as far as the rules allow. The dragged end can cross over the anchor.
     *
     * @param previousEdge The dragged end's last position. It sets the direction when [target]
     * is on the anchor.
     * @return The new range, or null if no valid range exists in that direction.
     */
    fun resolveDrag(anchor: Long, target: Long, previousEdge: Long): SelectionValue? {
        if (!isEnabled(anchor)) return null

        val direction = when {
            target != anchor -> if (target > anchor) 1L else -1L
            previousEdge != anchor -> if (previousEdge > anchor) 1L else -1L
            else -> 1L
        }
        val longest = (maxRangeDays?.let { it - 1L } ?: Long.MAX_VALUE)
        val distance = maxOf(abs(target - anchor), minRangeDays - 1L).coerceAtMost(longest)

        var reached = 0L
        var step = 1L
        while (step <= distance) {
            val epoch = anchor + direction * step
            if (!isWithinLimits(epoch)) break
            val selectable = isDateSelectable(LocalDate.ofEpochDay(epoch))
            if (selectable) reached = step else if (!allowUnselectableDatesInRange) break
            step++
        }

        if (reached + 1 < minRangeDays) return null
        val edge = anchor + direction * reached
        return SelectionValue(minOf(anchor, edge), maxOf(anchor, edge))
    }

    private fun isValidRange(low: Long, high: Long): Boolean {
        val length = high - low + 1
        if (length < minRangeDays) return false
        if (maxRangeDays != null && length > maxRangeDays) return false
        if (!isEnabled(low) || !isEnabled(high)) return false
        if (allowUnselectableDatesInRange) return true
        return (low + 1 until high).all(::isEnabled)
    }
}
