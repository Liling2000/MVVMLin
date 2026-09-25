package com.jiyi.power

import com.jiyi.power.app.common.MobilePowerConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class MobilePowerConfigTest {
    @Test
    fun ratedCapacityParserAcceptsNumberWithOptionalUnit() {
        assertEquals(30_000, MobilePowerConfig.parseRatedCapacityMah("30000"))
        assertEquals(30_000, MobilePowerConfig.parseRatedCapacityMah(" 30000 "))
        assertEquals(30_000, MobilePowerConfig.parseRatedCapacityMah("30000 mAh"))
        assertEquals(null, MobilePowerConfig.parseRatedCapacityMah("0"))
        assertEquals(null, MobilePowerConfig.parseRatedCapacityMah("invalid"))
    }

    @Test
    fun currentCapacityUsesDeviceReportedRatedCapacity() {
        assertEquals(0, MobilePowerConfig.currentCapacityMah(0, 30_000))
        assertEquals(15_000, MobilePowerConfig.currentCapacityMah(50, 30_000))
        assertEquals(30_000, MobilePowerConfig.currentCapacityMah(100, 30_000))
    }

    @Test
    fun missingOrInvalidRatedCapacityUsesDefaultValue() {
        assertEquals(12_500, MobilePowerConfig.currentCapacityMah(50, null))
        assertEquals(12_500, MobilePowerConfig.currentCapacityMah(50, 0))
        assertEquals(12_500, MobilePowerConfig.currentCapacityMah(50, -1))
    }

    @Test
    fun batteryPercentIsLimitedToValidRange() {
        assertEquals(0, MobilePowerConfig.currentCapacityMah(-1, 30_000))
        assertEquals(30_000, MobilePowerConfig.currentCapacityMah(101, 30_000))
    }
}
