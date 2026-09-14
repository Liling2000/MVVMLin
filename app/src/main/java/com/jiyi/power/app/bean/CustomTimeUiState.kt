package com.jiyi.power.app.bean

data class CustomTimeUiState(
    val hour: Int = 0,
    val minute: Int = 0,
) {
    val totalMinutes: Int get() = hour * 60 + minute
}
