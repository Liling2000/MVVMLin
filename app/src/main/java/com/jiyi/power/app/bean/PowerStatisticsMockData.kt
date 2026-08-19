package com.jiyi.power.app.bean

object PowerStatisticsMockData {
    val xLabels = listOf("00:00", "06:00", "12:00", "18:00", "24:00")

    val total = PowerStatistics(242.5f, 118.0f, points(12f, 135f, 165f, 242.5f, 220f, 145f, 92f), 400f, 5)

    val ports = linkedMapOf(
        "C1" to PowerStatistics(120f, 55f, points(0f, 50f, 45f, 120f, 108f, 50f, 30f), 140f, 8),
        "C2" to PowerStatistics(96f, 42.5f, points(8f, 35f, 62f, 96f, 82f, 38f, 18f), 120f, 7),
        "C3" to PowerStatistics(65f, 31.8f, points(5f, 22f, 48f, 65f, 58f, 28f, 12f), 80f, 5),
        "A1" to PowerStatistics(24f, 12.6f, points(2f, 9f, 18f, 24f, 20f, 11f, 6f), 40f, 5)
    )

    private fun points(vararg values: Float): List<ChartPoint> = values.mapIndexed { index, value ->
        ChartPoint(index * 4f, value)
    }
}
