package com.jiyi.power.app.viewmodel

import androidx.lifecycle.ViewModel
import com.jiyi.power.app.bean.TimerOption
import com.jiyi.power.app.bean.TimerSettingType
import com.jiyi.power.app.bean.TimerSettingUiData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class TimerSettingViewModel : ViewModel() {
    private val deviceCommands = MainFragmentViewModel()
    private val _uiState = MutableStateFlow(TimerSettingUiData())
    val uiState = _uiState.asStateFlow()

    fun initialize(type: TimerSettingType) {
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
        val value = state.selectedTime.toString()
        return when (state.type) {
            TimerSettingType.SHUTDOWN -> deviceCommands.setCountdownOff(value)
            TimerSettingType.REMINDER -> deviceCommands.setCountdownReminder(value)
        }
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
