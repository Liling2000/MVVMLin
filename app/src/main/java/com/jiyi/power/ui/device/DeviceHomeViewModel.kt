package com.jiyi.power.ui.device

import androidx.annotation.DrawableRes
import com.aleyn.mvvm.base.BaseViewModel
import com.jiyi.power.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class DeviceHomeUiState(
    val deviceName: String = "Inspire XH1",
    val batteryText: String = "90%",
    val connectionText: String = "蓝牙已连接 · 设备双连",
    val selectedNoiseMode: NoiseMode = NoiseMode.NORMAL,
    val dolbyEnabled: Boolean = true,
    val features: List<DeviceFeature> = DeviceFeatureCatalog.featuresFor("Inspire XH1")
)

data class DeviceFeature(
    val type: DeviceFeatureType,
    val title: String,
    @DrawableRes val iconRes: Int
)

enum class NoiseMode(val title: String) {
    NORMAL("正常"),
    TRANSPARENCY("通透"),
    ANC("降噪")
}

enum class DeviceFeatureType(
    val title: String,
    @DrawableRes val iconRes: Int
) {
    GESTURE_SETTINGS("手势设置", R.mipmap.device_home_gesture_settings),
    DUAL_CONNECTION("设备双连", R.mipmap.device_home_dual_connection),
    SLEEP_MODE("助眠模式", R.mipmap.device_home_sleep_mode),
    INSPIRE_CARE_PLUS("Inspire care+", R.mipmap.device_home_inspire_care_plus)
}

object DeviceFeatureCatalog {

    private val defaultFeatures = listOf(
        DeviceFeatureType.GESTURE_SETTINGS,
        DeviceFeatureType.DUAL_CONNECTION,
        DeviceFeatureType.SLEEP_MODE,
        DeviceFeatureType.INSPIRE_CARE_PLUS
    )

    private val deviceFeatures = mapOf(
        "Inspire XH1" to defaultFeatures,
        "Inspire XH1 Lite" to listOf(
            DeviceFeatureType.GESTURE_SETTINGS,
            DeviceFeatureType.DUAL_CONNECTION
        ),
        "Inspire Sleep" to listOf(
            DeviceFeatureType.SLEEP_MODE,
            DeviceFeatureType.GESTURE_SETTINGS
        )
    )

    fun featuresFor(deviceName: String): List<DeviceFeature> {
        return (deviceFeatures[deviceName] ?: defaultFeatures).map {
            DeviceFeature(type = it, title = it.title, iconRes = it.iconRes)
        }
    }
}

class DeviceHomeViewModel : BaseViewModel() {

    private val _uiState = MutableStateFlow(DeviceHomeUiState())
    val uiState: StateFlow<DeviceHomeUiState> = _uiState

    fun loadDevice(deviceName: String) {
        _uiState.update {
            it.copy(
                deviceName = deviceName,
                features = DeviceFeatureCatalog.featuresFor(deviceName)
            )
        }
    }

    fun selectNoiseMode(mode: NoiseMode) {
        _uiState.update { it.copy(selectedNoiseMode = mode) }
    }

    fun setDolbyEnabled(enabled: Boolean) {
        _uiState.update { it.copy(dolbyEnabled = enabled) }
    }
}