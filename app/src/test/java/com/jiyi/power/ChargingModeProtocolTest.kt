package com.jiyi.power

import com.jiyi.power.app.utils.ChargingModeProtocol
import com.jiyi.power.app.utils.ProtocolUtil
import org.junit.Assert.*
import org.junit.Test

class ChargingModeProtocolTest {
    @Test
    fun commandsUseBothPortRegistersAndProtocolModeValues() {
        val protocol = ChargingModeProtocol()
        assertEquals("AA103B024D55", protocol.readCommand())
        for (mode in 0..2) {
            val frames = protocol.writeCommands(mode).map { ProtocolUtil.parseResponse(it)!! }
            assertEquals(listOf("3B", "3C"), frames.map { it.functionCode })
            assertTrue(frames.all { it.commandType == "WRITE" && it.dataHex == "0$mode" })
        }
    }

    @Test
    fun fragmentedAndCombinedReadRepliesAreParsed() {
        val protocol = ChargingModeProtocol()
        val smart = frame("3B", listOf(0, 0))
        assertTrue(protocol.accept(smart.take(10)).isEmpty())
        val readings = protocol.accept(smart.drop(10) + frame("3B", listOf(2, 2)))
        assertEquals(listOf(0, 2), readings.map { it.selectedMode })
    }

    @Test
    fun differingOrUnknownPortModesHaveNoWholeDeviceSelection() {
        val protocol = ChargingModeProtocol()
        assertNull(protocol.accept(frame("3B", listOf(0, 1))).single().selectedMode)
        assertNull(protocol.accept(frame("3B", listOf(3, 3))).single().selectedMode)
    }

    @Test
    fun writeAcksEchoesAndInvalidFramesCannotConfirmMode() {
        val protocol = ChargingModeProtocol()
        assertTrue(protocol.accept(frame("3B", listOf(0))).isEmpty())
        assertTrue(protocol.accept(frame("3C", listOf(0))).isEmpty())
        assertTrue(protocol.accept(frame("3B", listOf(1, 1), 1)).isEmpty())
        assertTrue(protocol.accept(frame("3B", listOf(1, 1)).dropLast(4) + "0055").isEmpty())
        assertEquals(1, protocol.accept(frame("3B", listOf(1, 1))).single().selectedMode)
    }

    @Test
    fun resetDropsPartialRepliesAndLargerRegisterBlocksAreSupported() {
        val protocol = ChargingModeProtocol()
        protocol.accept(frame("3B", listOf(0, 0)).take(10))
        protocol.reset()
        assertEquals(2, protocol.accept(frame("3A", listOf(0, 2, 2))).single().selectedMode)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidModeCannotBeWritten() {
        ChargingModeProtocol().writeCommands(3)
    }

    private fun frame(code: String, data: List<Int>, command: Int = 2): String {
        val body = listOf(command, code.toInt(16), data.size) + data
        return (listOf(0xAA) + body + listOf(body.sum() and 0xFF, 0x55))
            .joinToString("") { "%02X".format(it) }
    }
}
