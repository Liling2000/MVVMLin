package com.jiyi.power

import com.jiyi.power.app.utils.HomeDeviceBatteryState
import com.jiyi.power.app.utils.MobilePowerProtocolManager
import com.jiyi.power.app.utils.CmdConstant
import org.junit.Assert.*
import org.junit.Test

class HomeDeviceBatteryStateTest {
    @Test
    fun queriesBatteryRegisterAndKeepsDevicesSeparate() {
        assertEquals("AA0016001655", MobilePowerProtocolManager.buildReadCommand(CmdConstant.FunctionCode.CODE_16))
        val state = HomeDeviceBatteryState()
        assertEquals(setOf("aa", "bb"), state.updateConnectedDevices(setOf("aa", "bb")))
        assertNull(state.percent("aa"))
        state.accept("AA", frame(0x16, byteArrayOf(86)))
        state.accept("bb", frame(0x16, byteArrayOf(25)))
        assertEquals(86, state.percent("aa"))
        assertEquals(25, state.percent("BB"))
        assertTrue(state.updateConnectedDevices(setOf("AA", "BB")).isEmpty())
    }

    @Test
    fun disconnectClearsValueAndPartialReplyBeforeReconnect() {
        val state = HomeDeviceBatteryState()
        state.updateConnectedDevices(setOf("aa", "bb"))
        state.accept("aa", frame(0x16, byteArrayOf(86)))
        val pending = frame(0x16, byteArrayOf(50))
        state.accept("aa", pending.take(8))
        state.accept("bb", frame(0x16, byteArrayOf(20)))
        state.updateConnectedDevices(setOf("bb"))
        state.accept("aa", frame(0x16, byteArrayOf(99)))
        assertNull(state.percent("aa"))
        assertEquals(20, state.percent("bb"))
        assertEquals(setOf("aa"), state.updateConnectedDevices(setOf("aa", "bb")))
        state.accept("aa", pending.drop(8))
        assertNull(state.percent("aa"))
        state.accept("aa", frame(0x16, byteArrayOf(0)))
        assertEquals(0, state.percent("aa"))
        state.clear()
        assertNull(state.percent("bb"))
        assertEquals(setOf("bb"), state.updateConnectedDevices(setOf("bb")))
    }

    @Test
    fun fragmentedAndCombinedFramesSupportContinuousReads() {
        val state = HomeDeviceBatteryState()
        state.updateConnectedDevices(setOf("aa", "bb"))
        val first = frame(0x15, byteArrayOf(0, 70), 0x12)
        state.accept("aa", first.take(10))
        state.accept("bb", frame(0x16, byteArrayOf(10)))
        assertNull(state.percent("aa"))
        state.accept("aa", first.drop(10) + frame(0x17, byteArrayOf(25)))
        assertEquals(70, state.percent("aa"))
        state.accept("aa", frame(0x16, byteArrayOf(80)) + frame(0x16, byteArrayOf(100), 3))
        assertEquals(100, state.percent("aa"))
        assertEquals(10, state.percent("bb"))
    }

    @Test
    fun refreshKeepsConfirmedValueAndDropsPartialReply() {
        val state = HomeDeviceBatteryState()
        state.updateConnectedDevices(setOf("aa"))
        state.accept("aa", frame(0x16, byteArrayOf(50)))
        val updated = frame(0x16, byteArrayOf(80))
        state.accept("aa", updated.take(8))

        state.prepareRefresh()

        assertEquals(50, state.percent("aa"))
        state.accept("aa", updated.drop(8))
        assertEquals(50, state.percent("aa"))
        state.accept("aa", updated)
        assertEquals(80, state.percent("aa"))
    }

    @Test
    fun corruptFramesWriteEchoesAndInvalidPercentAreNotDisplayed() {
        val state = HomeDeviceBatteryState()
        state.updateConnectedDevices(setOf("aa"))
        val valid = frame(0x16, byteArrayOf(90))
        state.accept("aa", valid.dropLast(4) + "0055")
        state.accept("aa", frame(0x16, byteArrayOf(80), 1))
        state.accept("aa", "invalid")
        assertNull(state.percent("aa"))
        state.accept("aa", valid)
        assertEquals(90, state.percent("aa"))
        state.accept("aa", frame(0x16, byteArrayOf(0xFF.toByte())))
        assertNull(state.percent("aa"))
    }

    private fun frame(code: Int, data: ByteArray, command: Int = 2): String {
        val body = byteArrayOf(command.toByte(), code.toByte(), data.size.toByte()) + data
        val checksum = body.sumOf { it.toInt() and 0xFF }.toByte()
        return (byteArrayOf(0xAA.toByte()) + body + byteArrayOf(checksum, 0x55))
            .joinToString("") { "%02X".format(it.toInt() and 0xFF) }
    }
}
