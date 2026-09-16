package com.jiyi.power.app.viewmodel

import com.jiyi.power.app.utils.ChargingModeProtocol
import com.jiyi.power.app.utils.CmdConstant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeoutOrNull

class ChargingModeViewModel : DeviceCommandViewModel() {
    data class UiState(val selectedMode: Int? = null, val isBusy: Boolean = false)

    private val protocol = ChargingModeProtocol()
    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()
    private var pendingRead: CompletableDeferred<ChargingModeProtocol.Reading?>? = null
    private var pendingWrite: Pair<String, CompletableDeferred<Boolean>>? = null

    init {
        launch {
            commandEvents.collect { event ->
                if (event is CommandEvent.WriteSucceeded) {
                    pendingWrite?.takeIf { it.first == event.functionCode }?.second?.complete(true)
                }
                if (event is CommandEvent.WriteFailed && event.functionCode in listOf("3B", "3C")) {
                    pendingWrite?.takeIf { it.first == event.functionCode }?.second?.complete(false)
                    pendingRead?.complete(null)
                }
            }
        }
    }

    fun refresh() {
        if (_uiState.value.isBusy) return
        _uiState.value = UiState(isBusy = true)
        launch {
            try {
                readMode()
            } finally {
                _uiState.value = _uiState.value.copy(isBusy = false)
            }
        }
    }

    /** Success requires device readback of both ports, not merely enqueueing BLE writes. */
    suspend fun applyMode(mode: Int): Boolean {
        if (mode !in 0..2 || _uiState.value.isBusy) return false
        _uiState.value = _uiState.value.copy(isBusy = true)
        try {
            val commands = protocol.writeCommands(mode)
            if (!writeMode(CmdConstant.FunctionCode.CODE_3B, commands[0]) ||
                !writeMode(CmdConstant.FunctionCode.CODE_3C, commands[1])) {
                _uiState.value = UiState(isBusy = true)
                return false
            }
            // Both BLE writes have completed before requesting device readback.
            return readMode()?.selectedMode == mode
        } finally {
            _uiState.value = _uiState.value.copy(isBusy = false)
        }
    }

    private suspend fun writeMode(code: String, command: String): Boolean {
        val result = CompletableDeferred<Boolean>()
        pendingWrite = code to result
        try {
            return sendDeviceCommand(code, command) &&
                withTimeoutOrNull(5_000) { result.await() } == true
        } finally {
            pendingWrite = null
            result.cancel()
        }
    }

    private suspend fun readMode(): ChargingModeProtocol.Reading? {
        protocol.reset()
        val reply = CompletableDeferred<ChargingModeProtocol.Reading?>()
        pendingRead = reply
        try {
            val reading = if (sendDeviceCommand(CmdConstant.FunctionCode.CODE_3B, protocol.readCommand())) {
                withTimeoutOrNull(5_000) { reply.await() }
            } else null
            _uiState.value = _uiState.value.copy(selectedMode = reading?.selectedMode)
            return reading
        } finally {
            pendingRead = null
            reply.cancel()
        }
    }

    override fun onBleDataReceive(data: String?) {
        super.onBleDataReceive(data)
        protocol.accept(data).forEach { reading -> pendingRead?.complete(reading) }
    }

    override fun onDeviceReconnected() = refresh()

    override fun onDeviceDisconnected() {
        protocol.reset()
        pendingWrite?.second?.complete(false)
        pendingRead?.complete(null)
        _uiState.value = _uiState.value.copy(selectedMode = null)
    }
}
