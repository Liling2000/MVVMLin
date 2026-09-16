package com.jiyi.power.app.viewmodel

import com.jiyi.power.app.utils.CmdConstant
import com.jiyi.power.app.utils.BatteryInfoProtocol
import com.jiyi.power.app.utils.MobilePowerProtocolManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class BatteryInfoViewModel : DeviceCommandViewModel() {
    private val protocol = BatteryInfoProtocol()
    private val _uiState = MutableStateFlow(protocol.state)
    val uiState = _uiState.asStateFlow()

    fun refresh() {
        protocol.reset()
        _uiState.value = protocol.state
        // 循环次数、健康度及全部七路电芯电压。
        sendDeviceCommand(
            CmdConstant.FunctionCode.CODE_1C,
            MobilePowerProtocolManager.buildReadCommand(CmdConstant.FunctionCode.CODE_1C, 0x16),
        )
        sendDeviceCommand(
            CmdConstant.FunctionCode.CODE_40,
            MobilePowerProtocolManager.buildReadCommand(CmdConstant.FunctionCode.CODE_40, 4),
        )
        // 沿用 F0..F7 设备信息的空 payload 事件查询方式。
        sendDeviceCommand(
            CmdConstant.FunctionCode.CODE_F6,
            MobilePowerProtocolManager.buildEventCommand(CmdConstant.FunctionCode.CODE_F6),
        )
    }

    override fun onBleDataReceive(data: String?) {
        super.onBleDataReceive(data)
        _uiState.value = protocol.accept(data)
    }

    override fun onDeviceReconnected() = refresh()

    override fun onDeviceDisconnected() {
        protocol.reset()
        _uiState.value = protocol.state
    }
}
