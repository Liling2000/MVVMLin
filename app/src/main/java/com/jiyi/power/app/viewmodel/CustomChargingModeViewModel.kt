package com.jiyi.power.app.viewmodel

import androidx.lifecycle.ViewModel
import com.jiyi.power.app.CustomChargingModeRepository
import com.jiyi.power.app.bean.CustomChargingMode
import com.jiyi.power.app.bean.PowerChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class CustomChargingModeViewModel : ViewModel() {
    private val _channels = MutableStateFlow(defaultChannels())
    val channels = _channels.asStateFlow()

    fun load(modeId: Long?) {
        val mode = modeId?.let(CustomChargingModeRepository::findById)
        _channels.value = if (mode == null) defaultChannels() else listOf(
            PowerChannel("C1", mode.c1Power, 20, 140),
            PowerChannel("C2", mode.c2Power, 20, 140),
            PowerChannel("A", mode.aPower, 18, 22),
        )
    }

    fun adjustPower(index: Int, delta: Int) {
        _channels.value = _channels.value.mapIndexed { currentIndex, channel ->
            if (currentIndex != index) channel else channel.copy(
                power = (channel.power + delta).coerceIn(channel.minPower, channel.maxPower),
            )
        }
    }

    fun setPower(index: Int, power: Int) = adjustPower(index, power - _channels.value[index].power)

    fun toMode(id: Long?, name: String): CustomChargingMode {
        val current = _channels.value
        return CustomChargingMode(
            id ?: System.currentTimeMillis(),
            name,
            current[0].power,
            current[1].power,
            current[2].power
        )
    }

    private fun defaultChannels() = listOf(
        PowerChannel("C1", 20, 20, 140),
        PowerChannel("C2", 20, 20, 140),
        PowerChannel("A", 18, 18, 22),
    )
}
