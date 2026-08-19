package com.jiyi.power.app.bean

import androidx.annotation.DrawableRes
import com.aleyn.mvvm.utils.MmkvManager
import com.jiyi.power.R

data class BleSavedDevice(
    val bluetoothName: String,
    val bluetoothSn: String,
    @DrawableRes val deviceIcon: Int = R.mipmap.device_home_headphones_product
)

object BleDeviceStore {
    private const val KEY_DEVICE_LIST = "key_ble_device_list"

    fun getDevices(): MutableList<BleSavedDevice> {
        return MmkvManager.getList(KEY_DEVICE_LIST)
    }

    fun saveDevice(device: BleSavedDevice) {
        val devices = getDevices()
        val oldIndex = devices.indexOfFirst { it.bluetoothSn == device.bluetoothSn }
        if (oldIndex >= 0) {
            devices[oldIndex] = device
        } else {
            devices.add(device)
        }
        MmkvManager.putList(KEY_DEVICE_LIST, devices)
    }

    fun removeDevice(bluetoothSn: String?) {
        val devices = getDevices()
        if (bluetoothSn.isNullOrBlank()) devices.clear() else devices.removeAll { it.bluetoothSn == bluetoothSn }
        MmkvManager.putList(KEY_DEVICE_LIST, devices)
    }
}
