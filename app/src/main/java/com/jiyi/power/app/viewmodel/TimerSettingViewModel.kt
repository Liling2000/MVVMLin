package com.jiyi.power.app.viewmodel

import com.jiyi.power.app.bean.TimerOption
import com.jiyi.power.app.bean.TimerSettingType
import com.jiyi.power.app.bean.TimerSettingUiData
import com.jiyi.power.app.utils.CmdConstant
import com.jiyi.power.app.utils.MobilePowerProtocolManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class TimerSettingViewModel : DeviceCommandViewModel() {
    private val _uiState = MutableStateFlow(TimerSettingUiData())
    val uiState = _uiState.asStateFlow()

    fun initialize(type: TimerSettingType, deviceSn: String? = null) {
        bindDevice(deviceSn)
        if (_uiState.value.options.isNotEmpty()) return
        val defaults = listOf(30, 60, 90, 120, 180).map { minutes ->
            TimerOption(name = minutes.toString(), time = minutes, selected = minutes == 30)
        } + TimerOption(name = "", time = 0, isCustom = true)
        _uiState.value = TimerSettingUiData(type, defaults, selectedTime = 30)
    }

    fun select(option: TimerOption) {
        if (option.isCustom) return
        updateSelection(option.time)
    }

    fun setCustomTime(minutes: Int) {
        if (minutes <= 0) return
        val options = _uiState.value.options.map {
            if (it.isCustom) it.copy(name = minutes.toString(), time = minutes) else it
        }
        _uiState.value = _uiState.value.copy(options = options, selectedTime = minutes)
        updateSelection(minutes)
    }

    fun confirm(): Boolean {
        val state = _uiState.value
        // V2.0：bit15 是使能位，低 15 位是分钟数，数据以小端序发送。
        val enabledMinutes = (state.selectedTime.coerceIn(1, 0x7FFF) or 0x8000)
        return sendDeviceCommand(
            if (state.type == TimerSettingType.SHUTDOWN) CmdConstant.FunctionCode.CODE_C0 else CmdConstant.FunctionCode.CODE_C1,
            MobilePowerProtocolManager.buildEventCommand(
                if (state.type == TimerSettingType.SHUTDOWN) CmdConstant.FunctionCode.CODE_C0 else CmdConstant.FunctionCode.CODE_C1,
                byteArrayOf((enabledMinutes and 0xFF).toByte(), (enabledMinutes shr 8).toByte()),
            ),
        )
    }

    private fun updateSelection(minutes: Int) {
        _uiState.update { state ->
            state.copy(
                options = state.options.map { it.copy(selected = it.time == minutes) },
                selectedTime = minutes,
            )
        }
    }
}
