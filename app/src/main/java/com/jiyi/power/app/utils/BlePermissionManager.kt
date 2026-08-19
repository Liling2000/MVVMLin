package com.jiyi.power.app.utils

import android.Manifest
import android.app.Activity
import android.os.Build
import com.blankj.utilcode.util.PermissionUtils

/**
 * 蓝牙权限申请结果。
 */
sealed interface BlePermissionResult {
    data object Granted : BlePermissionResult

    data class Denied(
        val deniedPermissions: List<String>
    ) : BlePermissionResult

    data class PermanentlyDenied(
        val deniedPermissions: List<String>
    ) : BlePermissionResult
}

/**
 * 统一管理 BLE 扫描和连接所需的动态权限。
 */
object BlePermissionManager {

    private val pendingCallbacks = mutableListOf<(BlePermissionResult) -> Unit>()
    private var isRequesting = false

    /**
     * 获取当前系统版本需要动态申请的蓝牙相关权限。
     */
    fun getRequiredPermissions(): List<String> {
        return when {
            isAndroid12OrAbove() -> listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )

            isAndroid6OrAbove() -> listOf(Manifest.permission.ACCESS_FINE_LOCATION)

            else -> emptyList()
        }
    }

    /**
     * 判断当前系统版本实际需要的权限是否已全部授予。
     */
    fun hasBluetoothPermissions(): Boolean {
        val permissions = getRequiredPermissions()
        return permissions.isEmpty() || PermissionUtils.isGranted(*permissions.toTypedArray())
    }

    /**
     * 申请 BLE 扫描和连接所需权限；已有权限时直接返回 Granted。
     */
    fun requestBluetoothPermissions(
        activity: Activity,
        callback: (BlePermissionResult) -> Unit
    ) {
        if (activity.isFinishing) {
            callback(BlePermissionResult.Denied(getRequiredPermissions()))
            return
        }

        val permissions = getRequiredPermissions()
        if (permissions.isEmpty() || hasBluetoothPermissions()) {
            callback(BlePermissionResult.Granted)
            return
        }

        synchronized(this) {
            pendingCallbacks.add(callback)
            if (isRequesting) {
                return
            }
            isRequesting = true
        }

        PermissionUtils.permission(*permissions.toTypedArray())
            .callback(object : PermissionUtils.FullCallback {
                override fun onGranted(granted: MutableList<String>) {
                    notifyResult(BlePermissionResult.Granted)
                }

                override fun onDenied(
                    forever: MutableList<String>,
                    denied: MutableList<String>
                ) {
                    val result = if (forever.isNotEmpty()) {
                        BlePermissionResult.PermanentlyDenied(
                            (forever + denied).distinct()
                        )
                    } else {
                        BlePermissionResult.Denied(denied.distinct())
                    }
                    notifyResult(result)
                }
            })
            .request()
    }

    /**
     * 打开应用权限设置页，永久拒绝后由页面按需调用。
     */
    fun openAppSettings() {
        PermissionUtils.launchAppDetailsSettings()
    }

    /**
     * Android 12 及以上使用蓝牙扫描和连接动态权限，不再申请定位权限。
     */
    private fun isAndroid12OrAbove(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }

    /**
     * Android 6 至 Android 11 需要为 BLE 扫描动态申请精确定位权限。
     */
    private fun isAndroid6OrAbove(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    }

    private fun notifyResult(result: BlePermissionResult) {
        val callbacks = synchronized(this) {
            isRequesting = false
            pendingCallbacks.toList().also {
                pendingCallbacks.clear()
            }
        }
        callbacks.forEach { it(result) }
    }
}
