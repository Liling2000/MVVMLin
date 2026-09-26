package com.jiyi.power.app

import com.alibaba.android.arouter.launcher.ARouter

import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.recyclerview.widget.LinearLayoutManager
import com.aleyn.mvvm.base.BaseActivity
import com.alibaba.android.arouter.facade.annotation.Route
import com.jiyi.power.R
import com.jiyi.power.app.adapter.CustomChargingModeAdapter
import com.jiyi.power.app.common.ChargingPreferences
import com.jiyi.power.app.common.RouterPath
import com.jiyi.power.app.viewmodel.ChargingModeViewModel
import com.jiyi.power.databinding.ActivityCustomChargingModeListBinding

@Route(path = RouterPath.PAGE_ROUTE_MODE_LIST)
class CustomChargingModeListActivity : BaseActivity<ActivityCustomChargingModeListBinding>() {
    private val viewModel by viewModels<ChargingModeViewModel>()
    private val modeAdapter = CustomChargingModeAdapter(
        onSelect = { mode -> selectMode(mode.id) },
        onEdit = { mode -> openEditor(mode.id) },
    )

    override fun initSystemBars() {
        setSystemBars(statusBarColorRes = R.color.color_f6f7f9)
    }

    override fun initView(savedInstanceState: Bundle?) = with(mBinding) {
        toolbar.setLeftClickListener { finish() }
        recyclerModes.layoutManager = LinearLayoutManager(this@CustomChargingModeListActivity)
        recyclerModes.adapter = modeAdapter
        buttonAddMode.setOnClickListener { openEditor(null) }
    }

    override fun onResume() {
        super.onResume()
        refreshModes()
    }

    override fun initData() {
        viewModel.bindDevice(intent.getStringExtra(MobilePowerMainActivity.EXTRA_DEVICE_SN))
    }

    private fun refreshModes() {
        modeAdapter.submitList(CustomChargingModeRepository.getAll(), -1L)
        lifecycleScope.launch {
            val state = viewModel.readDeviceState()
            val selected = if (state?.mode == ChargingPreferences.MODE_CUSTOM) {
                state.customPower?.let {
                    CustomChargingModeRepository.resolveSelected(it.c1Power, it.c2Power)
                } ?: run {
                    CustomChargingModeRepository.clearSelection()
                    null
                }
            } else {
                if (state?.mode != null) CustomChargingModeRepository.clearSelection()
                null
            }
            modeAdapter.submitList(CustomChargingModeRepository.getAll(), selected?.id ?: -1L)
        }
    }

    private fun selectMode(id: Long) {
        if (viewModel.uiState.value.isBusy) return
        lifecycleScope.launch {
            val mode = CustomChargingModeRepository.findById(id)
            if (mode == null || !viewModel.applyMode(ChargingPreferences.MODE_CUSTOM) ||
                !viewModel.applyCustomPower(mode.c1Power, mode.c2Power)) {
                com.blankj.utilcode.util.ToastUtils.showShort(R.string.power_command_failed)
                return@launch
            }
            CustomChargingModeRepository.select(id)
            getSharedPreferences(ChargingPreferences.FILE_NAME, MODE_PRIVATE).edit()
                .putInt(ChargingPreferences.KEY_MODE, ChargingPreferences.MODE_CUSTOM)
                .putBoolean(ChargingPreferences.KEY_LEGACY_SMART_MODE, false).apply()
            modeAdapter.submitList(CustomChargingModeRepository.getAll(), id)
        }
    }

    private fun openEditor(id: Long?) {
        ARouter.getInstance().build(RouterPath.PAGE_CUSTOM_CHARGING_MODE)
            .withString(MobilePowerMainActivity.EXTRA_DEVICE_SN, intent.getStringExtra(MobilePowerMainActivity.EXTRA_DEVICE_SN))
            .apply { id?.let { withLong(CustomChargingModeActivity.EXTRA_MODE_ID, it) } }
            .navigation(this)
    }
}
