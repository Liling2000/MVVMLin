package com.jiyi.power.ui.mobilepower.bean

import java.io.Serializable

/**
 * 电芯的电压/电流bean
 * @property cellNum Int
 * @property value String
 * @constructor
 */
data class CellStatueBean(var cellNum: Int = 0, var value: Float = 0F): Serializable
