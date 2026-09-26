package com.jiyi.power.app.utils

/** Reads the consecutive C1/C2 custom output-power registers at 0x32/0x33. */
class CustomPowerProtocol {
    data class Reading(val c1Power: Int, val c2Power: Int)

    private var buffer = ""

    fun reset() { buffer = "" }

    fun readCommand(): String? = MobilePowerProtocolManager.buildReadCommand("32", 2)

    fun accept(data: String?): List<Reading> {
        val chunk = data?.filterNot(Char::isWhitespace)?.uppercase() ?: return emptyList()
        if (chunk.length % 2 != 0 || chunk.any { it !in "0123456789ABCDEF" }) return emptyList()
        buffer = (buffer + chunk).takeLast(8192)
        val readings = mutableListOf<Reading>()
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
            if (frame.raw.commandType != "RESPONSE") continue
            val c1Power = frame.registers["32"]?.value ?: continue
            val c2Power = frame.registers["33"]?.value ?: continue
            readings += Reading(c1Power, c2Power)
        }
        return readings
    }
}
