package com.jiyi.power

import com.jiyi.power.app.bean.DeviceExceptionReply
import com.jiyi.power.app.utils.CmdConstant
import com.jiyi.power.app.utils.DeviceExceptionLogReader
import com.jiyi.power.app.utils.DeviceProtectionProtocol
import com.jiyi.power.app.utils.MobilePowerProtocolManager
import org.junit.Assert.*
import org.junit.Test

class DeviceProtectionProtocolTest {
    @Test
    fun commandsReadStorageThenLogs() {
        assertEquals("AA00D100D155", MobilePowerProtocolManager.buildReadCommand(CmdConstant.FunctionCode.CODE_D1))
        assertEquals("AA00D000D055", MobilePowerProtocolManager.buildReadCommand(CmdConstant.FunctionCode.CODE_D0))
    }

    @Test
    fun storageCountUsesLittleEndianAndPreservesDisabledFlag() {
        val reply = decode(0xD1, byteArrayOf(1, 2, 1)) as DeviceExceptionReply.Storage
        assertTrue(reply.disabled)
        assertEquals(258, reply.count)
        assertEquals(DeviceExceptionReply.Invalid, decode(0xD1, byteArrayOf(0, 1)))
        assertEquals(DeviceExceptionReply.Invalid, decode(0xD1, byteArrayOf(2, 0, 0)))
    }

    @Test
    fun allDefinedTypesAndUnknownTypesKeepRawData() {
        for (code in (1..9) + 0x42) {
            val page = decode(0xD0, record(code)) as DeviceExceptionReply.Page
            val item = page.records.single()
            assertEquals(code, item.typeCode)
            assertEquals(if (code == 1) 3 else null, item.batteryNumber)
            assertEquals("3412", item.parameterHex)
            assertEquals("0102030405060708", item.timestampHex)
        }
    }

    @Test
    fun paddingIsNotARecordAndSentinelAndMalformedPagesAreRejected() {
        val page = decode(0xD0, record(5) + ByteArray(60)) as DeviceExceptionReply.Page
        assertEquals(listOf(5), page.records.map { it.typeCode })
        assertTrue((decode(0xD0, ByteArray(72)) as DeviceExceptionReply.Page).records.isEmpty())
        for (data in listOf(byteArrayOf(), ByteArray(11), ByteArray(73), record(0xFF))) {
            assertEquals(DeviceExceptionReply.Invalid, decode(0xD0, data))
        }
    }

    @Test
    fun readsMultiplePagesWithoutResettingOffsetAndKeepsRepeatedTypes() {
        val reader = DeviceExceptionLogReader()
        reader.begin()
        assertEquals("D1", reader.expectedCode)
        assertTrue(reader.accept(parsed(0xD1, byteArrayOf(0, 7, 0))))
        assertEquals("D0", reader.expectedCode)
        assertFalse(reader.accept(parsed(0xD1, byteArrayOf(0, 7, 0))))
        assertTrue(reader.accept(parsed(0xD0, (1..6).flatMap { record(it).toList() }.toByteArray())))
        assertEquals(6, reader.records.size)
        assertEquals("D0", reader.expectedCode)
        assertFalse(reader.complete)
        assertTrue(reader.accept(parsed(0xD0, record(5) + ByteArray(60))))
        assertTrue(reader.complete)
        assertNull(reader.expectedCode)
        assertEquals((0..6).toList(), reader.records.map { it.index })
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 5), reader.records.map { it.typeCode })
    }

    @Test
    fun emptyStorageCompletesWithoutReadingD0() {
        val reader = DeviceExceptionLogReader()
        reader.begin()
        reader.accept(parsed(0xD1, byteArrayOf(1, 0, 0)))
        assertTrue(reader.complete)
        assertTrue(reader.disabled == true)
        assertNull(reader.expectedCode)
        assertTrue(reader.records.isEmpty())
    }

    @Test
    fun earlyEmptyPageAndInconsistentCountFailInsteadOfLoopingOrReportingSuccess() {
        for (data in listOf(ByteArray(72), record(1) + record(2))) {
            val reader = DeviceExceptionLogReader()
            reader.begin()
            reader.accept(parsed(0xD1, byteArrayOf(0, 1, 0)))
            reader.accept(parsed(0xD0, data))
            assertTrue(reader.failed)
            assertFalse(reader.complete)
            assertNull(reader.expectedCode)
        }
    }

    @Test
    fun stoppedOrUnexpectedResponsesCannotAdvanceAndRefreshStartsAtD1() {
        val reader = DeviceExceptionLogReader()
        reader.begin()
        assertFalse(reader.accept(parsed(0xD0, record(1))))
        reader.accept(parsed(0xD1, byteArrayOf(0, 2, 0)))
        reader.accept(parsed(0xD0, record(1)))
        reader.stop()
        assertFalse(reader.complete)
        assertFalse(reader.accept(parsed(0xD0, record(2))))
        reader.begin()
        assertEquals("D1", reader.expectedCode)
        assertTrue(reader.records.isEmpty())
        assertNull(reader.totalCount)
    }

    @Test
    fun fragmentedAndCombinedFramesAreValidatedBeforeParsing() {
        val protocol = DeviceProtectionProtocol()
        val response = frame(0xD0, record(9) + ByteArray(60))
        assertTrue(protocol.accept(response.take(14)).isEmpty())
        val frames = protocol.accept(response.drop(14) + frame(0xD1, byteArrayOf(0, 1, 0)))
        assertEquals(listOf("D0", "D1"), frames.map { it.raw.functionCode })
        assertNull(decode(0xD1, byteArrayOf(0, 1, 0), 1))
        assertTrue(protocol.accept(response.dropLast(4) + "0055").isEmpty())
        assertTrue(protocol.accept("invalid").isEmpty())
        assertEquals(1, protocol.accept(response).size)
    }

    @Test
    fun disconnectResetDropsPartialFrame() {
        val protocol = DeviceProtectionProtocol()
        val response = frame(0xD1, byteArrayOf(0, 0, 0))
        protocol.accept(response.take(8))
        protocol.reset()
        assertTrue(protocol.accept(response.drop(8)).isEmpty())
        assertEquals(1, protocol.accept(response).size)
    }

    private fun decode(code: Int, data: ByteArray, command: Int = 2) =
        MobilePowerProtocolManager.toExceptionReply(parsed(code, data, command))

    private fun parsed(code: Int, data: ByteArray, command: Int = 2) =
        MobilePowerProtocolManager.parseFrame(frame(code, data, command))!!

    private fun record(code: Int) =
        byteArrayOf(code.toByte(), 3, 0x34, 0x12, 1, 2, 3, 4, 5, 6, 7, 8)

    private fun frame(code: Int, data: ByteArray, command: Int = 2): String {
        val body = byteArrayOf(command.toByte(), code.toByte(), data.size.toByte()) + data
        val checksum = body.sumOf { it.toInt() and 0xFF }.toByte()
        return (byteArrayOf(0xAA.toByte()) + body + byteArrayOf(checksum, 0x55))
            .joinToString("") { "%02X".format(it.toInt() and 0xFF) }
    }
}