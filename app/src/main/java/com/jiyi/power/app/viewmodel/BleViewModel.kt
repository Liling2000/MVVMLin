package com.jiyi.power.app.viewmodel

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import android.content.Context
import androidx.lifecycle.viewModelScope
import com.aleyn.mvvm.base.BaseViewModel
import com.jiyi.power.R
import com.jiyi.power.app.bean.BleDeviceStore
import com.jiyi.power.app.bean.BleSavedDevice
import com.liling.ble.callback.BleScanDeviceCallBack
import com.liling.ble.constant.BleConstant
import com.liling.ble.listener.BleDataListener
import com.liling.ble.manager.Ble
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BleScanDevice(
    val name: String,
    val sn: String,
    val rssi: Int,
    val scanResult: ScanResult,
    val iconRes: Int = R.mipmap.ic_s_device
){
    override fun toString(): String {
        return "BleScanDevice(rssi=$rssi, sn='$sn', name='$name')"
    }
}

data class BleUiState(
    val isScanning: Boolean = false,
    val devices: List<BleScanDevice> = emptyList(),
    val connectingSn: String? = null
)

sealed interface BleConnectEvent {
    data class Success(val device: BleSavedDevice) : BleConnectEvent
    data class Failed(val message: String) : BleConnectEvent
}

/**
 * 管理 BLE 扫描、连接、连接状态和设备返回数据。
 */
class BleViewModel : BaseViewModel() {

    private val bleApi = Ble.getBleApi()
    private val _uiState = MutableStateFlow(BleUiState())
    val uiState: StateFlow<BleUiState> = _uiState

    private val _connectEvent = MutableSharedFlow<BleConnectEvent>()
    val connectEvent: SharedFlow<BleConnectEvent> = _connectEvent.asSharedFlow()

    private val _receivedData = MutableSharedFlow<Pair<String, ByteArray>>()
    val receivedData: SharedFlow<Pair<String, ByteArray>> = _receivedData.asSharedFlow()

    private var connectTimeoutJob: Job? = null
    private var listenersReady = false

    fun initBle(context: Context) {
        bleApi.init(context.applicationContext)
        setupBleListeners()
    }

    /**
     * 设置 BLE 扫描、连接和数据监听。
     */
    fun setupBleListeners() {
        if (listenersReady) return
        listenersReady = true
        bleApi.setOnBleScanResultCallBack(object : BleScanDeviceCallBack {
            override fun scanFinished(scanResults: MutableList<ScanResult>) {
                _uiState.update { it.copy(isScanning = false) }
            }

            override fun scanFirstDevice(scanResult: ScanResult) {
                addScanResult(scanResult)
            }

            override fun scanDevice(scanResult: ScanResult) {
                addScanResult(scanResult)
            }
        })
        bleApi.setOnBleDataListener(object : BleDataListener {
            override fun onBleNotify(device: BluetoothDevice, data: ByteArray) {
                viewModelScope.launch {
                    _receivedData.emit(device.address to data)
                }
            }

            override fun sendConnectState(device: BluetoothDevice, state: Int) {
                handleConnectState(device, state)
            }

            override fun sendWriteMsg(device: BluetoothDevice, state: Int) = Unit

            override fun mtuResponse(sn: String, state: Int, mtu: Int) = Unit

            override fun onRssiResponse(sn: String, model: String, rssi: Int) = Unit
        })
    }

    /**
     * 有扫描权限后开始扫描附近 BLE 设备。
     */
    fun startScan() {
        setupBleListeners()
        if (!bleApi.isBleOpen()) {
            bleApi.startBle()
        }
        _uiState.update { it.copy(isScanning = true, devices = emptyList()) }
        bleApi.scanDevice(true)
        bleApi.stopScanDelay(true)
    }

    fun stopScan() {
        bleApi.stopScanDelay(false)
        _uiState.update { it.copy(isScanning = false) }
    }

    fun connect(device: BleScanDevice) {
        stopScan()
        _uiState.update { it.copy(connectingSn = device.sn) }
        connectTimeoutJob?.cancel()
        connectTimeoutJob = viewModelScope.launch {
            delay(CONNECT_TIMEOUT)
            if (_uiState.value.connectingSn == device.sn) {
                bleApi.disconnectBle(device.sn, device.name)
                _uiState.update { it.copy(connectingSn = null) }
                _connectEvent.emit(BleConnectEvent.Failed("连接超时，请重试"))
            }
        }
        bleApi.connectNoQueue(device.scanResult.device, device.name)
    }

    private fun addScanResult(scanResult: ScanResult) {
        val device = scanResult.device ?: return
        val name = scanResult.scanRecord?.deviceName
            ?: device.name
            ?: return
        val item = BleScanDevice(
            name = name,
            sn = device.address,
            rssi = scanResult.rssi,
            scanResult = scanResult
        )
        _uiState.update { state ->
            val devices = state.devices.toMutableList()
            val oldIndex = devices.indexOfFirst { it.sn == item.sn }
            if (oldIndex >= 0) {
                devices[oldIndex] = item
            } else {
                devices.add(item)
            }
            state.copy(devices = devices)
        }
    }

    private fun handleConnectState(device: BluetoothDevice, state: Int) {
        when (state) {
            BleConstant.BleConnectState.stateConnected -> onConnectSuccess(device)
            BleConstant.BleConnectState.stateDisconnected -> onConnectFailed(device)
        }
    }

    private fun onConnectSuccess(device: BluetoothDevice) {
        connectTimeoutJob?.cancel()
        val savedDevice = BleSavedDevice(
            bluetoothName = device.name ?: _uiState.value.devices.firstOrNull {
                it.sn == device.address
            }?.name.orEmpty(),
            bluetoothSn = device.address,
            deviceIcon = R.mipmap.device_home_headphones_product
        )
        BleDeviceStore.saveDevice(savedDevice)
        _uiState.update { it.copy(connectingSn = null) }
        viewModelScope.launch {
            _connectEvent.emit(BleConnectEvent.Success(savedDevice))
        }
    }

    private fun onConnectFailed(device: BluetoothDevice) {
        if (_uiState.value.connectingSn != device.address) return
        connectTimeoutJob?.cancel()
        _uiState.update { it.copy(connectingSn = null) }
        viewModelScope.launch {
            _connectEvent.emit(BleConnectEvent.Failed("连接失败，请重试"))
        }
    }

    override fun onCleared() {
        connectTimeoutJob?.cancel()
        super.onCleared()
    }

    companion object {
        private const val CONNECT_TIMEOUT = 15_000L
    }
}
