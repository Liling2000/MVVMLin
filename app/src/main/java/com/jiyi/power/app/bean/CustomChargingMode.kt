package com.jiyi.power.app.bean

data class PowerChannel(
    val name: String,
    val power: Int,
    val minPower: Int,
    val maxPower: Int,
    val discrete: Boolean = false,
)

data class CustomChargingMode(
    val id: Long,
    val name: String,
    val c1Power: Int,
    val c2Power: Int,
    val aPower: Int,
)
