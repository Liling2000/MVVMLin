package com.jiyi.power.app.viewmodel

import androidx.lifecycle.ViewModel
import com.jiyi.power.app.bean.DeviceExceptionRecord
import com.jiyi.power.app.repository.DeviceExceptionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DeviceExceptionUiState(
    val records: List<DeviceExceptionRecord> = emptyList(),
)

class DeviceExceptionViewModel(
    private val repository: DeviceExceptionRepository = DeviceExceptionRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(DeviceExceptionUiState())
    val uiState = _uiState.asStateFlow()

    fun loadRecords() {
        _uiState.value = DeviceExceptionUiState(repository.getRecords())
    }
}
