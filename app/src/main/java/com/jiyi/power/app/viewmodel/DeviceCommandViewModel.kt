package com.jiyi.power.app.viewmodel

import com.aleyn.mvvm.base.BaseViewModel
import com.jiyi.power.app.bean.BleDeviceStore
import com.jiyi.power.app.ble.BleConnectionCoordinator
import com.jiyi.power.app.ble.BleIoEvent
import com.jiyi.power.app.ble.DeviceConnectionState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Shared transport for pages that operate on the currently opened power bank. */
open class DeviceCommandViewModel : BaseViewModel() {
    sealed interface CommandEvent {
        data class WriteSucceeded(val functionCode: String) : CommandEvent
        data class WriteFailed(val functionCode: String) : CommandEvent
        data object Disconnected : CommandEvent
        data object Reconnected : CommandEvent
    }

    private val _commandEvents = MutableSharedFlow<CommandEvent>(extraBufferCapacity = 16)
    val commandEvents = _commandEvents.asSharedFlow()
    protected var deviceSn: String? = null

    init {
        // ViewModel 统一接管 BLE 回调，页面只订阅 commandEvents，避免重复解析同一数据包。
        launch {
            BleConnectionCoordinator.ioEvents.collect { event ->
                when (event) {
                    is BleIoEvent.Notification -> if (matches(event.sn)) {
                        onBleDataReceive(event.data.joinToString("") { "%02X".format(it.toInt() and 0xFF) })
                    }
                    is BleIoEvent.WriteResult -> if (matches(event.sn)) {
                        // 写队列回调带回原始发送帧，其第 3 字节即为协议功能码。
                        event.data?.getOrNull(2)?.toInt()?.and(0xFF)?.let { code ->
                            _commandEvents.tryEmit(
                                if (event.success) CommandEvent.WriteSucceeded("%02X".format(code))
                                else CommandEvent.WriteFailed("%02X".format(code)),
                            )
                        }
                    }
                    else -> Unit
                }
            }
        }
        launch {
            BleConnectionCoordinator.connectionEvents.collect { event ->
                when (event) {
                    is com.jiyi.power.app.ble.BleConnectionEvent.Connected -> if (matches(event.device.bluetoothSn)) {
                        _commandEvents.tryEmit(CommandEvent.Reconnected)
                        onDeviceReconnected()
                    }
                    is com.jiyi.power.app.ble.BleConnectionEvent.Disconnected -> if (matches(event.sn)) {
                        _commandEvents.tryEmit(CommandEvent.Disconnected)
                    }
                }
            }
        }
    }

    /** 优先使用页面传入设备；仅在缺省时回退最近绑定设备。 */
    fun bindDevice(sn: String?) { deviceSn = sn ?: BleDeviceStore.getDevices().lastOrNull()?.bluetoothSn }

    protected open fun onDeviceReconnected() = Unit

    protected fun sendDeviceCommand(functionCode: String, command: String?): Boolean {
        val sn = deviceSn ?: BleDeviceStore.getDevices().lastOrNull()?.bluetoothSn ?: return false
        // 未连接时不进入写队列，确保 UI 不会误报“已设置”。
        if (command == null || !isConnected(sn)) return false
        deviceSn = sn
        sendCmdData(sn, command)
        return true
    }

    protected fun isConnected(sn: String = deviceSn.orEmpty()): Boolean =
        BleConnectionCoordinator.connectionStates.value.any { (savedSn, state) ->
            savedSn.equals(sn, ignoreCase = true) && state == DeviceConnectionState.CONNECTED
        }

    private fun matches(sn: String): Boolean =
        deviceSn?.equals(sn, ignoreCase = true) ?: false
}
