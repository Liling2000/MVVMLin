package com.jiyi.power.app.bean

import androidx.annotation.DrawableRes
import com.aleyn.mvvm.utils.MmkvManager
import com.jiyi.power.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BleSavedDevice(
    val bluetoothName: String,
    val bluetoothSn: String,
    @DrawableRes val deviceIcon: Int = R.mipmap.ic_s_device
)

object BleDeviceStore {
    private const val KEY_DEVICE_LIST = "key_ble_device_list"

    private val _devices = MutableStateFlow<List<BleSavedDevice>>(readDevices())
    val devices: StateFlow<List<BleSavedDevice>> = _devices.asStateFlow()

    fun getDevices(): MutableList<BleSavedDevice> {
        return _devices.value.toMutableList()
    }

    fun saveDevice(device: BleSavedDevice) {
        val devices = getDevices()
        val oldIndex = devices.indexOfFirst { it.bluetoothSn == device.bluetoothSn }
        if (oldIndex >= 0) {
            devices[oldIndex] = device
        } else {
            devices.add(device)
        }
        updateDevices(devices)
    }

    fun removeDevice(bluetoothSn: String?) {
        val devices = getDevices()
        if (bluetoothSn.isNullOrBlank()) devices.clear() else devices.removeAll { it.bluetoothSn == bluetoothSn }
        updateDevices(devices)
    }

    private fun updateDevices(devices: List<BleSavedDevice>) {
        val snapshot = devices.toList()
        MmkvManager.putList(KEY_DEVICE_LIST, snapshot)
        _devices.value = snapshot
    }

    private fun readDevices(): List<BleSavedDevice> = MmkvManager.getList(KEY_DEVICE_LIST)
}
