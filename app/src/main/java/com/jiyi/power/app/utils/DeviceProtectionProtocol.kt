package com.jiyi.power.app.utils

import com.jiyi.power.app.bean.ParsedFrame
import java.util.Locale

/** 保留未完成的 BLE 分包；只输出通过帧校验的响应。 */
class DeviceProtectionProtocol {
    private var buffer = ""

    fun reset() { buffer = "" }

    fun accept(data: String?): List<ParsedFrame> {
        val chunk = data?.filterNot(Char::isWhitespace)?.uppercase(Locale.ROOT) ?: return emptyList()
        if (chunk.length % 2 != 0 || chunk.any { it !in "0123456789ABCDEF" }) return emptyList()
        buffer = (buffer + chunk).takeLast(8192)
        val frames = mutableListOf<ParsedFrame>()
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
            frames += frame
        }
        return frames
    }
}