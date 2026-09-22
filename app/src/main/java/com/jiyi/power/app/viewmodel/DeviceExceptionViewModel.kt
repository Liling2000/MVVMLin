package com.jiyi.power.app.viewmodel

import androidx.annotation.StringRes
import com.jiyi.power.R
import com.jiyi.power.app.bean.DeviceExceptionRecord
import com.jiyi.power.app.utils.DeviceExceptionLogReader
import com.jiyi.power.app.utils.DeviceProtectionProtocol
import com.jiyi.power.app.utils.MobilePowerProtocolManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DeviceExceptionUiState(
    val records: List<DeviceExceptionRecord> = emptyList(),
    val isLoading: Boolean = false,
    val disabled: Boolean? = null,
    val totalCount: Int? = null,
    @StringRes val messageRes: Int? = null,
)

class DeviceExceptionViewModel : DeviceCommandViewModel() {
    private val protocol = DeviceProtectionProtocol()
    private val reader = DeviceExceptionLogReader()
    private val _uiState = MutableStateFlow(DeviceExceptionUiState())
    val uiState = _uiState.asStateFlow()
    private var timeoutJob: Job? = null

    init {
        launch {
            commandEvents.collect { event ->
                if (event is CommandEvent.WriteFailed && event.functionCode == reader.expectedCode &&
                    _uiState.value.isLoading) fail(R.string.protection_query_failed)
            }
        }
    }

    fun refresh() {
        if (_uiState.value.isLoading) return
        timeoutJob?.cancel()
        protocol.reset()
        reader.begin()
        _uiState.value = DeviceExceptionUiState(isLoading = true)
        if (!isConnected()) {
            fail(R.string.protection_disconnected)
            return
        }
        requestNext()
    }

    private fun requestNext() {
        val code = reader.expectedCode ?: return
        // 定时从发送前开始；BLE 写成功不等于收到了日志数据。
        timeoutJob?.cancel()
        timeoutJob = launch {
            delay(5_000)
            if (_uiState.value.isLoading) fail(R.string.protection_query_timeout)
        }
        if (!sendDeviceCommand(code, MobilePowerProtocolManager.buildReadCommand(code))) {
            fail(R.string.protection_query_failed)
        }
    }

    override fun onBleDataReceive(data: String?) {
        if (!_uiState.value.isLoading) return
        for (frame in protocol.accept(data)) {
            if (!reader.accept(frame)) continue
            timeoutJob?.cancel()
            if (reader.failed) {
                fail(R.string.protection_log_incomplete)
                return
            }
            _uiState.value = DeviceExceptionUiState(
                records = reader.records,
                isLoading = !reader.complete,
                disabled = reader.disabled,
                totalCount = reader.totalCount,
                messageRes = if (reader.complete && reader.records.isEmpty()) R.string.protection_log_empty else null,
            )
            if (reader.complete) return
            requestNext()
        }
    }

    override fun onDeviceReconnected() = refresh()

    override fun onDeviceDisconnected() = fail(R.string.protection_disconnected)

    private fun fail(@StringRes message: Int) {
        timeoutJob?.cancel()
        reader.stop()
        protocol.reset()
        // 已收到的部分历史记录可以保留，但明确标记读取未完成。
        _uiState.value = _uiState.value.copy(isLoading = false, messageRes = message)
    }
}