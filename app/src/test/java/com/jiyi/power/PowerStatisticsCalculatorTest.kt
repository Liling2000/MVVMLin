package com.jiyi.power

import com.jiyi.power.app.bean.PortPowerSample
import com.jiyi.power.app.bean.PowerStatisticsCalculator
import com.jiyi.power.app.bean.PortPowerStatisticsData
import com.jiyi.power.app.bean.StatisticsPort
import org.junit.Assert.assertEquals
import org.junit.Test

class PowerStatisticsCalculatorTest {
    @Test
    fun `history keeps latest sixty samples`() {
        val history = (0 until 65).fold(PortPowerStatisticsData()) { samples, value ->
            PowerStatisticsCalculator.append(samples, PortPowerSample(value.toFloat(), 0f, 0f))
        }

        assertEquals(60, history.totalDataList.size)
        assertEquals(60, history.c1DataList.size)
        assertEquals(5f, history.c1DataList.first())
        assertEquals(64f, history.c1DataList.last())
    }

    @Test
    fun `total statistics sum each sample before calculating peak and average`() {
        val data = listOf(
            PortPowerSample(10f, 20f, 5f),
            PortPowerSample(30f, 15f, 10f),
        ).fold(PortPowerStatisticsData(), PowerStatisticsCalculator::append)
        val result = PowerStatisticsCalculator.calculate(data.totalDataList)

        assertEquals(55f, result.peakPower)
        assertEquals(45f, result.averagePower)
        assertEquals(listOf(35f, 55f), result.points.map { it.y })
    }

    @Test
    fun `port statistics only use selected port`() {
        val data = listOf(
            PortPowerSample(10f, 20f, 5f),
            PortPowerSample(30f, 40f, 15f),
        ).fold(PortPowerStatisticsData(), PowerStatisticsCalculator::append)

        val result = PowerStatisticsCalculator.calculate(data.dataList(StatisticsPort.A1))

        assertEquals(15f, result.peakPower)
        assertEquals(10f, result.averagePower)
        assertEquals(listOf(5f, 15f), result.points.map { it.y })
    }

    @Test
    fun `each port keeps an independent data list`() {
        val data = PowerStatisticsCalculator.append(
            PortPowerStatisticsData(),
            PortPowerSample(c1PowerW = 11f, c2PowerW = 22f, a1PowerW = 3f),
        )

        assertEquals(listOf(36f), data.totalDataList)
        assertEquals(listOf(11f), data.c1DataList)
        assertEquals(listOf(22f), data.c2DataList)
        assertEquals(listOf(3f), data.a1DataList)
    }
}
