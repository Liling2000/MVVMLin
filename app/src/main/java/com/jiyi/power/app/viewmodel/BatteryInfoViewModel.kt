package com.jiyi.power.app.viewmodel

import androidx.lifecycle.ViewModel
import com.jiyi.power.app.bean.BatteryCellInfo
import com.jiyi.power.app.bean.BatteryInfoUiData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class BatteryInfoViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(
        BatteryInfoUiData(
            healthPercent = 100,
            manufacturer = "SUNPOWER",
            model = "INR21700-5000",
            cycleCount = 12,
            recommendedYears = 5,
            batterySeries = "5S",
            cells = listOf(5000, 5000, 5000, 5000, 5000).mapIndexed { index, voltage -> BatteryCellInfo(index + 1, voltage) },
            ratedPowerW = 300,
            maxChargePowerW = 150,
            maxDischargePowerW = 300,
            totalDischargeHours = 20,
            totalDischargeCapacityMah = 9315
        )
    )
    val uiState = _uiState.asStateFlow()
}
