package com.jiyi.power.app

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.aleyn.mvvm.base.BaseVMActivity
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.blankj.utilcode.util.BarUtils
import com.blankj.utilcode.util.ToastUtils
import com.jiyi.power.R
import com.jiyi.power.app.adapter.BleDeviceAdapter
import com.jiyi.power.app.common.RouterPath
import com.jiyi.power.app.utils.BlePermissionManager
import com.jiyi.power.app.utils.BlePermissionResult
import com.jiyi.power.app.viewmodel.BleConnectEvent
import com.jiyi.power.app.viewmodel.BleUiState
import com.jiyi.power.app.viewmodel.BleViewModel
import com.jiyi.power.databinding.ActivityBleDeviceScanBinding
import com.jiyi.power.app.MobilePowerMainActivity
import kotlinx.coroutines.launch

@Route(path = RouterPath.BLE_SCAN)
class BleDeviceScanActivity : BaseVMActivity<BleViewModel, ActivityBleDeviceScanBinding>() {

    private val deviceAdapter = BleDeviceAdapter { device -> viewModel.connect(device) }
    private var scanAnimator: ObjectAnimator? = null

    override fun initView(savedInstanceState: Bundle?) {
        BarUtils.setStatusBarColor(this, ContextCompat.getColor(this, R.color.color_f0f1f4))
        BarUtils.setStatusBarLightMode(this, true)
        mBinding.toolbar.setLeftClickListener { finish() }
        mBinding.toolbar.setRightIconClickListener {
            ToastUtils.showShort(R.string.scan_help_message)
        }
        mBinding.rvDevices.apply {
            layoutManager = LinearLayoutManager(this@BleDeviceScanActivity)
            adapter = deviceAdapter
            itemAnimator = null
        }
    }

    override fun initObserve() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.connectEvent.collect { event ->
                    when (event) {
                        is BleConnectEvent.Success -> {
                            ToastUtils.showShort(R.string.scan_connect_success)
                            ARouter.getInstance()
                                .build(RouterPath.MOBILE_POWER_MAIN)
                                .withString(
                                    MobilePowerMainActivity.EXTRA_DEVICE_SN,
                                    event.device.bluetoothSn,
                                )
                                .navigation()
                            finish()
                        }
                        is BleConnectEvent.Failed -> ToastUtils.showShort(event.message)
                    }
                }
            }
        }
    }

    override fun initData() {
        viewModel.initBle(applicationContext)
        requestAndStartScan()
    }

    override fun onDestroy() {
        scanAnimator?.cancel()
        viewModel.stopScan()
        super.onDestroy()
    }

    private fun requestAndStartScan() {
        BlePermissionManager.requestBluetoothPermissions(this) { result ->
            when (result) {
                BlePermissionResult.Granted -> viewModel.startScan()
                is BlePermissionResult.Denied -> ToastUtils.showShort(R.string.bluetooth_permission_denied)
                is BlePermissionResult.PermanentlyDenied -> ToastUtils.showShort(R.string.bluetooth_permission_settings)
            }
        }
    }

    private fun render(state: BleUiState) {
        deviceAdapter.submitList(state.devices)
        deviceAdapter.setConnectingDevice(state.connectingSn)
        updateScanAnimation(state.isScanning)
    }

    private fun updateScanAnimation(scanning: Boolean) {
        if (scanning && scanAnimator?.isRunning != true) {
            scanAnimator = ObjectAnimator.ofFloat(mBinding.imageScanning, "alpha", 1f, 0.68f, 1f).apply {
                duration = 1_500L
                repeatCount = ValueAnimator.INFINITE
                start()
            }
        } else if (!scanning) {
            scanAnimator?.cancel()
            mBinding.imageScanning.alpha = 1f
        }
    }
}
