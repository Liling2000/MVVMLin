package com.jiyi.power.ui.mobilepower.bean

/**
 * @date 2023/6/1
 * @description 可选时间类型
 * @param time 选择时间值
 * @param isChosen 是否选中
 */
data class StatisticsData(var cell: Int = 0, var voltage: Int = 0, var current: Int = 0, var repairNum: Int = -1)
