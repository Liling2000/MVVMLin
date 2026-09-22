package com.jiyi.power.app.utils

import java.util.Locale

/** 首页各设备的电量及通知缓冲区，断开连接时一并丢弃。 */
class HomeDeviceBatteryState {
    private class DeviceState(var buffer: String = "", var percent: Int? = null)

    private val devices = mutableMapOf<String, DeviceState>()

    /** 返回本次新连接的设备，供页面发起查询。 */
    fun updateConnectedDevices(connectedSns: Set<String>): Set<String> {
        val keys = connectedSns.map { it.uppercase(Locale.ROOT) }.toSet()
        devices.keys.retainAll(keys)
        return connectedSns.filterTo(mutableSetOf()) { sn ->
            val key = sn.uppercase(Locale.ROOT)
            if (key in devices) false else {
                devices[key] = DeviceState()
                true
            }
        }
    }

    fun percent(sn: String): Int? = devices[sn.uppercase(Locale.ROOT)]?.percent

    fun clear() = devices.clear()

    /** 按设备组帧，兼容分包、粘包及包含电量寄存器的连续读取响应。 */
    fun accept(sn: String, data: String) {
        val state = devices[sn.uppercase(Locale.ROOT)] ?: return
        val chunk = data.filterNot(Char::isWhitespace).uppercase(Locale.ROOT)
        if (chunk.length % 2 != 0 || chunk.any { it !in "0123456789ABCDEF" }) return
        state.buffer = (state.buffer + chunk).takeLast(8192)
        while (state.buffer.length >= 2) {
            if (!state.buffer.startsWith("AA")) {
                state.buffer = state.buffer.drop(2)
                continue
            }
            if (state.buffer.length < 8) break
            val command = state.buffer.substring(2, 4).toInt(16)
            val length = ((command shr 6) shl 8) + state.buffer.substring(6, 8).toInt(16)
            if (length > CmdConstant.MAX_DATA_LENGTH) {
                state.buffer = state.buffer.drop(2)
                continue
            }
            val frameLength = (CmdConstant.MIN_FRAME_LENGTH + length) * 2
            if (state.buffer.length < frameLength) break
            val frame = MobilePowerProtocolManager.parseFrame(state.buffer.take(frameLength))
            if (frame == null) {
                state.buffer = state.buffer.drop(2)
                continue
            }
            state.buffer = state.buffer.drop(frameLength)
            val type = frame.raw.commandCode.toInt(16) and 0x0F
            if (type != CmdConstant.CommandType.RESPONSE && type != CmdConstant.CommandType.EVENT) continue
            val value = frame.registers[CmdConstant.FunctionCode.CODE_16]?.value ?: continue
            state.percent = value.takeIf { it in 0..100 }
        }
    }
}
