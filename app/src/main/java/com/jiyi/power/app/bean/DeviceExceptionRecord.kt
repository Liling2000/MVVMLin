package com.jiyi.power.app.bean

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class DeviceExceptionRecord(
    val id: Long,
    val titleType: Int,
    val descriptionType: Int,
    val time: String,
    @StringRes val statusRes: Int,
    @DrawableRes val iconRes: Int,
)
