package com.jiyi.power.app.utils

import com.jiyi.power.app.bean.Payload

/** Reads the current C2 screensaver text. */
class ScreenTextProtocol {
    private var buffer = ""

    fun reset() { buffer = "" }

    fun readCommand(): String? =
        MobilePowerProtocolManager.buildReadCommand(CmdConstant.FunctionCode.CODE_C2)

    fun accept(data: String?): List<String> {
        val chunk = data?.filterNot(Char::isWhitespace)?.uppercase() ?: return emptyList()
        if (chunk.length % 2 != 0 || chunk.any { it !in "0123456789ABCDEF" }) return emptyList()
        buffer = (buffer + chunk).takeLast(8192)
        val texts = mutableListOf<String>()
        while (buffer.length >= 8) {
            if (!buffer.startsWith("AA")) {
                buffer = buffer.drop(2)
                continue
            }
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
            if (frame.raw.commandType != "RESPONSE" ||
                frame.raw.functionCode != CmdConstant.FunctionCode.CODE_C2) continue
            (frame.payload as? Payload.Text)?.value?.let(texts::add)
        }
        return texts
    }
}
