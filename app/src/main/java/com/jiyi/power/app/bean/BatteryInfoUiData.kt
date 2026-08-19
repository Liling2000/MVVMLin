package com.jiyi.power.app.bean

data class BatteryCellInfo(val index: Int, val voltageMv: Int?)

data class BatteryInfoUiData(
    val healthPercent: Int? = null,
    val manufacturer: String? = null,
    val model: String? = null,
    val cycleCount: Int? = null,
    val recommendedYears: Int? = null,
    val batterySeries: String? = null,
    val cells: List<BatteryCellInfo> = emptyList(),
    val ratedPowerW: Int? = null,
    val maxChargePowerW: Int? = null,
    val maxDischargePowerW: Int? = null,
    val totalDischargeHours: Long? = null,
    val totalDischargeCapacityMah: Long? = null
)
