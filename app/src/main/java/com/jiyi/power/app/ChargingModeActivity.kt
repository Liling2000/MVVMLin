package com.jiyi.power.app

import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import androidx.recyclerview.widget.LinearLayoutManager
import com.aleyn.mvvm.base.BaseActivity
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.blankj.utilcode.util.BarUtils
import com.jiyi.power.R
import com.jiyi.power.app.adapter.ChargingModeAdapter
import com.jiyi.power.app.common.ChargingPreferences
import com.jiyi.power.app.common.RouterPath
import com.jiyi.power.app.common.RouterPath.ROUTE_MODE_LIST
import com.jiyi.power.app.viewmodel.ChargingModeViewModel
import com.jiyi.power.databinding.ActivityChargingModeBinding

@Route(path = RouterPath.ROUTE_CHARGING_MODE)
class ChargingModeActivity : BaseActivity<ActivityChargingModeBinding>() {
    private val viewModel by viewModels<ChargingModeViewModel>()
    private val preferences by lazy { getSharedPreferences(ChargingPreferences.FILE_NAME, MODE_PRIVATE) }

    private val deviceSn by lazy {
        intent.getStringExtra(MobilePowerMainActivity.EXTRA_DEVICE_SN)
            ?: com.jiyi.power.app.bean.BleDeviceStore.getDevices().lastOrNull()?.bluetoothSn
    }

    private val modeAdapter = ChargingModeAdapter { mode ->
        if (mode == ChargingPreferences.MODE_CUSTOM) {
            ARouter.getInstance().build(ROUTE_MODE_LIST)
                .withString(MobilePowerMainActivity.EXTRA_DEVICE_SN, deviceSn).navigation()
        } else {
            selectMode(mode)
        }
    }

    override fun initView(savedInstanceState: Bundle?) {
        val background = ContextCompat.getColor(this, R.color.color_f6f7f9)
        BarUtils.setStatusBarColor(this, background)
        BarUtils.setStatusBarLightMode(this, true)
        BarUtils.setNavBarColor(this, background)
        mBinding.toolbar.setLeftClickListener { finish() }
        mBinding.recyclerModes.layoutManager = LinearLayoutManager(this)
        mBinding.recyclerModes.adapter = modeAdapter
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    modeAdapter.selectMode(state.selectedMode)
                    modeAdapter.setInteractionEnabled(!state.isBusy)
                    state.selectedMode?.let(::saveConfirmedMode)
                }
            }
        }
    }

    override fun initData() {
        viewModel.bindDevice(deviceSn)
    }

    override fun onStart() {
        super.onStart()
        viewModel.refresh()
    }

    private fun selectMode(mode: Int) {
        if (viewModel.uiState.value.isBusy) return
        lifecycleScope.launch {
            if (!viewModel.applyMode(mode)) {
                com.blankj.utilcode.util.ToastUtils.showShort(R.string.power_command_failed)
            }
        }
    }

    private fun saveConfirmedMode(mode: Int) {
        preferences.edit()
            .putInt(ChargingPreferences.KEY_MODE, mode)
            .putBoolean(ChargingPreferences.KEY_LEGACY_SMART_MODE, mode == ChargingPreferences.MODE_SMART)
            .apply()
    }
}
