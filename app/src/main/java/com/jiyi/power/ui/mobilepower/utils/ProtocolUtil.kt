package com.jiyi.power.ui.mobilepower.utils

import com.jiyi.power.ui.mobilepower.bean.ProtocolParsedData
import com.jiyi.power.ui.mobilepower.utils.CmdConstant.BYTE_MASK
import com.jiyi.power.ui.mobilepower.utils.CmdConstant.CommandBit.BLOCK
import com.jiyi.power.ui.mobilepower.utils.CmdConstant.CommandBit.CONTINUOUS
import com.jiyi.power.ui.mobilepower.utils.CmdConstant.CommandBit.TYPE_MASK
import com.jiyi.power.ui.mobilepower.utils.CmdConstant.CommandType.EVENT
import com.jiyi.power.ui.mobilepower.utils.CmdConstant.CommandType.READ
import com.jiyi.power.ui.mobilepower.utils.CmdConstant.CommandType.RESPONSE
import com.jiyi.power.ui.mobilepower.utils.CmdConstant.CommandType.WRITE
import com.jiyi.power.ui.mobilepower.utils.CmdConstant.FRAME_HEAD
import com.jiyi.power.ui.mobilepower.utils.CmdConstant.FRAME_TAIL
import com.jiyi.power.ui.mobilepower.utils.CmdConstant.INDEX_COMMAND_CODE
import com.jiyi.power.ui.mobilepower.utils.CmdConstant.INDEX_DATA_LENGTH
import com.jiyi.power.ui.mobilepower.utils.CmdConstant.INDEX_DATA_START
import com.jiyi.power.ui.mobilepower.utils.CmdConstant.INDEX_FRAME_HEAD
import com.jiyi.power.ui.mobilepower.utils.CmdConstant.INDEX_FUNCTION_CODE
import com.jiyi.power.ui.mobilepower.utils.CmdConstant.MAX_DATA_LENGTH
import com.jiyi.power.ui.mobilepower.utils.CmdConstant.MIN_FRAME_LENGTH
import java.util.Locale

/**
 * 移动电源底层协议工具。
 *
 * 这里仅处理“帧”的通用规则：组包、拆包、长度校验、校验和校验。
 * 功能码对应的业务含义由 [MobilePowerProtocolManager] 继续解析。
 *
 * 帧格式：
 * AA | 命令码 | 功能码 | 数据长度低 8 位 | 数据区 | 校验和 | 55
 *
 * 命令码的 bit 分布：
 * - bit0-bit3：命令类型，READ/WRITE/RESPONSE/EVENT。
 * - bit4：连续读取标记。
 * - bit5：块数据标记。
 * - bit6-bit7：数据长度高 2 位，所以最大数据长度是 0x3FF。
 */
object ProtocolUtil {

    /** 普通写命令：命令码类型为 WRITE，数据长度来自 dataHex。 */
    fun buildWriteCommand(
        functionCode: String, dataHex: String = ""
    ): String? {
        return buildCommand(
            commandType = WRITE, functionCode = functionCode, dataHex = dataHex
        )
    }

    fun buildBlockWriteCommand(
        functionCode: String, dataHex: String
    ): String? {
        return buildCommand(
            commandType = WRITE, functionCode = functionCode, dataHex = dataHex, isBlock = true
        )
    }

    fun buildEventCommand(
        functionCode: String, dataHex: String = ""
    ): String? {
        return buildCommand(
            commandType = EVENT, functionCode = functionCode, dataHex = dataHex
        )
    }

    fun buildReadCommand(
        functionCode: String, dataHex: String = ""
    ): String? {
        return buildCommand(
            commandType = READ, functionCode = functionCode, dataHex = dataHex
        )
    }

    fun buildReadCommand(
        functionCode: String, readLength: Int
    ): String? {
        return buildCommand(
            commandType = READ,
            functionCode = functionCode,
            dataHex = "",
            isContinuous = true,
            dataLengthOverride = readLength
        )
    }

    private fun buildCommand(
        commandType: Int,
        functionCode: String,
        dataHex: String,
        isBlock: Boolean = false,
        isContinuous: Boolean = false,
        dataLengthOverride: Int? = null
    ): String? {
        // 先把外部传入的十六进制字符串标准化并转成字节，任一字段非法时直接返回 null。
        // 这样调用方可以把 null 视为“命令无法构造”，避免发出半合法的帧。
        val functionCodeByte = functionCode.hexToSingleByte() ?: return null
        val dataBytes = dataHex.hexToByteArrayOrNull() ?: return null
        val dataLength = dataLengthOverride ?: dataBytes.size

        // 协议只有 10 bit 表示数据长度：命令码 bit6-bit7 是高 2 位，
        // dataLength 字段是低 8 位，所以允许范围是 0..0x3FF。
        if (dataLength !in 0..MAX_DATA_LENGTH) return null
        if (dataLengthOverride != null && dataBytes.isNotEmpty() && dataLengthOverride != dataBytes.size) {
            return null
        }

        val commandCode = buildCommandCode(
            commandType = commandType,
            isBlock = isBlock,
            isContinuous = isContinuous,
            dataLengthExtend = dataLength shr 8
        ) ?: return null

        // 最小帧长包含：帧头、命令码、功能码、长度、校验和、帧尾。
        // 数据区从 INDEX_DATA_START 开始，长度为 dataBytes.size。
        val frame = ByteArray(MIN_FRAME_LENGTH + dataBytes.size)

        frame[INDEX_FRAME_HEAD] = FRAME_HEAD.toByte()
        frame[INDEX_COMMAND_CODE] = commandCode.toByte()
        frame[INDEX_FUNCTION_CODE] = functionCodeByte
        frame[INDEX_DATA_LENGTH] = (dataLength and BYTE_MASK).toByte()

        // 连续读取命令通常没有数据区，但 dataLengthOverride 会声明期望读取的长度；
        // 其他写入/事件命令则把 dataHex 的真实字节复制到数据区。
        dataBytes.copyInto(
            destination = frame, destinationOffset = INDEX_DATA_START
        )

        // 校验和只覆盖命令码到数据区，帧头 AA 和帧尾 55 不参与计算。
        frame[frame.lastIndex - 1] = calcChecksum(frame) ?: return null
        frame[frame.lastIndex] = FRAME_TAIL.toByte()

        return frame.toHexString()
    }

    private fun buildCommandCode(
        commandType: Int, isBlock: Boolean, isContinuous: Boolean, dataLengthExtend: Int
    ): Int? {
        // 命令类型只允许协议定义的 4 类；长度扩展只占命令码高 2 位。
        if (commandType !in READ..EVENT) {
            return null
        }

        if (dataLengthExtend !in 0..3) {
            return null
        }

        var commandCode = commandType

        // bit4/bit5 是独立标志位，可以和 READ/WRITE/EVENT 等命令类型组合。
        if (isContinuous) {
            commandCode = commandCode or CONTINUOUS
        }

        if (isBlock) {
            commandCode = commandCode or BLOCK
        }

        commandCode = commandCode or (dataLengthExtend shl 6)

        return commandCode
    }

    fun calcChecksum(frame: ByteArray): Byte? {
        if (frame.size < MIN_FRAME_LENGTH) return null

        var sum = 0
        val checksumIndex = frame.lastIndex - 1

        // 协议校验和为命令码、功能码、长度和数据区逐字节求和后取低 8 位。
        for (index in INDEX_COMMAND_CODE until checksumIndex) {
            sum += frame[index].toPositiveInt()
        }

        return (sum and BYTE_MASK).toByte()
    }

    fun parseResponse(responseHex: String): ProtocolParsedData? {
        // 解析入口保持严格：十六进制非法、帧头帧尾不匹配、长度不一致、校验失败都返回 null。
        val frame = responseHex.hexToByteArrayOrNull() ?: return null

        if (frame.size < MIN_FRAME_LENGTH) return null
        if (frame.first().toPositiveInt() != FRAME_HEAD) return null
        if (frame.last().toPositiveInt() != FRAME_TAIL) return null

        val commandCodeValue = frame[INDEX_COMMAND_CODE].toPositiveInt()
        val functionCodeValue = frame[INDEX_FUNCTION_CODE].toPositiveInt()
        val dataLengthExtend = commandCodeValue shr 6
        val dataLength = (dataLengthExtend shl 8) + frame[INDEX_DATA_LENGTH].toPositiveInt()

        // 按协议长度字段反推完整帧长，必须和实际字节数完全一致，避免截断包或粘包被误解析。
        val expectedLength = MIN_FRAME_LENGTH + dataLength
        if (frame.size != expectedLength) return null

        val receivedChecksum = frame[frame.lastIndex - 1]
        val calculatedChecksum = calcChecksum(frame) ?: return null

        if (receivedChecksum != calculatedChecksum) return null

        // 低 4 位才是命令类型，高位上的连续/块/长度扩展标记不能参与类型判断。
        val commandTypeValue = commandCodeValue and TYPE_MASK
        val commandType = commandTypeValue.toCommandTypeString() ?: return null

        // 数据区长度由协议字段决定，不包含校验和和帧尾。
        val dataBytes = frame.copyOfRange(
            fromIndex = INDEX_DATA_START, toIndex = INDEX_DATA_START + dataLength
        )

        return ProtocolParsedData(
            commandCode = commandCodeValue.toHexByteString(),
            commandType = commandType,
            isBlock = commandCodeValue and BLOCK != 0,
            isContinuous = commandCodeValue and CONTINUOUS != 0,
            dataLengthExtend = dataLengthExtend,
            functionCode = functionCodeValue.toHexByteString(),
            dataHex = dataBytes.toHexString()
        )
    }

    private fun String.hexToByteArrayOrNull(): ByteArray? {
        val cleanHex = cleanHexString()

        if (cleanHex.isEmpty()) return byteArrayOf()
        if (cleanHex.length % 2 != 0) return null

        return try {
            ByteArray(cleanHex.length / 2) { index ->
                cleanHex.substring(index * 2, index * 2 + 2).toInt(16).toByte()
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun String.hexToSingleByte(): Byte? {
        val bytes = hexToByteArrayOrNull() ?: return null
        return if (bytes.size == 1) bytes[0] else null
    }

    private fun String.cleanHexString(): String {
        return this.replace(" ", "").replace("0x", "", ignoreCase = true).replace("\n", "")
            .replace("\r", "").replace("\t", "").uppercase(Locale.US)
    }

    private fun ByteArray.toHexString(): String {
        return joinToString(separator = "") {
            it.toPositiveInt().toHexByteString()
        }
    }

    private fun Byte.toPositiveInt(): Int {
        return this.toInt() and BYTE_MASK
    }

    private fun Int.toHexByteString(): String {
        return "%02X".format(Locale.US, this and BYTE_MASK)
    }

    private fun Int.toCommandTypeString(): String? {
        return when (this) {
            READ -> "READ"
            WRITE -> "WRITE"
            RESPONSE -> "RESPONSE"
            EVENT -> "EVENT"
            else -> null
        }
    }
}
