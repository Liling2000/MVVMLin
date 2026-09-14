package com.jiyi.power.app.viewmodel

import com.jiyi.power.app.bean.CustomTimeUiState
import com.jiyi.power.app.bean.TimerSettingType
import com.jiyi.power.app.utils.CmdConstant
import com.jiyi.power.app.utils.MobilePowerProtocolManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class CustomTimeViewModel : DeviceCommandViewModel() {
    private val _uiState = MutableStateFlow(CustomTimeUiState())
    val uiState = _uiState.asStateFlow()
    private var type = TimerSettingType.SHUTDOWN

    fun initialize(settingType: TimerSettingType, initialMinutes: Int, deviceSn: String? = null) {
        bindDevice(deviceSn)
        type = settingType
        updateTotalMinutes(initialMinutes)
    }

    fun updateHour(hour: Int) = updateTime(hour, _uiState.value.minute)
    fun updateMinute(minute: Int) = updateTime(_uiState.value.hour, minute)
    fun addMinutes(minutes: Int) = updateTotalMinutes(_uiState.value.totalMinutes + minutes)
    fun reset() = updateTime(0, 0)

    fun confirm(): Boolean {
        // 与快捷定时相同：0x8000 表示启用，其余位保存分钟数。
        val enabledMinutes = (_uiState.value.totalMinutes.coerceIn(1, 0x7FFF) or 0x8000)
        val code = if (type == TimerSettingType.SHUTDOWN) CmdConstant.FunctionCode.CODE_C0 else CmdConstant.FunctionCode.CODE_C1
        return sendDeviceCommand(
            code,
            MobilePowerProtocolManager.buildEventCommand(
                code, byteArrayOf((enabledMinutes and 0xFF).toByte(), (enabledMinutes shr 8).toByte()),
            ),
        )
    }

    private fun updateTime(hour: Int, minute: Int) = updateTotalMinutes(hour * 60 + minute)

    private fun updateTotalMinutes(minutes: Int) {
        val safeTotal = minutes.coerceIn(MIN_HOUR * 60 + MIN_MINUTE, MAX_HOUR * 60 + MAX_MINUTE)
        _uiState.value = CustomTimeUiState(safeTotal / 60, safeTotal % 60)
    }

    companion object {
        const val MIN_HOUR = 0
        const val MAX_HOUR = 23
        const val MIN_MINUTE = 0
        const val MAX_MINUTE = 59
    }
}
