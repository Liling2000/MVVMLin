package com.jiyi.power.app

import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.aleyn.mvvm.base.BaseActivity
import com.blankj.utilcode.util.BarUtils
import com.jiyi.power.R
import com.jiyi.power.app.adapter.BatteryCellAdapter
import com.jiyi.power.app.bean.BatteryInfoUiData
import com.jiyi.power.app.viewmodel.BatteryInfoViewModel
import com.jiyi.power.databinding.ActivityBatteryInfoBinding
import kotlinx.coroutines.launch

class BatteryInfoActivity : BaseActivity<ActivityBatteryInfoBinding>() {
    private val viewModel by viewModels<BatteryInfoViewModel>()
    private val cellAdapter = BatteryCellAdapter()

    override fun initView(savedInstanceState: Bundle?) {
        val background = ContextCompat.getColor(this, R.color.color_f6f7f9)
        BarUtils.setStatusBarColor(this, background)
        BarUtils.setStatusBarLightMode(this, true)
        BarUtils.setNavBarColor(this, background)
        mBinding.toolbar.setLeftClickListener { finish() }
        mBinding.toolbar.setRightIconClickListener {
            startActivity(android.content.Intent(this, DeviceExceptionRecordActivity::class.java))
        }
        mBinding.recyclerCells.apply {
            layoutManager = LinearLayoutManager(this@BatteryInfoActivity)
            adapter = cellAdapter
            isNestedScrollingEnabled = false
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.uiState.collect(::renderBatteryInfo) }
        }
    }

    override fun initData() = Unit

    private fun renderBatteryInfo(data: BatteryInfoUiData) = with(mBinding) {
        val health = data.healthPercent?.coerceIn(0, 100)
        textHealthPercent.text =
            health?.let { getString(R.string.battery_health_percent, it) } ?: emptyValue()
        progressHealth.progress = health ?: 0
        infoManufacturer.setRightTextValue(data.manufacturer ?: emptyValue())
        infoModel.setRightTextValue(data.model ?: emptyValue())
        infoCycle.setRightTextValue(data.cycleCount?.let {
            getString(
                R.string.battery_cycle_format, it
            )
        } ?: emptyValue())
        infoRecommended.setRightTextValue(data.recommendedYears?.let {
            getString(
                R.string.battery_year_format, it
            )
        } ?: emptyValue())
        infoSeries.setRightTextValue(data.batterySeries ?: emptyValue())
        cellAdapter.submitList(data.cells)
        infoRatedPower.setRightTextValue(formatPower(data.ratedPowerW))
        infoMaxCharge.setRightTextValue(formatPower(data.maxChargePowerW))
        infoMaxDischarge.setRightTextValue(formatPower(data.maxDischargePowerW))
        infoDischargeTime.setRightTextValue(data.totalDischargeHours?.let {
            getString(
                R.string.battery_hours_format, it
            )
        } ?: emptyValue())
        infoDischargeCapacity.setRightTextValue(data.totalDischargeCapacityMah?.let {
            getString(
                R.string.battery_capacity_format, it
            )
        } ?: emptyValue())
    }

    private fun formatPower(value: Int?) =
        value?.let { getString(R.string.battery_power_format, it) } ?: emptyValue()

    private fun emptyValue() = getString(R.string.battery_empty_value)
}
