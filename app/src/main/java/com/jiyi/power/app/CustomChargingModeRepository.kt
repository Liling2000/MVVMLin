package com.jiyi.power.app

import com.aleyn.mvvm.utils.MmkvManager
import com.jiyi.power.app.bean.CustomChargingMode

object CustomChargingModeRepository {
    private const val KEY_MODES = "custom_charging_modes"
    private const val KEY_SELECTED_ID = "selected_custom_charging_mode_id"

    fun getAll(): List<CustomChargingMode> = MmkvManager.getList<CustomChargingMode>(KEY_MODES)
    fun findById(id: Long): CustomChargingMode? = getAll().firstOrNull { it.id == id }
    fun selectedId(): Long = MmkvManager.getString(KEY_SELECTED_ID).toLongOrNull() ?: -1L

    fun save(mode: CustomChargingMode) {
        val modes = getAll().toMutableList()
        val index = modes.indexOfFirst { it.id == mode.id }
        if (index >= 0) modes[index] = mode else modes += mode
        MmkvManager.putList(KEY_MODES, modes)
        if (selectedId() <= 0) select(mode.id)
    }

    fun select(id: Long) = MmkvManager.putString(KEY_SELECTED_ID, id.toString())
}
