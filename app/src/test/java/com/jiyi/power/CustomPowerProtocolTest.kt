package com.jiyi.power

import com.jiyi.power.app.utils.CustomPowerProtocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomPowerProtocolTest {
    @Test
    fun readsBothCustomPowerRegisters() {
        val protocol = CustomPowerProtocol()

        assertEquals("AA1032024455", protocol.readCommand())
        assertEquals(
            CustomPowerProtocol.Reading(c1Power = 100, c2Power = 65),
            protocol.accept(frame("32", listOf(100, 65))).single(),
        )
    }

    @Test
    fun ignoresWriteAcknowledgementsAndOtherRegisters() {
        val protocol = CustomPowerProtocol()

        assertTrue(protocol.accept(frame("32", listOf(100), command = 0)).isEmpty())
        assertTrue(protocol.accept(frame("3B", listOf(2, 2))).isEmpty())
    }

    private fun frame(code: String, data: List<Int>, command: Int = 2): String {
        val body = listOf(command, code.toInt(16), data.size) + data
        return (listOf(0xAA) + body + listOf(body.sum() and 0xFF, 0x55))
            .joinToString("") { "%02X".format(it) }
    }
}
