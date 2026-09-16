package com.jiyi.power.app.utils

import com.jiyi.power.app.bean.BatteryCellInfo
import com.jiyi.power.app.bean.BatteryInfoUiData
import com.jiyi.power.app.bean.ParsedFrame
import com.jiyi.power.app.bean.Payload

/** 电池页的通知组帧和增量回填；不将缺失字段替换成示例值。 */
class BatteryInfoProtocol {
    private var buffer = ""
    private val registers = mutableMapOf<String, Int>()
    var state = BatteryInfoUiData()
        private set

    fun reset() {
        buffer = ""
        registers.clear()
        state = BatteryInfoUiData()
    }

    fun accept(data: String?): BatteryInfoUiData {
        val chunk = data?.filterNot(Char::isWhitespace)?.uppercase() ?: return state
        if (chunk.length % 2 != 0 || chunk.any { it !in "0123456789ABCDEF" }) return state
        buffer = (buffer + chunk).takeLast(8192)
        while (buffer.length >= 2) {
            if (!buffer.startsWith("AA")) {
                buffer = buffer.drop(2)
                continue
            }
            if (buffer.length < 8) break
            val command = buffer.substring(2, 4).toInt(16)
            val length = ((command shr 6) shl 8) + buffer.substring(6, 8).toInt(16)
            if (length > CmdConstant.MAX_DATA_LENGTH) {
                buffer = buffer.drop(2)
                continue
            }
            val frameLength = (CmdConstant.MIN_FRAME_LENGTH + length) * 2
            if (buffer.length < frameLength) break
            val frame = MobilePowerProtocolManager.parseFrame(buffer.take(frameLength))
            if (frame == null) {
                buffer = buffer.drop(2)
                continue
            }
            buffer = buffer.drop(frameLength)
            applyFrame(frame)
        }
        return state
    }

    private fun applyFrame(frame: ParsedFrame) {
        val type = frame.raw.commandCode.toInt(16) and 0x0F
        if (type != CmdConstant.CommandType.RESPONSE && type != CmdConstant.CommandType.EVENT) return
        if (frame.raw.functionCode == CmdConstant.FunctionCode.CODE_F6) {
            val text = frame.payload as? Payload.Text ?: return
            state = state.copy(manufacturer = text.value.takeIf { it.isNotBlank() })
            return
        }
        if (frame.payload !is Payload.RegisterBlock) return
        registers.putAll(frame.registers.mapValues { it.value.value })
        fun u16(lowCode: String, highCode: String): Int? {
            val lo = registers[lowCode] ?: return null
            val hi = registers[highCode] ?: return null
            return lo or (hi shl 8)
        }
        state = state.copy(
            healthPercent = registers[CmdConstant.FunctionCode.CODE_1E]?.takeIf { it in 0..100 },
            cycleCount = u16(CmdConstant.FunctionCode.CODE_1C, CmdConstant.FunctionCode.CODE_1D),
            // 没有串数和未安装电芯的判定规则，保留全部七路原始读数。
            cells = listOf(
                CmdConstant.FunctionCode.CODE_23 to CmdConstant.FunctionCode.CODE_24,
                CmdConstant.FunctionCode.CODE_25 to CmdConstant.FunctionCode.CODE_26,
                CmdConstant.FunctionCode.CODE_27 to CmdConstant.FunctionCode.CODE_28,
                CmdConstant.FunctionCode.CODE_29 to CmdConstant.FunctionCode.CODE_2A,
                CmdConstant.FunctionCode.CODE_2B to CmdConstant.FunctionCode.CODE_2C,
                CmdConstant.FunctionCode.CODE_2D to CmdConstant.FunctionCode.CODE_2E,
                CmdConstant.FunctionCode.CODE_30 to CmdConstant.FunctionCode.CODE_31,
            ).mapIndexed { index, (low, high) -> BatteryCellInfo(index + 1, u16(low, high)) },
            totalDischargeMinutes = u16(CmdConstant.FunctionCode.CODE_40, CmdConstant.FunctionCode.CODE_41)?.toLong(),
            totalDischargeCapacityMah = u16(CmdConstant.FunctionCode.CODE_42, CmdConstant.FunctionCode.CODE_43)?.toLong(),
        )
    }
}
