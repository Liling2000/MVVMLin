package com.jiyi.power.app

import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import androidx.recyclerview.widget.LinearLayoutManager
import com.aleyn.mvvm.base.BaseActivity
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.jiyi.power.R
import com.jiyi.power.app.adapter.ChargingModeAdapter
import com.jiyi.power.app.common.ChargingPreferences
import com.jiyi.power.app.common.RouterPath
import com.jiyi.power.app.common.RouterPath.PAGE_ROUTE_MODE_LIST
import com.jiyi.power.app.viewmodel.ChargingModeViewModel
import com.jiyi.power.databinding.ActivityChargingModeBinding

@Route(path = RouterPath.PAGE_ROUTE_CHARGING_MODE)
class ChargingModeActivity : BaseActivity<ActivityChargingModeBinding>() {
    private val viewModel by viewModels<ChargingModeViewModel>()
    private val preferences by lazy { getSharedPreferences(ChargingPreferences.FILE_NAME, MODE_PRIVATE) }

    private val deviceSn by lazy {
        intent.getStringExtra(MobilePowerMainActivity.EXTRA_DEVICE_SN)
            ?: com.jiyi.power.app.bean.BleDeviceStore.getDevices().lastOrNull()?.bluetoothSn
    }

    private val modeAdapter = ChargingModeAdapter { mode ->
        if (mode == ChargingPreferences.MODE_CUSTOM) {
            ARouter.getInstance().build(PAGE_ROUTE_MODE_LIST)
                .withString(MobilePowerMainActivity.EXTRA_DEVICE_SN, deviceSn)
                .navigation(this@ChargingModeActivity)
        } else {
            selectMode(mode)
        }
    }

    override fun initView(savedInstanceState: Bundle?) {
        mBinding.toolbar.setLeftClickListener { finish() }
        mBinding.recyclerModes.apply {
            layoutManager = LinearLayoutManager(this@ChargingModeActivity)
            adapter = modeAdapter
            // 模式刷新只更新选中/未选中内容，不使用默认的透明度交叉动画。
            itemAnimator = null
        }
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
            showLoading(getString(R.string.setting_in_progress))
            try {
                if (!viewModel.applyMode(mode)) {
                    com.blankj.utilcode.util.ToastUtils.showShort(R.string.power_command_failed)
                }
            } finally {
                dismissLoading()
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
