package com.jiyi.power

import com.jiyi.power.app.bean.Payload
import com.jiyi.power.app.utils.CmdConstant
import com.jiyi.power.app.utils.MobilePowerProtocolManager
import com.jiyi.power.app.utils.ProtocolUtil
import org.junit.Assert.*
import org.junit.Test

class PortDetailsProtocolTest {
    @Test
    fun cableBytesAreUnsignedAndPortsUseTheirOwnPayload() {
        assertEquals(Payload.CableInfo(3, 60), parse(CmdConstant.FunctionCode.CODE_D2, byteArrayOf(3, 60)))
        assertEquals(Payload.CableInfo(5, 240), parse(CmdConstant.FunctionCode.CODE_D4, byteArrayOf(5, 240.toByte())))
    }

    @Test
    fun missingOrMalformedCableDataDoesNotProduceCableInfo() {
        assertEquals(Payload.Empty, parse(CmdConstant.FunctionCode.CODE_D2, byteArrayOf()))
        assertTrue(parse(CmdConstant.FunctionCode.CODE_D4, byteArrayOf(5)) is Payload.Unknown)
        assertTrue(parse(CmdConstant.FunctionCode.CODE_D2, byteArrayOf(3, 60, 1)) is Payload.Unknown)
    }

    @Test
    fun deviceInfoDecodesUtf8AndRemovesPadding() {
        for (code in listOf(CmdConstant.FunctionCode.CODE_D3, CmdConstant.FunctionCode.CODE_D5)) {
            assertEquals(Payload.Text("测试设备"), parse(code, "测试设备".toByteArray(Charsets.UTF_8).copyOf(32)))
            assertEquals(Payload.Text(""), parse(code, ByteArray(32)))
        }
    }

    @Test
    fun detailQueriesUseBlockReadCommands() {
        assertEquals("AA20D200F255", ProtocolUtil.buildBlockReadCommand(CmdConstant.FunctionCode.CODE_D2))
        assertEquals("AA20D500F555", ProtocolUtil.buildBlockReadCommand(CmdConstant.FunctionCode.CODE_D5))
    }

    private fun parse(code: String, data: ByteArray): Payload? {
        val body = byteArrayOf(0x22, code.toInt(16).toByte(), data.size.toByte()) + data
        val checksum = body.sumOf { it.toInt() and 0xFF }.toByte()
        val frame = byteArrayOf(0xAA.toByte()) + body + byteArrayOf(checksum, 0x55)
        return MobilePowerProtocolManager.parseFrame(frame.joinToString("") { "%02X".format(it.toInt() and 0xFF) })?.payload
    }
}
