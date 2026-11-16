package com.devrj.draggabledatepicker

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.devrj.draggabledatepicker.enums.TagAlignment
import com.devrj.draggabledatepicker.enums.TagStyle
import com.devrj.draggabledatepicker.enums.TagType
import com.devrj.draggabledatepicker.model.CalendarTag
import com.devrj.draggabledatepicker.model.toSnapshot
import com.devrj.draggabledatepicker.model.toTag
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class CalendarTagSnapshotTest {
    @Test
    fun `saved tags survive serialization, as a Bundle requires`() {
        val tags = linkedMapOf(
            100L to listOf(
                CalendarTag.pill("Holiday", backgroundColor = Color.Red).toSnapshot(),
                CalendarTag.price("$120").toSnapshot(),
                CalendarTag(
                    text = "Event",
                    alignment = TagAlignment.TOP_END,
                    fontSize = 11.sp,
                    type = TagType.HOLIDAYS,
                    color = Color.Blue,
                ).toSnapshot(),
            ),
        )

        val bytes = ByteArrayOutputStream().also { out ->
            ObjectOutputStream(out).use { it.writeObject(tags) }
        }.toByteArray()

        @Suppress("UNCHECKED_CAST")
        val restored = ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() }
            as Map<Long, List<com.devrj.draggabledatepicker.model.CalendarTagSnapshot>>
        val result = restored.getValue(100L).map { it.toTag() }

        assertEquals(TagStyle.PILL, result[0].style)
        assertEquals(Color.Red, result[0].backgroundColor)
        assertEquals(TextUnit.Unspecified, result[1].fontSize)
        assertEquals(TagType.PRICE, result[1].type)
        assertEquals(11.sp, result[2].fontSize)
        assertEquals(TagAlignment.TOP_END, result[2].alignment)
    }
}
