package com.jiyi.power.app.common

/** Product-level parameters for the mobile power device. */
object MobilePowerConfig {
    const val BLUETOOTH_DEVICE_NAME = "ELAU"
    const val DEFAULT_RATED_CAPACITY_MAH = 25_000
    const val BATTERY_MANUFACTURER = "SUNPOWER"
    const val BATTERY_MODEL = "INR21700-5000"
    const val BATTERY_RECOMMENDED_YEARS = 5
    const val BATTERY_RATED_POWER_W = 300
    const val BATTERY_MAX_CHARGE_POWER_W = 150
    const val BATTERY_MAX_DISCHARGE_POWER_W = 300
    private val capacityUnitSuffix = Regex("\\s*mAh\\s*$", RegexOption.IGNORE_CASE)

    fun parseRatedCapacityMah(value: String?): Int? = value
        ?.trim()
        ?.replace(capacityUnitSuffix, "")
        ?.trim()
        ?.toIntOrNull()
        ?.takeIf { it > 0 }

    fun currentCapacityMah(batteryPercent: Int, reportedRatedCapacityMah: Int?): Int {
        val ratedCapacityMah = reportedRatedCapacityMah
            ?.takeIf { it > 0 }
            ?: DEFAULT_RATED_CAPACITY_MAH
        return ratedCapacityMah * batteryPercent.coerceIn(0, 100) / 100
    }
}
