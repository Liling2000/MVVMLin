package com.jiyi.power.app.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import android.content.Context
import android.util.Log
import com.jiyi.power.app.bean.BleDeviceStore
import com.jiyi.power.app.bean.BleSavedDevice
import com.jiyi.power.app.utils.BlePermissionManager
import com.jiyi.power.app.viewmodel.BleScanDevice
import com.liling.ble.callback.BleScanDeviceCallBack
import com.liling.ble.constant.BleConstant
import com.liling.ble.listener.BleDataListener
import com.liling.ble.listener.BleWriteDataStatueListener
import com.liling.ble.manager.Ble
import com.liling.ble.utils.BleUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.logging.Logger

enum class DeviceConnectionState { DISCONNECTED, CONNECTING, CONNECTED }

sealed interface BleConnectionEvent {
    data class Connected(val device: BleSavedDevice, val userInitiated: Boolean) :
        BleConnectionEvent

    data class Disconnected(val sn: String) : BleConnectionEvent
}

sealed interface BleIoEvent {
    data class Notification(val sn: String, val data: ByteArray) : BleIoEvent
    data class WriteResult(val sn: String, val success: Boolean, val data: ByteArray?) : BleIoEvent
    data class MtuResult(val sn: String, val state: Int, val mtu: Int) : BleIoEvent
    data class RssiResult(val sn: String, val model: String, val rssi: Int) : BleIoEvent
}

/** Application 进程内唯一的 BLE 回调入口和扫描/重连调度器。 */
object BleConnectionCoordinator {
    private const val CONNECT_TIMEOUT_MS = 15_000L
    private const val SCAN_RESTART_DELAY_MS = 500L
    private const val CONNECT_RETRY_BASE_MS = 2_000L
    private const val CONNECT_RETRY_MAX_MS = 60_000L

    private val TAG = "BleConnectionCoordinator"
    private val bleApi = Ble.getBleApi()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val scanOwners = mutableSetOf<String>()
    private val retryCount = mutableMapOf<String, Int>()
    private val retryAfter = mutableMapOf<String, Long>()
    private val manuallyConnecting = mutableSetOf<String>()
    private val connectTimeoutJobs = mutableMapOf<String, Job>()

    private val _scanDevices = MutableStateFlow<List<BleScanDevice>>(emptyList())
    val scanDevices: StateFlow<List<BleScanDevice>> = _scanDevices.asStateFlow()

    private val _connectionStates = MutableStateFlow<Map<String, DeviceConnectionState>>(emptyMap())
    val connectionStates: StateFlow<Map<String, DeviceConnectionState>> =
        _connectionStates.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _connectionEvents = MutableSharedFlow<BleConnectionEvent>(extraBufferCapacity = 16)
    val connectionEvents: SharedFlow<BleConnectionEvent> = _connectionEvents.asSharedFlow()

    private val _ioEvents = MutableSharedFlow<BleIoEvent>(extraBufferCapacity = 64)
    val ioEvents: SharedFlow<BleIoEvent> = _ioEvents.asSharedFlow()

    private var initialized = false
    private var foreground = false
    private var autoReconnectEnabled = false
    private var scanRunning = false
    private var restartJob: Job? = null

    fun initialize(context: Context) {
        if (initialized) return
        initialized = true
        bleApi.init(context.applicationContext)
        bleApi.setOnBleScanResultCallBack(scanCallback)
        bleApi.setOnBleDataListener(dataListener)
        bleApi.setOnBleWriteDataListener(writeDataListener)
        refreshBoundDevices()
    }

    fun onAppForeground() {
        foreground = true
        refreshBoundDevices()
        reconcileScan()
    }

    fun onAppBackground() {
        foreground = false
        stopPhysicalScan()
    }

    /** 权限获取后或绑定列表变化后调用。没有已绑定设备时不会开启自动回连扫描。 */
    fun startAutoReconnect() {
        autoReconnectEnabled = BleDeviceStore.getDevices().isNotEmpty()
        refreshBoundDevices()
        reconcileScan()
    }

    fun refreshBoundDevices() {
        val saved = BleDeviceStore.getDevices()
        val old = _connectionStates.value
        _connectionStates.value = saved.associate { device ->
            device.bluetoothSn to (old[device.bluetoothSn]
                ?: if (BlePermissionManager.hasBluetoothPermissions() && bleApi.isBleConnected(
                        device.bluetoothSn
                    )
                ) DeviceConnectionState.CONNECTED else DeviceConnectionState.DISCONNECTED)
        }
        autoReconnectEnabled = autoReconnectEnabled && saved.isNotEmpty()
    }

    fun acquireScan(owner: String, clearPreviousResults: Boolean = false) {
        if (clearPreviousResults) _scanDevices.value = emptyList()
        scanOwners += owner
        reconcileScan()
    }

    fun releaseScan(owner: String) {
        scanOwners -= owner
        reconcileScan()
    }

    fun connect(device: BleScanDevice, userInitiated: Boolean = true) {
        val sn = device.sn
        if (_connectionStates.value[sn] == DeviceConnectionState.CONNECTED || _connectionStates.value[sn] == DeviceConnectionState.CONNECTING) return
        if (userInitiated) manuallyConnecting += sn
        setConnectionState(sn, DeviceConnectionState.CONNECTING)
        bleApi.connectWithQueue(device.scanResult.device, device.name)
    }

    fun write(sn: String, data: ByteArray, withQueue: Boolean = true) {
        if (withQueue) bleApi.writeDataWithQueue(data, sn) else bleApi.writeDataNoQueue(data, sn)
    }

    private fun reconcileScan() {
        val hasReconnectTarget = autoReconnectEnabled && _connectionStates.value.any {
            it.value != DeviceConnectionState.CONNECTED
        }
        val shouldScan =
            foreground && BlePermissionManager.hasBluetoothPermissions() && (scanOwners.isNotEmpty() || hasReconnectTarget)
        if (shouldScan) startPhysicalScan() else stopPhysicalScan()
    }

    private fun startPhysicalScan() {
        if (scanRunning) return
        if (!bleApi.isBleOpen()) {
            bleApi.startBle()
            scheduleRestart(1_000L)
            return
        }
        restartJob?.cancel()
        scanRunning = true
        _isScanning.value = true
        bleApi.scanDevice(true)
        // 底层一轮扫描默认 10 秒；结束回调后重新开始，形成前台持续扫描。
        bleApi.stopScanDelay(true)
    }

    private fun stopPhysicalScan() {
        restartJob?.cancel()
        restartJob = null
        if (scanRunning) bleApi.stopScanDelay(false)
        scanRunning = false
        _isScanning.value = false
    }

    private fun scheduleRestart(delayMs: Long = SCAN_RESTART_DELAY_MS) {
        restartJob?.cancel()
        restartJob = scope.launch {
            delay(delayMs)
            reconcileScan()
        }
    }

    private fun handleScanResult(result: ScanResult) {
        val device = result.device ?: return
        val name =
            result.scanRecord?.deviceName ?: runCatching { device.name }.getOrNull() ?: return
        val item = BleScanDevice(name, device.address, result.rssi, result)
        _scanDevices.update { current ->
            current.toMutableList().apply {
                val index = indexOfFirst { it.sn.equals(item.sn, ignoreCase = true) }
                if (index >= 0) this[index] = item else add(item)
            }
        }

        val saved = BleDeviceStore.getDevices().firstOrNull {
            it.bluetoothSn.equals(item.sn, ignoreCase = true)
        } ?: return
        if (!autoReconnectEnabled || !foreground) return
        val state = _connectionStates.value[saved.bluetoothSn]
        if (state == DeviceConnectionState.CONNECTED || state == DeviceConnectionState.CONNECTING) return
        if (System.currentTimeMillis() < (retryAfter[saved.bluetoothSn] ?: 0L)) return
        connect(item.copy(sn = saved.bluetoothSn), userInitiated = false)
    }

    private fun setConnectionState(sn: String, state: DeviceConnectionState) {
        _connectionStates.update { it + (sn to state) }
    }

    private fun handleConnectFailure(sn: String) {
        connectTimeoutJobs.remove(sn)?.cancel()
        if (_connectionStates.value[sn] == DeviceConnectionState.DISCONNECTED) return
        setConnectionState(sn, DeviceConnectionState.DISCONNECTED)
        manuallyConnecting.remove(sn)
        val attempts = (retryCount[sn] ?: 0) + 1
        retryCount[sn] = attempts
        val delayMs = (CONNECT_RETRY_BASE_MS * (1L shl minOf(attempts - 1, 5)))
            .coerceAtMost(CONNECT_RETRY_MAX_MS)
        retryAfter[sn] = System.currentTimeMillis() + delayMs
        _connectionEvents.tryEmit(BleConnectionEvent.Disconnected(sn))
        reconcileScan()
    }

    private fun startConnectTimeout(sn: String, model: String) {
        connectTimeoutJobs.remove(sn)?.cancel()
        connectTimeoutJobs[sn] = scope.launch {
            delay(CONNECT_TIMEOUT_MS)
            if (_connectionStates.value[sn] != DeviceConnectionState.CONNECTING) return@launch
            bleApi.disconnectBle(sn, model)
            handleConnectFailure(sn)
        }
    }

    private val scanCallback = object : BleScanDeviceCallBack {
        override fun scanFinished(scanResults: MutableList<ScanResult>) {
            scanRunning = false
            _isScanning.value = false
            scheduleRestart()
        }

        override fun scanFirstDevice(scanResult: ScanResult) = handleScanResult(scanResult)
        override fun scanDevice(scanResult: ScanResult) = handleScanResult(scanResult)
    }

    private val dataListener = object : BleDataListener {
        override fun onBleNotify(device: BluetoothDevice, data: ByteArray) {
            Log.d(TAG, "onBleNotify, sn = ${device.address}, data = ${BleUtils.byteToString(data)}")
            _ioEvents.tryEmit(BleIoEvent.Notification(device.address, data.copyOf()))
        }

        override fun sendConnectState(device: BluetoothDevice, state: Int) {
            val sn = device.address
            Log.d(TAG,"sendConnectState, sn = $sn, state = $state")
            when (state) {
                BleConstant.BleConnectState.stateConnecting -> {
                    setConnectionState(sn, DeviceConnectionState.CONNECTING)
                    val model = BleDeviceStore.getDevices().firstOrNull {
                        it.bluetoothSn.equals(sn, ignoreCase = true)
                    }?.bluetoothName ?: runCatching { device.name }.getOrNull().orEmpty()
                    startConnectTimeout(sn, model)
                }

                BleConstant.BleConnectState.stateConnected -> {
                    connectTimeoutJobs.remove(sn)?.cancel()
                    setConnectionState(sn, DeviceConnectionState.CONNECTED)
                    retryCount.remove(sn)
                    retryAfter.remove(sn)
                    val userInitiated = manuallyConnecting.remove(sn)
                    val existing = BleDeviceStore.getDevices().firstOrNull {
                        it.bluetoothSn.equals(sn, ignoreCase = true)
                    }
                    val saved = existing ?: BleSavedDevice(
                        bluetoothName = runCatching { device.name }.getOrNull().orEmpty(),
                        bluetoothSn = sn,
                    )
                    if (userInitiated && existing == null) {
                        BleDeviceStore.saveDevice(saved)
                        autoReconnectEnabled = true
                        refreshBoundDevices()
                    }
                    _connectionEvents.tryEmit(BleConnectionEvent.Connected(saved, userInitiated))
                    reconcileScan()
                }

                BleConstant.BleConnectState.stateDisconnected -> {
                    handleConnectFailure(sn)
                }
            }
        }

        override fun sendWriteMsg(device: BluetoothDevice, state: Int) = Unit

        override fun mtuResponse(sn: String, state: Int, mtu: Int) {
            _ioEvents.tryEmit(BleIoEvent.MtuResult(sn, state, mtu))
        }

        override fun onRssiResponse(sn: String, model: String, rssi: Int) {
            _ioEvents.tryEmit(BleIoEvent.RssiResult(sn, model, rssi))
        }
    }

    private val writeDataListener = BleWriteDataStatueListener { sn, success, data ->
        _ioEvents.tryEmit(BleIoEvent.WriteResult(sn, success, data?.copyOf()))
    }
}
