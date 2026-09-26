package com.jiyi.power

import com.jiyi.power.app.utils.TimerReminderProtocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerReminderProtocolTest {
    @Test
    fun buildsReadCommandAndParsesEnabledMinutes() {
        val protocol = TimerReminderProtocol()

        assertEquals("AA00C100C155", protocol.readCommand())
        assertEquals(listOf(90), protocol.accept(frame(0x805A)))
    }

    @Test
    fun zeroAndFragmentedRepliesAreHandled() {
        val protocol = TimerReminderProtocol()
        val zero = frame(0)

        assertTrue(protocol.accept(zero.take(8)).isEmpty())
        assertEquals(listOf(0), protocol.accept(zero.drop(8)))
    }

    private fun frame(value: Int): String {
        val data = listOf(value and 0xFF, (value shr 8) and 0xFF)
        val body = listOf(2, 0xC1, data.size) + data
        return (listOf(0xAA) + body + listOf(body.sum() and 0xFF, 0x55))
            .joinToString("") { "%02X".format(it) }
    }
}
