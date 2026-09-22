package com.jiyi.power.app.error

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.jiyi.power.R
import com.jiyi.power.app.bean.DeviceExceptionRecord

@StringRes
fun protectionTitleResource(type: Int): Int = when (type) {
    0x01 -> R.string.protection_overvoltage
    0x02 -> R.string.protection_temperature
    0x03 -> R.string.protection_undervoltage
    0x04 -> R.string.protection_overvoltage_disabled
    0x05 -> R.string.protection_short_circuit
    0x06 -> R.string.protection_port_misuse
    0x07 -> R.string.protection_charge_temperature
    0x08 -> R.string.protection_discharge_temperature
    0x09 -> R.string.protection_overcurrent
    else -> R.string.protection_unknown_type
}

@DrawableRes
fun protectionIconResource(type: Int): Int = when (type) {
    0x02, 0x07, 0x08 -> R.mipmap.ic_temperature_control
    0x09 -> R.mipmap.ic_common_lightning_slanted
    0x05, 0x06 -> R.mipmap.ic_power_cable
    0x01, 0x03, 0x04 -> R.mipmap.ic_mobile_device_filled
    else -> R.mipmap.ic_warning_outline
}

fun protectionLabel(context: Context, item: DeviceExceptionRecord): String =
    context.getString(protectionTitleResource(item.typeCode))

fun protectionDetail(context: Context, item: DeviceExceptionRecord): String = buildString {
    append(context.getString(R.string.protection_log_history_notice))
    append("\n\n")
    append(context.getString(R.string.protection_log_type, "%02X".format(item.typeCode)))
    item.batteryNumber?.let {
        append("\n")
        append(context.getString(R.string.protection_log_battery, it))
    }
    append("\n")
    append(context.getString(R.string.protection_log_parameter, item.parameterHex))
    append("\n")
    append(context.getString(R.string.protection_log_timestamp, item.timestampHex))
    append("\n\n")
    append(context.getString(R.string.protection_log_raw_notice))
}