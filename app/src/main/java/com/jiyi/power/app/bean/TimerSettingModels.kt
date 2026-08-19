package com.jiyi.power.app.bean

enum class TimerSettingType {
    SHUTDOWN,
    REMINDER,
}

data class TimerOption(
    val name: String,
    val time: Int,
    val selected: Boolean = false,
    val isCustom: Boolean = false,
)

data class TimerSettingUiData(
    val type: TimerSettingType = TimerSettingType.SHUTDOWN,
    val options: List<TimerOption> = emptyList(),
    val selectedTime: Int = 30,
)

data class TimeText(val number: String, val unit: TimeUnit)

enum class TimeUnit { MINUTE, HOUR }

fun formatTime(minutes: Int): TimeText =
    if (minutes >= 120 && minutes % 60 == 0) {
        TimeText((minutes / 60).toString(), TimeUnit.HOUR)
    } else {
        TimeText(minutes.toString(), TimeUnit.MINUTE)
    }
