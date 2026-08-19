package com.jiyi.power.app.bean

data class ChartPoint(val x: Float, val y: Float)

data class PowerStatistics(
    val peakPower: Float,
    val averagePower: Float,
    val points: List<ChartPoint>,
    val maxY: Float,
    val yItemCount: Int
)
