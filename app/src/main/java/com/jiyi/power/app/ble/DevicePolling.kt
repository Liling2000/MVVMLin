package com.jiyi.power.app.ble

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Polls a device only while this owner is visible and that device is connected.
 *
 * A new polling session starts after every resume or reconnect. [poll] is invoked
 * immediately, and [firstPoll] is true only for the first invocation in that session.
 */
fun LifecycleOwner.launchDevicePolling(
    deviceSn: String?,
    intervalMs: Long,
    poll: suspend (firstPoll: Boolean) -> Unit,
): Job {
    require(intervalMs > 0) { "Polling interval must be greater than zero" }

    return lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            BleConnectionCoordinator.connectionStates.map { states ->
                !deviceSn.isNullOrBlank() && states.any { (sn, state) ->
                    sn.equals(
                        deviceSn, ignoreCase = true
                    ) && state == DeviceConnectionState.CONNECTED
                }
            }.distinctUntilChanged().collectLatest { connected ->
                if (!connected) return@collectLatest

                var firstPoll = true
                while (currentCoroutineContext().isActive) {
                    poll(firstPoll)
                    firstPoll = false
                    delay(intervalMs)
                }
            }
        }
    }
}
