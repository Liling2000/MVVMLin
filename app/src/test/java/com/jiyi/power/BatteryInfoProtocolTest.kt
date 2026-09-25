package com.jiyi.power

import com.jiyi.power.app.utils.CmdConstant
import com.jiyi.power.app.utils.BatteryInfoProtocol
import org.junit.Assert.*
import org.junit.Test

class BatteryInfoProtocolTest {
    @Test
    fun fragmentedAndCombinedRepliesFillBatteryFields() {
        val protocol = BatteryInfoProtocol()
        val registers = ByteArray(0x16)
        registers[0] = 0x34
        registers[1] = 0x12
        registers[2] = 97
        listOf(
            CmdConstant.FunctionCode.CODE_23, CmdConstant.FunctionCode.CODE_25,
            CmdConstant.FunctionCode.CODE_27, CmdConstant.FunctionCode.CODE_29,
            CmdConstant.FunctionCode.CODE_2B, CmdConstant.FunctionCode.CODE_2D,
            CmdConstant.FunctionCode.CODE_30,
        ).forEachIndexed { index, code ->
            val voltage = 4000 + index
            registers[code.toInt(16) - CmdConstant.FunctionCode.CODE_1C.toInt(16)] = voltage.toByte()
            registers[code.toInt(16) + 1 - CmdConstant.FunctionCode.CODE_1C.toInt(16)] = (voltage shr 8).toByte()
        }
        val first = frame(CmdConstant.FunctionCode.CODE_1C, registers)
        assertNull(protocol.accept(first.take(18)).healthPercent)
        val state = protocol.accept(first.drop(18) + frame(CmdConstant.FunctionCode.CODE_40, byteArrayOf(61, 0, 0xFF.toByte(), 0xFF.toByte())))
        assertEquals(97, state.healthPercent)
        assertEquals(0x1234, state.cycleCount)
        assertEquals((4000..4006).toList(), state.cells.map { it.voltageMv })
        assertEquals("7", state.batterySeries)
        assertEquals(61L, state.totalDischargeMinutes)
        assertEquals(65535L, state.totalDischargeCapacityMah)
        assertNull(state.model)
        assertNull(state.ratedPowerW)
    }

    @Test
    fun manufacturerTextAndPartialRegistersDoNotEraseOtherFields() {
        val protocol = BatteryInfoProtocol()
        protocol.accept(frame(CmdConstant.FunctionCode.CODE_F6, "电池厂商".toByteArray().copyOf(32), 3))
        protocol.accept(frame(CmdConstant.FunctionCode.CODE_1C, byteArrayOf(12)))
        assertNull(protocol.state.cycleCount)
        val state = protocol.accept(frame(CmdConstant.FunctionCode.CODE_1D, byteArrayOf(1, 88)))
        assertEquals(268, state.cycleCount)
        assertEquals(88, state.healthPercent)
        assertEquals("电池厂商", state.manufacturer)
        assertTrue(state.cells.isEmpty())
        assertNull(state.batterySeries)
    }

    @Test
    fun onlyCellsWithValidVoltageAreShownAndCounted() {
        val protocol = BatteryInfoProtocol()

        protocol.accept(frame(CmdConstant.FunctionCode.CODE_23, littleEndian(4012)))
        protocol.accept(frame(CmdConstant.FunctionCode.CODE_25, littleEndian(0)))
        protocol.accept(frame(CmdConstant.FunctionCode.CODE_27, littleEndian(0xFFFF)))
        val state = protocol.accept(frame(CmdConstant.FunctionCode.CODE_2B, littleEndian(3988)))

        assertEquals(listOf(1, 5), state.cells.map { it.index })
        assertEquals(listOf(4012, 3988), state.cells.map { it.voltageMv })
        assertEquals("2", state.batterySeries)
    }

    @Test
    fun invalidFramesAndWriteEchoesDoNotPopulateState() {
        val protocol = BatteryInfoProtocol()
        val valid = frame(CmdConstant.FunctionCode.CODE_1E, byteArrayOf(90))
        val corrupt = valid.dropLast(4) + "0055"
        assertNull(protocol.accept(corrupt).healthPercent)
        assertNull(protocol.accept(frame(CmdConstant.FunctionCode.CODE_1E, byteArrayOf(90), 1)).healthPercent)
        assertEquals(90, protocol.accept(valid).healthPercent)
        assertNull(protocol.accept(frame(CmdConstant.FunctionCode.CODE_1E, byteArrayOf(0xFF.toByte()))).healthPercent)
    }

    @Test
    fun resetDropsOldValuesAndPartialFrame() {
        val protocol = BatteryInfoProtocol()
        protocol.accept(frame(CmdConstant.FunctionCode.CODE_1E, byteArrayOf(90)))
        protocol.accept(frame(CmdConstant.FunctionCode.CODE_40, byteArrayOf(1, 0)).take(8))
        protocol.reset()
        assertNull(protocol.state.healthPercent)
        assertTrue(protocol.state.cells.isEmpty())
        assertEquals(80, protocol.accept(frame(CmdConstant.FunctionCode.CODE_1E, byteArrayOf(80))).healthPercent)
        assertNull(protocol.state.totalDischargeMinutes)
    }

    private fun frame(code: String, data: ByteArray, command: Int = 2): String {
        val body = byteArrayOf(command.toByte(), code.toInt(16).toByte(), data.size.toByte()) + data
        val checksum = body.sumOf { it.toInt() and 0xFF }.toByte()
        return (byteArrayOf(0xAA.toByte()) + body + byteArrayOf(checksum, 0x55))
            .joinToString("") { "%02X".format(it.toInt() and 0xFF) }
    }

    private fun littleEndian(value: Int) = byteArrayOf(value.toByte(), (value shr 8).toByte())
}
