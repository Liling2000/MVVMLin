package com.jiyi.power.app.utils

/** V2.0: C1/C2 use 0x3B/0x3C; 0 = smart, 1 = idle, 2 = custom. */
class ChargingModeProtocol {
    data class Reading(val c1: Int, val c2: Int) {
        // Mixed or unknown port modes cannot select a whole-device mode.
        val selectedMode: Int? get() = c1.takeIf { it == c2 && it in 0..2 }
    }

    private var buffer = ""

    fun reset() { buffer = "" }

    fun readCommand(): String? = MobilePowerProtocolManager.buildReadCommand("3B", 2)

    fun writeCommands(mode: Int): List<String> {
        require(mode in 0..2)
        return listOf("3B", "3C").map {
            requireNotNull(MobilePowerProtocolManager.buildWriteByteCommand(it, mode))
        }
    }

    /** Assemble BLE chunks and require both registers in a read reply; ignore single-byte write ACKs. */
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
            val c1 = frame.registers["3B"]?.value ?: continue
            val c2 = frame.registers["3C"]?.value ?: continue
            readings += Reading(c1, c2)
        }
        return readings
    }
}
