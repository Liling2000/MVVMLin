package com.jiyi.power.app.viewmodel

import android.bluetooth.le.ScanResult
import android.content.Context
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.aleyn.mvvm.base.BaseViewModel
import com.jiyi.power.R
import com.jiyi.power.app.bean.BleSavedDevice
import com.jiyi.power.app.ble.BleConnectionCoordinator
import com.jiyi.power.app.ble.BleConnectionEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class BleScanDevice(
    val name: String,
    val sn: String,
    val rssi: Int,
    val scanResult: ScanResult,
    val iconRes: Int = R.mipmap.ic_s_device
)

data class BleUiState(
    val isScanning: Boolean = false,
    val devices: List<BleScanDevice> = emptyList(),
    val connectingSn: String? = null
)

sealed interface BleConnectEvent {
    data class Success(val device: BleSavedDevice) : BleConnectEvent
    data class Failed(val message: String) : BleConnectEvent
}

/** 扫描页面的 UI 状态适配层；底层 BLE 回调只由 Coordinator 持有。 */
class BleViewModel : BaseViewModel() {
    private val owner = "scan-page-${System.identityHashCode(this)}"
    private val _uiState = MutableStateFlow(BleUiState())
    val uiState: StateFlow<BleUiState> = _uiState

    private val _connectEvent = MutableSharedFlow<BleConnectEvent>()
    val connectEvent: SharedFlow<BleConnectEvent> = _connectEvent.asSharedFlow()
    private var pendingConnectSn: String? = null

    init {
        viewModelScope.launch {
            combine(
                BleConnectionCoordinator.scanDevices,
                BleConnectionCoordinator.isScanning,
                BleConnectionCoordinator.connectionStates,
            ) { devices, scanning, states ->
                BleUiState(
                    isScanning = scanning,
                    devices = devices,
                    connectingSn = states.entries.firstOrNull {
                        it.value == com.jiyi.power.app.ble.DeviceConnectionState.CONNECTING
                    }?.key,
                )
            }.collect { _uiState.value = it }
        }
        viewModelScope.launch {
            BleConnectionCoordinator.connectionEvents.collect { event ->
                if (event is BleConnectionEvent.Connected && event.userInitiated) {
                    pendingConnectSn = null
                    _connectEvent.emit(BleConnectEvent.Success(event.device))
                } else if (event is BleConnectionEvent.Disconnected &&
                    event.sn.equals(pendingConnectSn, ignoreCase = true)
                ) {
                    pendingConnectSn = null
                    _connectEvent.emit(BleConnectEvent.Failed("连接超时或连接失败，请重试"))
                }
            }
        }
    }

    fun initBle(context: Context) {
        BleConnectionCoordinator.initialize(context.applicationContext)
    }

    fun startScan() {
        BleConnectionCoordinator.acquireScan(owner, clearPreviousResults = true)
    }

    fun stopScan() {
        BleConnectionCoordinator.releaseScan(owner)
    }

    fun connect(device: BleScanDevice) {
        Log.e("LLK", "connect 1")
        pendingConnectSn = device.sn
        BleConnectionCoordinator.connect(device, userInitiated = true)
    }

    override fun onCleared() {
        stopScan()
        super.onCleared()
    }
}
