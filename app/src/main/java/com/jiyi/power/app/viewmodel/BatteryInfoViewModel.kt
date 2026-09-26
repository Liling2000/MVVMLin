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
        // Keep the last confirmed snapshot visible while a new query round is in flight.
        protocol.prepareRefresh()
        // 循环次数和健康度连续读取；电芯逐路探测，只展示实际返回有效电压的路数。
        sendDeviceCommand(
            CmdConstant.FunctionCode.CODE_1C,
            MobilePowerProtocolManager.buildReadCommand(CmdConstant.FunctionCode.CODE_1C, 3),
        )
        BatteryInfoProtocol.CELL_VOLTAGE_REGISTER_PAIRS.forEach { (lowCode, _) ->
            sendDeviceCommand(
                lowCode,
                MobilePowerProtocolManager.buildReadCommand(lowCode, 2),
            )
        }
        sendDeviceCommand(
            CmdConstant.FunctionCode.CODE_40,
            MobilePowerProtocolManager.buildReadCommand(CmdConstant.FunctionCode.CODE_40, 4),
        )
        // F6 返回电池制造商；无回包或空内容时页面使用产品配置中的默认值。
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
