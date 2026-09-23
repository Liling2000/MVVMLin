package com.jiyi.power.app

import com.alibaba.android.arouter.launcher.ARouter

import com.jiyi.power.app.common.RouterPath

import com.alibaba.android.arouter.facade.annotation.Route

import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.aleyn.mvvm.base.BaseActivity
import com.jiyi.power.R
import com.jiyi.power.app.adapter.BatteryCellAdapter
import com.jiyi.power.app.bean.BatteryInfoUiData
import com.jiyi.power.app.viewmodel.BatteryInfoViewModel
import com.jiyi.power.databinding.ActivityBatteryInfoBinding
import kotlinx.coroutines.launch

@Route(path = RouterPath.PAGE_BATTERY_INFO)
class BatteryInfoActivity : BaseActivity<ActivityBatteryInfoBinding>() {
    private val viewModel by viewModels<BatteryInfoViewModel>()
    private val cellAdapter = BatteryCellAdapter()

    override fun initView(savedInstanceState: Bundle?) {
        mBinding.toolbar.setLeftClickListener { finish() }
        mBinding.toolbar.setRightIconClickListener {
            ARouter.getInstance().build(RouterPath.PAGE_DEVICE_EXCEPTION_RECORD)
                .withString(MobilePowerMainActivity.EXTRA_DEVICE_SN,
                    intent.getStringExtra(MobilePowerMainActivity.EXTRA_DEVICE_SN))
                .navigation(this)
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

    override fun initData() {
        viewModel.bindDevice(intent.getStringExtra(MobilePowerMainActivity.EXTRA_DEVICE_SN))
    }

    override fun onStart() {
        super.onStart()
        viewModel.refresh()
    }

    private fun renderBatteryInfo(data: BatteryInfoUiData) = with(mBinding) {
        val health = data.healthPercent?.coerceIn(0, 100)
        textHealthPercent.text =
            health?.let { getString(R.string.battery_health_percent, it) } ?: emptyValue()
        progressHealth.progress = health ?: 0
        // 协议只提供健康百分比，未定义“优秀”等等级。
        textHealthLevel.visibility = android.view.View.GONE
        textHealthCycle.text = data.cycleCount?.let {
            getString(R.string.battery_cycle_format, it)
        } ?: emptyValue()
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
        infoDischargeTime.setRightTextValue(data.totalDischargeMinutes?.let {
            getString(
                R.string.battery_duration_format, it / 60, it % 60
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
