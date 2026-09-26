package com.jiyi.power.app.viewmodel

import com.jiyi.power.app.CustomChargingModeRepository
import com.jiyi.power.app.bean.CustomChargingMode
import com.jiyi.power.app.bean.PowerChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

class CustomChargingModeViewModel : DeviceCommandViewModel() {
    private val _channels = MutableStateFlow(defaultChannels())
    val channels = _channels.asStateFlow()
    private var pendingWrite: Pair<String, CompletableDeferred<Boolean>>? = null

    init {
        launch {
            commandEvents.collect { event ->
                when (event) {
                    is CommandEvent.WriteSucceeded -> pendingWrite
                        ?.takeIf { it.first == event.functionCode }?.second?.complete(true)
                    is CommandEvent.WriteFailed -> pendingWrite
                        ?.takeIf { it.first == event.functionCode }?.second?.complete(false)
                    else -> Unit
                }
            }
        }
    }

    fun load(modeId: Long?) {
        val mode = modeId?.let(CustomChargingModeRepository::findById)
        _channels.value = if (mode == null) defaultChannels() else listOf(
            PowerChannel("C1", mode.c1Power, 20, 140),
            PowerChannel("C2", mode.c2Power, 20, 140),
            PowerChannel("A", normalizeAPower(mode.aPower), A_MIN_POWER, A_MAX_POWER, discrete = true),
        )
    }

    fun adjustPower(index: Int, delta: Int) {
        _channels.value = _channels.value.mapIndexed { currentIndex, channel ->
            if (currentIndex != index) channel else {
                val power = if (channel.discrete) {
                    when {
                        delta < 0 -> channel.minPower
                        delta > 0 -> channel.maxPower
                        else -> channel.power
                    }
                } else {
                    (channel.power + delta).coerceIn(channel.minPower, channel.maxPower)
                }
                channel.copy(power = power)
            }
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
            current.first { it.name == "A" }.power,
        )
    }

    suspend fun applyPower(): Boolean {
        val c1 = _channels.value.firstOrNull { it.name == "C1" }?.power ?: return false
        val c2 = _channels.value.firstOrNull { it.name == "C2" }?.power ?: return false
        return writePower("32", c1) && writePower("33", c2)
    }

    private suspend fun writePower(code: String, power: Int): Boolean {
        val result = CompletableDeferred<Boolean>()
        pendingWrite = code to result
        return try {
            sendDeviceCommand(code, com.jiyi.power.app.utils.MobilePowerProtocolManager
                .buildWriteByteCommand(code, power)) &&
                withTimeoutOrNull(5_000) { result.await() } == true
        } finally {
            pendingWrite = null
            result.cancel()
        }
    }

    private fun defaultChannels() = listOf(
        PowerChannel("C1", 20, 20, 140),
        PowerChannel("C2", 20, 20, 140),
        PowerChannel("A", A_MIN_POWER, A_MIN_POWER, A_MAX_POWER, discrete = true),
    )

    private fun normalizeAPower(power: Int) =
        if (power > (A_MIN_POWER + A_MAX_POWER) / 2) A_MAX_POWER else A_MIN_POWER

    private companion object {
        const val A_MIN_POWER = 18
        const val A_MAX_POWER = 22
    }
}
