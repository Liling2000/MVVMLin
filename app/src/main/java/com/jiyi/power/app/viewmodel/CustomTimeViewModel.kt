package com.jiyi.power.app.viewmodel

import androidx.lifecycle.ViewModel
import com.jiyi.power.app.bean.CustomTimeUiState
import com.jiyi.power.app.bean.TimerSettingType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class CustomTimeViewModel : ViewModel() {
    private val deviceCommands = MainFragmentViewModel()
    private val _uiState = MutableStateFlow(CustomTimeUiState())
    val uiState = _uiState.asStateFlow()
    private var type = TimerSettingType.SHUTDOWN

    fun initialize(settingType: TimerSettingType, initialMinutes: Int) {
        type = settingType
        updateTotalMinutes(initialMinutes)
    }

    fun updateHour(hour: Int) = updateTime(hour, _uiState.value.minute)
    fun updateMinute(minute: Int) = updateTime(_uiState.value.hour, minute)
    fun addMinutes(minutes: Int) = updateTotalMinutes(_uiState.value.totalMinutes + minutes)
    fun reset() = updateTime(0, 0)

    fun confirm(): Boolean = when (type) {
        TimerSettingType.SHUTDOWN -> deviceCommands.setCountdownOff(_uiState.value.totalMinutes.toString())
        TimerSettingType.REMINDER -> deviceCommands.setCountdownReminder(_uiState.value.totalMinutes.toString())
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
