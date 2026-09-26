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
import com.jiyi.power.app.bean.BleSavedDevice
import com.jiyi.power.app.ble.BleConnectionCoordinator
import com.jiyi.power.app.ble.BleIoEvent
import com.jiyi.power.app.ble.DeviceConnectionState
import com.jiyi.power.app.common.RouterPath
import com.jiyi.power.databinding.HomeFragmentBinding
import com.jiyi.power.app.MobilePowerMainActivity
import com.jiyi.power.app.utils.BlePermissionManager
import com.jiyi.power.app.utils.BlePermissionResult
import com.jiyi.power.app.utils.CmdConstant
import com.jiyi.power.app.utils.HomeDeviceBatteryState
import com.jiyi.power.app.utils.MobilePowerProtocolManager
import com.jiyi.power.app.widget.popup.AppPopupManager
import com.blankj.utilcode.util.ToastUtils
import com.youth.banner.indicator.CircleIndicator
import com.liling.ble.utils.BleUtils
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class HomeFragment : BaseFragment<HomeFragmentBinding>() {
    private val deviceAdapter = HomeDeviceAdapter(this::openDevice)
    private val deviceBatteryState = HomeDeviceBatteryState()
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
            // 列表位于 NestedScrollView 中且高度为 wrap_content；设备增删后必须重新测量。
            setHasFixedSize(false)
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
        val states = BleConnectionCoordinator.connectionStates.value
        val connectedSns = states
            .filterValues { it == DeviceConnectionState.CONNECTED }
            .keys
        deviceBatteryState.updateConnectedDevices(connectedSns)
        // 返回首页时主动重建列表，避免依赖状态流是否产生了新值。
        renderDevices(devices, states)
        queryBatteryPercent(connectedSns)
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
                try {
                    // 先监听回包，再发送查询，避免快速响应丢失。
                    launch(start = CoroutineStart.UNDISPATCHED) {
                        BleConnectionCoordinator.ioEvents.collect { event ->
                            if (event is BleIoEvent.Notification) {
                                val states = BleConnectionCoordinator.connectionStates.value
                                val connected = states.any { (sn, state) ->
                                    sn.equals(event.sn, ignoreCase = true) && state == DeviceConnectionState.CONNECTED
                                }
                                if (connected) {
                                    val previous = deviceBatteryState.percent(event.sn)
                                    deviceBatteryState.accept(event.sn, BleUtils.byteToString(event.data))
                                    if (previous != deviceBatteryState.percent(event.sn)) renderDevices(states)
                                }
                            }
                        }
                    }
                    combine(
                        BleDeviceStore.devices,
                        BleConnectionCoordinator.connectionStates,
                    ) { devices, states -> devices to states }
                        .collect { (devices, states) ->
                            val connectedSns = states
                                .filterValues { it == DeviceConnectionState.CONNECTED }
                                .keys
                            val newConnections = deviceBatteryState.updateConnectedDevices(connectedSns)
                            renderDevices(devices, states)
                            queryBatteryPercent(newConnections)
                        }
                } finally {
                    // 页面重新可见后会查询新电量；这里只丢弃通知半包，保留已显示的有效值。
                    deviceBatteryState.prepareRefresh()
                }
            }
        }
    }

    private fun queryBatteryPercent(deviceSns: Set<String>) {
        if (deviceSns.isEmpty()) return
        val command = MobilePowerProtocolManager.buildReadCommand(
            CmdConstant.FunctionCode.CODE_16,
        ) ?: return
        val data = BleUtils.hexStringToByte(command)
        deviceSns.forEach { sn -> BleConnectionCoordinator.write(sn, data) }
    }

    private fun renderDevices(
        devices: List<BleSavedDevice>,
        states: Map<String, DeviceConnectionState>,
    ) {
        val items = devices.map { saved ->
            val connectionState = states.entries.firstOrNull {
                it.key.equals(saved.bluetoothSn, ignoreCase = true)
            }?.value
            val status = when (connectionState) {
                DeviceConnectionState.CONNECTED -> R.string.home_bluetooth_connected
                DeviceConnectionState.CONNECTING -> R.string.home_bluetooth_connecting
                else -> R.string.home_bluetooth_disconnected
            }
            HomeDeviceItem.Device(
                sn = saved.bluetoothSn,
                name = saved.bluetoothName.ifBlank { getString(R.string.home_device_name) },
                description = getString(R.string.home_device_description),
                power = if (connectionState == DeviceConnectionState.CONNECTED) {
                    deviceBatteryState.percent(saved.bluetoothSn)?.let { getString(R.string.home_device_power, it) }
                        ?: getString(R.string.power_unknown_value)
                } else getString(R.string.power_unknown_value),
                status = getString(status),
                imageRes = saved.deviceIcon,
            )
        }.toMutableList<HomeDeviceItem>()
        items += HomeDeviceItem.AddDevice
        deviceAdapter.submitList(items)
    }

    private fun renderDevices(states: Map<String, DeviceConnectionState>) {
        renderDevices(BleDeviceStore.devices.value, states)
    }

    private fun openDeviceScanner() {
        ARouter.getInstance().build(RouterPath.PAGE_BLE_SCAN).navigation(requireActivity())
    }

    private fun openDevice(item: HomeDeviceItem.Device?) {
        Log.e("LLK", "openDevice")
        if (item == null) {
            openDeviceScanner()
            return
        }
        ARouter.getInstance().build(RouterPath.PAGE_MOBILE_POWER_MAIN)
            .withString(MobilePowerMainActivity.EXTRA_DEVICE_SN, item.sn)
            .navigation(requireActivity())
    }
}
