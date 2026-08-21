package com.jiyi.power.app.fragment

import android.os.Bundle
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.aleyn.mvvm.base.BaseFragment
import com.aleyn.mvvm.R as BaseR
import com.alibaba.android.arouter.launcher.ARouter
import com.jiyi.power.R
import com.jiyi.power.app.adapter.HomeBannerAdapter
import com.jiyi.power.app.adapter.HomeDeviceAdapter
import com.jiyi.power.app.adapter.HomeDeviceItem
import com.jiyi.power.app.bean.BleDeviceStore
import com.jiyi.power.app.ble.BleConnectionCoordinator
import com.jiyi.power.app.ble.DeviceConnectionState
import com.jiyi.power.app.common.RouterPath
import com.jiyi.power.databinding.HomeFragmentBinding
import com.jiyi.power.app.MobilePowerMainActivity
import com.jiyi.power.app.utils.BlePermissionManager
import com.jiyi.power.app.utils.BlePermissionResult
import com.jiyi.power.app.widget.popup.AppPopupManager
import com.blankj.utilcode.util.ToastUtils
import com.youth.banner.indicator.CircleIndicator
import kotlinx.coroutines.launch

class HomeFragment : BaseFragment<HomeFragmentBinding>() {
    private val deviceAdapter = HomeDeviceAdapter(this::openDevice)
    private var permissionExplanationShown = false

    override fun initView(savedInstanceState: Bundle?) {
        setupBanner()
        setupDeviceList()
        mBinding.buttonAddDevice.setOnClickListener { openDeviceScanner() }
        observeConnectionStates()
    }

    private fun setupBanner() = with(mBinding.bannerHome) {
        adapter = HomeBannerAdapter(
            listOf(
                R.mipmap.home_banner_show,
                R.mipmap.home_banner_show,
                R.mipmap.home_banner_show
            )
        )
        indicator = CircleIndicator(requireContext())
        setIndicatorNormalColorRes(BaseR.color.color_ffffff)
        setIndicatorSelectedColorRes(R.color.color_004098)
        setIndicatorSpace(10)
        setIndicatorRadius(4)
        setLoopTime(4_000)
        addBannerLifecycleObserver(viewLifecycleOwner)
    }

    private fun setupDeviceList() {
        mBinding.recyclerDevices.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = deviceAdapter
            setHasFixedSize(true)
            // 连接状态更新只替换文字，禁用默认的整卡淡入淡出，避免多设备回连时连续闪烁。
            (itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        }
        renderDevices(BleConnectionCoordinator.connectionStates.value)
    }

    override fun onResume() {
        super.onResume()
        refreshHomeDevices()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden && view != null) refreshHomeDevices()
    }

    private fun refreshHomeDevices() {
        val devices = BleDeviceStore.getDevices()
        BleConnectionCoordinator.refreshBoundDevices()
        // BleDeviceStore 不是可观察数据源；每次首页可见都必须主动重建列表。
        renderDevices(BleConnectionCoordinator.connectionStates.value)
        if (devices.isEmpty()) return
        if (BlePermissionManager.hasBluetoothPermissions()) {
            BleConnectionCoordinator.startAutoReconnect()
        } else if (!permissionExplanationShown) {
            permissionExplanationShown = true
            AppPopupManager.showPermissionRequest(requireContext(), onAgree = {
                BlePermissionManager.requestBluetoothPermissions(requireActivity()) { result ->
                    when (result) {
                        BlePermissionResult.Granted -> BleConnectionCoordinator.startAutoReconnect()
                        is BlePermissionResult.Denied -> ToastUtils.showShort(R.string.bluetooth_permission_denied)
                        is BlePermissionResult.PermanentlyDenied -> {
                            ToastUtils.showShort(R.string.bluetooth_permission_settings)
                            BlePermissionManager.openAppSettings()
                        }
                    }
                }
            })
        }
    }

    private fun observeConnectionStates() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                BleConnectionCoordinator.connectionStates.collect(::renderDevices)
            }
        }
    }

    private fun renderDevices(states: Map<String, DeviceConnectionState>) {
        val items = BleDeviceStore.getDevices().map { saved ->
            val status = when (states[saved.bluetoothSn]) {
                DeviceConnectionState.CONNECTED -> R.string.home_bluetooth_connected
                DeviceConnectionState.CONNECTING -> R.string.home_bluetooth_connecting
                else -> R.string.home_bluetooth_disconnected
            }
            HomeDeviceItem.Device(
                sn = saved.bluetoothSn,
                name = saved.bluetoothName.ifBlank { getString(R.string.home_device_name) },
                description = getString(R.string.home_device_description),
                power = getString(R.string.home_device_power),
                status = getString(status),
                imageRes = saved.deviceIcon,
            )
        }.toMutableList<HomeDeviceItem>()
        items += HomeDeviceItem.AddDevice
        deviceAdapter.submitList(items)
    }

    private fun openDeviceScanner() {
        ARouter.getInstance().build(RouterPath.BLE_SCAN).navigation()
    }

    private fun openDevice(item: HomeDeviceItem.Device?) {
        Log.e("LLK", "openDevice")
        if (item == null) {
            openDeviceScanner()
            return
        }
        ARouter.getInstance().build(RouterPath.MOBILE_POWER_MAIN)
            .withString(MobilePowerMainActivity.EXTRA_DEVICE_SN, item.sn)
            .navigation()
    }
}
