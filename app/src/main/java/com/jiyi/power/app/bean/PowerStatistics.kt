package com.jiyi.power.app.bean

data class ChartPoint(val x: Float, val y: Float)

data class PortPowerSample(
    val c1PowerW: Float,
    val c2PowerW: Float,
    val a1PowerW: Float,
) {
    val totalPowerW: Float get() = c1PowerW + c2PowerW + a1PowerW
}

enum class StatisticsPort { C1, C2, A1 }

data class PortPowerStatisticsData(
    val totalDataList: List<Float> = emptyList(),
    val c1DataList: List<Float> = emptyList(),
    val c2DataList: List<Float> = emptyList(),
    val a1DataList: List<Float> = emptyList(),
) {
    fun dataList(port: StatisticsPort): List<Float> = when (port) {
        StatisticsPort.C1 -> c1DataList
        StatisticsPort.C2 -> c2DataList
        StatisticsPort.A1 -> a1DataList
    }
}

data class PowerStatistics(
    val peakPower: Float,
    val averagePower: Float,
    val points: List<ChartPoint>,
    val maxY: Float,
    val yItemCount: Int
)

object PowerStatisticsCalculator {
    const val MAX_SAMPLE_COUNT = 60

    fun append(
        current: PortPowerStatisticsData,
        sample: PortPowerSample,
    ): PortPowerStatisticsData = current.copy(
        totalDataList = appendValue(current.totalDataList, sample.totalPowerW),
        c1DataList = appendValue(current.c1DataList, sample.c1PowerW),
        c2DataList = appendValue(current.c2DataList, sample.c2PowerW),
        a1DataList = appendValue(current.a1DataList, sample.a1PowerW),
    )

    fun calculate(values: List<Float>): PowerStatistics {
        val peak = values.maxOrNull() ?: 0f
        val average = values.takeIf { it.isNotEmpty() }?.average()?.toFloat() ?: 0f
        return PowerStatistics(
            peakPower = peak,
            averagePower = average,
            points = values.mapIndexed { index, value -> ChartPoint(index.toFloat(), value) },
            maxY = chartMaximum(peak),
            yItemCount = 5,
        )
    }

    private fun appendValue(current: List<Float>, value: Float): List<Float> =
        (current + value).takeLast(MAX_SAMPLE_COUNT)

    private fun chartMaximum(peak: Float): Float {
        val step = when {
            peak <= 40f -> 10f
            peak <= 100f -> 20f
            peak <= 200f -> 50f
            else -> 100f
        }
        return (kotlin.math.ceil(peak / step) * step).toFloat().coerceAtLeast(step)
    }
}
