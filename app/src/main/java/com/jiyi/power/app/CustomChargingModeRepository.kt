package com.jiyi.power.app

import com.aleyn.mvvm.utils.MmkvManager
import com.jiyi.power.app.bean.CustomChargingMode

object CustomChargingModeRepository {
    private const val KEY_MODES = "custom_charging_modes"
    private const val KEY_SELECTED_ID = "selected_custom_charging_mode_id"

    fun getAll(): List<CustomChargingMode> = MmkvManager.getList<CustomChargingMode>(KEY_MODES)
    fun findById(id: Long): CustomChargingMode? = getAll().firstOrNull { it.id == id }
    fun selectedId(): Long = MmkvManager.getString(KEY_SELECTED_ID).toLongOrNull() ?: -1L

    /** Prefer the cached selection, then reconcile it with the first mode matching device power. */
    fun resolveSelected(c1Power: Int, c2Power: Int): CustomChargingMode? {
        val modes = getAll()
        val matchesPower: (CustomChargingMode) -> Boolean = {
            it.c1Power == c1Power && it.c2Power == c2Power
        }
        modes.firstOrNull { it.id == selectedId() && matchesPower(it) }?.let { return it }
        return modes.firstOrNull(matchesPower)?.also { select(it.id) }
            ?: run {
                clearSelection()
                null
            }
    }

    fun save(mode: CustomChargingMode) {
        val modes = getAll().toMutableList()
        val index = modes.indexOfFirst { it.id == mode.id }
        if (index >= 0) modes[index] = mode else modes += mode
        MmkvManager.putList(KEY_MODES, modes)
    }

    fun select(id: Long) = MmkvManager.putString(KEY_SELECTED_ID, id.toString())
    fun clearSelection() = MmkvManager.remove(KEY_SELECTED_ID)
}
