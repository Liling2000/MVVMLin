package com.jiyi.power.app.repository

import com.jiyi.power.R
import com.jiyi.power.app.bean.DeviceExceptionRecord
import com.jiyi.power.app.error.ErrorDescriptionConstant
import com.jiyi.power.app.error.ErrorTitleConstant

class DeviceExceptionRepository {
    fun getRecords(): List<DeviceExceptionRecord> = listOf(
        DeviceExceptionRecord(
            id = 1,
            titleType = ErrorTitleConstant.BATTERY_OVERHEAT,
            descriptionType = ErrorDescriptionConstant.BATTERY_OVERHEAT_DESC,
            time = "2024-05-20 14:30",
            statusRes = R.string.error_status_auto_protection,
            iconRes = R.mipmap.ic_temperature,
        ),
        DeviceExceptionRecord(
            id = 2,
            titleType = ErrorTitleConstant.VOLTAGE_UNSTABLE,
            descriptionType = ErrorDescriptionConstant.VOLTAGE_UNSTABLE_DESC,
            time = "2024-05-19 09:12",
            statusRes = R.string.error_status_cooling,
            iconRes = R.mipmap.ic_warning_outline,
        ),
    )
}
