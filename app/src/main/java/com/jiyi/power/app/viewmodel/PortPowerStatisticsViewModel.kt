package com.jiyi.power.app.viewmodel

import com.jiyi.power.app.bean.Payload
import com.jiyi.power.app.bean.PortPowerSample
import com.jiyi.power.app.bean.PortPowerStatisticsData
import com.jiyi.power.app.bean.PowerStatisticsCalculator
import com.jiyi.power.app.utils.CmdConstant
import com.jiyi.power.app.utils.MobilePowerProtocolManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class PortPowerStatisticsViewModel : DeviceCommandViewModel() {
    private val _statisticsData = MutableStateFlow(PortPowerStatisticsData())
    val statisticsData = _statisticsData.asStateFlow()

    private var receiveBuffer = ""

    fun requestPortPowers(deviceSn: String?): Boolean {
        bindDevice(deviceSn)
        val command = MobilePowerProtocolManager.buildReadCommand(
            CmdConstant.FunctionCode.CODE_04,
            POWER_REGISTER_COUNT,
        )
        return sendDeviceCommand(CmdConstant.FunctionCode.CODE_04, command)
    }

    override fun onBleDataReceive(data: String?) {
        extractFrames(data).forEach { frameHex ->
            val frame = MobilePowerProtocolManager.parseFrame(frameHex) ?: return@forEach
            if (frame.raw.commandType != RESPONSE_COMMAND_TYPE ||
                !frame.raw.functionCode.equals(CmdConstant.FunctionCode.CODE_04, ignoreCase = true)
            ) return@forEach

            val snapshot = (frame.payload as? Payload.RegisterBlock)?.snapshot ?: return@forEach
            val sample = PortPowerSample(
                c1PowerW = snapshot.c1.powerW?.toFloat() ?: return@forEach,
                c2PowerW = snapshot.c2.powerW?.toFloat() ?: return@forEach,
                a1PowerW = snapshot.usbA.powerW?.toFloat() ?: return@forEach,
            )
            _statisticsData.update { PowerStatisticsCalculator.append(it, sample) }
        }
    }

    override fun onDeviceDisconnected() {
        receiveBuffer = ""
    }

    private fun extractFrames(data: String?): List<String> {
        val chunk = data?.filterNot(Char::isWhitespace)?.uppercase() ?: return emptyList()
        if (chunk.isEmpty() || chunk.length % 2 != 0 || chunk.any { it !in HEX_DIGITS }) {
            return emptyList()
        }
        receiveBuffer = (receiveBuffer + chunk).takeLast(MAX_BUFFER_HEX_LENGTH)
        val frames = mutableListOf<String>()
        while (receiveBuffer.isNotEmpty()) {
            val head = receiveBuffer.indexOf(FRAME_HEAD)
            if (head < 0) {
                receiveBuffer = ""
                break
            }
            if (head % 2 != 0) {
                receiveBuffer = receiveBuffer.drop(head + 1)
                continue
            }
            if (head > 0) receiveBuffer = receiveBuffer.substring(head)
            if (receiveBuffer.length < FRAME_HEADER_HEX_LENGTH) break

            val command = receiveBuffer.substring(2, 4).toInt(16)
            val lowLength = receiveBuffer.substring(6, 8).toInt(16)
            val dataLength = ((command shr 6) shl 8) + lowLength
            val frameHexLength = (CmdConstant.MIN_FRAME_LENGTH + dataLength) * 2
            if (frameHexLength > MAX_FRAME_HEX_LENGTH) {
                receiveBuffer = receiveBuffer.drop(2)
                continue
            }
            if (receiveBuffer.length < frameHexLength) break
            frames += receiveBuffer.substring(0, frameHexLength)
            receiveBuffer = receiveBuffer.substring(frameHexLength)
        }
        return frames
    }

    private companion object {
        // 0x04..0x12: C1/C2 的功率低高字节以及 USB-A 功率。
        const val POWER_REGISTER_COUNT = 0x0F
        const val RESPONSE_COMMAND_TYPE = "RESPONSE"
        const val FRAME_HEAD = "AA"
        const val FRAME_HEADER_HEX_LENGTH = 8
        const val MAX_FRAME_HEX_LENGTH = 4096 * 2
        const val MAX_BUFFER_HEX_LENGTH = 8192
        const val HEX_DIGITS = "0123456789ABCDEF"
    }
}
