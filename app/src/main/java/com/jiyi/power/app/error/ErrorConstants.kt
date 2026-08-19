package com.jiyi.power.app.error

import androidx.annotation.StringRes
import com.jiyi.power.R

object ErrorTitleConstant {
    const val BATTERY_OVERHEAT = 1
    const val VOLTAGE_UNSTABLE = 2
    const val SENSOR_ERROR = 3

    val map: Map<Int, Int> = mapOf(
        BATTERY_OVERHEAT to R.string.error_title_battery_overheat,
        VOLTAGE_UNSTABLE to R.string.error_title_voltage_unstable,
        SENSOR_ERROR to R.string.error_title_sensor_error,
    )
}

object ErrorDescriptionConstant {
    const val BATTERY_OVERHEAT_DESC = 101
    const val VOLTAGE_UNSTABLE_DESC = 102
    const val SENSOR_ERROR_DESC = 103

    val map: Map<Int, Int> = mapOf(
        BATTERY_OVERHEAT_DESC to R.string.error_desc_battery_overheat,
        VOLTAGE_UNSTABLE_DESC to R.string.error_desc_voltage_unstable,
        SENSOR_ERROR_DESC to R.string.error_desc_sensor_error,
    )
}

@StringRes
fun titleResource(type: Int): Int = ErrorTitleConstant.map[type] ?: R.string.error_title_unknown

@StringRes
fun descriptionResource(type: Int): Int =
    ErrorDescriptionConstant.map[type] ?: R.string.error_desc_unknown
