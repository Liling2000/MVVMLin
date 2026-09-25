package com.jiyi.power.app

import com.jiyi.power.app.utils.CmdConstant
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.aleyn.mvvm.base.BaseActivity
import com.aleyn.mvvm.extend.flowLaunch
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.blankj.utilcode.util.ToastUtils
import com.jiyi.power.R
import com.jiyi.power.app.bean.BleDeviceStore
import com.jiyi.power.app.common.RouterPath
import com.jiyi.power.app.common.MobilePowerConfig
import com.jiyi.power.databinding.ActivityMobilePowerMainBinding
import com.jiyi.power.databinding.ItemPowerPortBinding
import com.jiyi.power.app.bean.MobilePowerHomeInfoBean
import com.jiyi.power.app.bean.MobilePowerPortInfo
import com.jiyi.power.app.bean.MobilePowerPortType
import com.jiyi.power.app.bean.PortDirection
import com.jiyi.power.app.ble.BleConnectionCoordinator
import com.jiyi.power.app.ble.DeviceConnectionState
import com.jiyi.power.app.ble.launchDevicePolling
import com.jiyi.power.app.common.RouterPath.PAGE_ROUTE_THEME
import com.jiyi.power.app.viewmodel.MainFragmentViewModel
import com.jiyi.power.app.viewmodel.DeviceCommandViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.Locale

@Route(path = RouterPath.PAGE_MOBILE_POWER_MAIN)
class MobilePowerMainActivity : BaseActivity<ActivityMobilePowerMainBinding>() {
    override fun initSystemBars() {
        setSystemBars(statusBarColorRes = R.color.color_f7f9fb)
    }

    private val viewModel: MainFragmentViewModel by viewModels()
    private var selectedPort = Port.C1
    private var renderingSwitch = false
    private var pendingLowCurrentMode: Boolean? = null
    private val deviceSn: String? by lazy {
        intent.getStringExtra(EXTRA_DEVICE_SN) ?: BleDeviceStore.getDevices()
            .lastOrNull()?.bluetoothSn
    }

    override fun initView(savedInstanceState: Bundle?) {
        setupStaticContent()
        setupClicks()
        setupBleNotifications()
        setupDevicePolling()
        render(null)
    }

    override fun initObserve() {
        flowLaunch {
            viewModel.homeInfo.collect(::render)
        }
        flowLaunch {
            viewModel.portDetails.collect { renderPortDetails(viewModel.homeInfo.value) }
        }
        flowLaunch {
            viewModel.ratedCapacityMah.collect { ratedCapacityMah ->
                renderCurrentCapacity(
                    batteryPercent = viewModel.homeInfo.value?.battery?.percent,
                    ratedCapacityMah = ratedCapacityMah,
                )
            }
        }
    }

    override fun initData() {
        viewModel.bindDevice(deviceSn)
    }

    private fun setupDevicePolling() {
        launchDevicePolling(
            deviceSn = deviceSn,
            intervalMs = DASHBOARD_POLL_INTERVAL_MS,
        ) { firstPoll ->
            if (firstPoll) {
                viewModel.requestDashboard(deviceSn)
            } else {
                viewModel.requestDashboardSnapshot(deviceSn)
            }
        }
    }

    private fun setupStaticContent() = with(mBinding) {
        setupPortCard(cardC1, R.string.power_port_c1)
        setupPortCard(cardC2, R.string.power_port_c2)
        setupPortCard(cardA1, R.string.power_port_a1)

        detailCable.textLabel.setText(R.string.power_cable_info)
        detailCable.textValue.setText(R.string.power_unknown_value)
        detailCable.imageIcon.setImageResource(R.mipmap.ic_charging_cable)
        detailProtocol.textLabel.setText(R.string.power_charge_protocol)
        detailProtocol.imageIcon.setImageResource(R.mipmap.ic_lightning)
        detailModel.textLabel.setText(R.string.power_device_model)
        detailModel.imageIcon.setImageResource(R.mipmap.ic_device_outline)

        detailCycleCount.textLabel.setText(R.string.power_cycle_count)
        detailCapacity.textLabel.setText(R.string.power_current_capacity)
    }

    private fun setupClicks() = with(mBinding) {
        toolbar.setLeftClickListener { finish() }
        toolbar.setRightIconClickListener {
            ARouter.getInstance().build(RouterPath.PAGE_DEVICE_SETTING)
                .withString(EXTRA_DEVICE_SN, deviceSn).navigation(this@MobilePowerMainActivity)
        }
        cardC1.root.setOnClickListener { selectPort(Port.C1) }
        cardC2.root.setOnClickListener { selectPort(Port.C2) }
        cardA1.root.setOnClickListener { selectPort(Port.A1) }
        tabC1.setOnClickListener { selectPort(Port.C1) }
        tabC2.setOnClickListener { selectPort(Port.C2) }
        tabA1.setOnClickListener { selectPort(Port.A1) }

        switchLowCurrent.setOnClickListener { switchLowCurrent.toggle() }
        switchLowCurrent.setOnCheckedChangeListener { checked ->
            if (renderingSwitch) return@setOnCheckedChangeListener
            val sent = viewModel.setLowCurrentMode(deviceSn, checked)
            if (sent) {
                pendingLowCurrentMode = checked
                switchLowCurrent.isEnabled = false
            } else {
                renderingSwitch = true
                switchLowCurrent.setEnableEffect(false)
                switchLowCurrent.isChecked = viewModel.homeInfo.value?.settings?.lowCurrentMode == true
                switchLowCurrent.setEnableEffect(true)
                renderingSwitch = false
                ToastUtils.showShort(R.string.power_command_failed)
            }
        }

        buttonScreenSettings.setOnClickListener {
            ARouter.getInstance().build(PAGE_ROUTE_THEME).navigation(this@MobilePowerMainActivity)
        }
        cardBatteryInfo.setOnClickListener {
            ARouter.getInstance().build(RouterPath.PAGE_BATTERY_INFO)
                .withString(EXTRA_DEVICE_SN, deviceSn)
                .navigation(this@MobilePowerMainActivity)
        }
    }

    private fun setupPortCard(binding: ItemPowerPortBinding, labelRes: Int) {
        binding.textPort.setText(labelRes)
    }

    private fun setupBleNotifications() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 原始 BLE 数据已由 ViewModel 处理；Activity 仅消费业务级的写入/连接结果。
                viewModel.commandEvents.collect { event ->
                    when (event) {
                        is DeviceCommandViewModel.CommandEvent.WriteSucceeded -> if (event.functionCode == CmdConstant.FunctionCode.CODE_34) {
                            // 等设备应用新配置后重新读取寄存器，以设备回包为最终状态。
                            delay(150L)
                            viewModel.requestDashboardSnapshot(deviceSn)
                            mBinding.switchLowCurrent.isEnabled = true
                        }
                        is DeviceCommandViewModel.CommandEvent.WriteFailed -> if (event.functionCode == CmdConstant.FunctionCode.CODE_34) {
                            pendingLowCurrentMode = null
                            mBinding.switchLowCurrent.isEnabled = true
                            ToastUtils.showShort(R.string.power_command_failed)
                        }
                        DeviceCommandViewModel.CommandEvent.Disconnected ->
                            ToastUtils.showShort(R.string.power_device_disconnected_notice)
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun selectPort(port: Port) {
        selectedPort = port
        renderPortSelection()
        renderPortDetails(viewModel.homeInfo.value)
    }

    private fun render(info: MobilePowerHomeInfoBean?) = with(mBinding) {
        val battery = info?.battery
        textBatteryPercent.setTvContent((battery?.percent ?: 0).toString())
        val remainMinutes = battery?.dischargeRemainMinutes ?: 0
        textRemainingTime.text = getString(
            R.string.power_remaining_time,
            remainMinutes / 60,
            remainMinutes % 60,
        )
        textTemperature.text = getString(
            R.string.power_temperature_value,
            (battery?.temperatureC ?: 0).toFloat(),
        )

        renderPortCard(cardC1, info.port(MobilePowerPortType.C1))
        renderPortCard(cardC2, info.port(MobilePowerPortType.C2))
        renderPortCard(cardA1, info.port(MobilePowerPortType.USB_A))
        renderPortSelection()
        renderPortDetails(info)

        renderingSwitch = true
        switchLowCurrent.setEnableEffect(false)
        switchLowCurrent.isChecked = info?.settings?.lowCurrentMode == true
        switchLowCurrent.setEnableEffect(true)
        switchLowCurrent.isEnabled = BleConnectionCoordinator.connectionStates.value.any { (sn, state) ->
            sn.equals(deviceSn, ignoreCase = true) && state == DeviceConnectionState.CONNECTED
        }
        renderingSwitch = false
        pendingLowCurrentMode?.let { expected ->
            if (info != null) {
                if (info.settings.lowCurrentMode == expected) {
                    ToastUtils.showShort(
                        getString(
                            R.string.power_low_current_updated,
                            getString(if (expected) R.string.power_enabled else R.string.power_disabled),
                        ),
                    )
                } else {
                    ToastUtils.showShort(R.string.power_command_failed)
                }
                pendingLowCurrentMode = null
            }
        }
        textBatteryHealth.setTvContent(battery?.healthPercent?.toString() ?: getString(R.string.power_unknown_value))
        detailCycleCount.textValue.text = battery?.cycleCount?.let {
            getString(R.string.power_cycle_count_value, it)
        } ?: getString(R.string.power_unknown_value)
        renderCurrentCapacity(
            batteryPercent = battery?.percent,
            ratedCapacityMah = viewModel.ratedCapacityMah.value,
        )
    }

    private fun renderCurrentCapacity(batteryPercent: Int?, ratedCapacityMah: Int?) {
        mBinding.detailCapacity.textValue.text = batteryPercent?.let { percent ->
            getString(
                R.string.power_capacity_value,
                MobilePowerConfig.currentCapacityMah(percent, ratedCapacityMah),
            )
        } ?: getString(R.string.power_unknown_value)
    }

    private fun renderPortCard(binding: ItemPowerPortBinding, port: MobilePowerPortInfo?) {
        val direction = port?.direction ?: PortDirection.NONE
        val isActive = direction == PortDirection.INPUT || direction == PortDirection.OUTPUT
        val metrics = port?.metrics

        // 未连接端口保留白底，以浅色标签和占位数据区分状态。
        binding.root.setBackgroundResource(
            if (isActive) R.drawable.bg_power_port_active else R.drawable.bg_power_port_inactive,
        )
        binding.textStatus.text = when (direction) {
            PortDirection.INPUT -> getString(R.string.power_input)
            PortDirection.OUTPUT -> getString(R.string.power_output)
            PortDirection.NONE -> getString(R.string.power_unknown_value)
        }
        binding.textStatus.setTextColor(
            ContextCompat.getColor(
                this, if (isActive) R.color.color_0752ae else R.color.color_757589
            ),
        )
        binding.textPort.setBackgroundResource(
            if (isActive) R.drawable.bg_power_chip_normal else R.drawable.bg_power_chip_inactive,
        )
        binding.textPort.setTextColor(
            ContextCompat.getColor(
                this, if (isActive) R.color.color_0752ae else R.color.color_ffffff
            ),
        )
        val powerColor = ContextCompat.getColor(
            this, R.color.color_191c1e
        )
        binding.textPower.setContentTextColor(powerColor)
        binding.textPower.setUnitTextColor(powerColor)
        binding.textMetrics.setTextColor(
            ContextCompat.getColor(
                this,
                if (isActive) R.color.color_a5a6aa else R.color.color_9a9da7
            ),
        )
        if (!isActive) {
            binding.textPower.setTvContent(getString(R.string.power_unknown_value))
            binding.textMetrics.text = getString(
                R.string.power_value_voltage_current_text,
                getString(R.string.power_empty_voltage),
                getString(R.string.power_empty_current),
            )
            return
        }

        // Active 时字段可以独立缺失；0 是有效实时值，不能把卡片切为置灰状态。
        binding.textPower.setTvContent(metrics?.powerW?.let { String.format(Locale.US, "%.1f", it.toFloat()) }
            ?: getString(R.string.power_unknown_value))
        binding.textMetrics.text = getString(
            R.string.power_value_voltage_current_text,
            metrics?.voltageMv?.let { getString(R.string.power_value_voltage, it / 1000f) }
                ?: getString(R.string.power_empty_voltage),
            metrics?.currentMa?.let { getString(R.string.power_value_current, it / 1000f) }
                ?: getString(R.string.power_empty_current),
        )
    }

    private fun renderPortSelection() = with(mBinding) {
        listOf(
            Port.C1 to tabC1,
            Port.C2 to tabC2,
            Port.A1 to tabA1,
        ).forEach { (port, tab) ->
            val selected = port == selectedPort
            tab.isSelected = selected
            tab.setTextColor(
                ContextCompat.getColor(
                    this@MobilePowerMainActivity,
                    if (selected) R.color.color_004098 else R.color.color_454558,
                ),
            )
        }
    }

    private fun renderPortDetails(info: MobilePowerHomeInfoBean?) = with(mBinding) {
        val metrics = when (selectedPort) {
            Port.C1 -> info.port(MobilePowerPortType.C1)?.metrics
            Port.C2 -> info.port(MobilePowerPortType.C2)?.metrics
            Port.A1 -> info.port(MobilePowerPortType.USB_A)?.metrics
        }
        detailProtocol.textValue.text =
            metrics?.protocol?.text ?: getString(R.string.power_unknown_value)
        val details = when (selectedPort) {
            Port.C1 -> viewModel.portDetails.value[MobilePowerPortType.C1]
            Port.C2 -> viewModel.portDetails.value[MobilePowerPortType.C2]
            Port.A1 -> null
        }
        detailCable.textValue.text = details?.cable?.let {
            "${it.maxCurrentA}A-${it.maxPowerW}W"
        } ?: getString(R.string.power_unknown_value)
        detailModel.textValue.text = details?.deviceInfo ?: getString(R.string.power_unknown_value)
    }

    private fun MobilePowerHomeInfoBean?.port(type: MobilePowerPortType): MobilePowerPortInfo? =
        this?.ports?.firstOrNull { it.type == type }

    private enum class Port { C1, C2, A1 }

    companion object {
        const val EXTRA_DEVICE_SN = "device_sn"
        private const val DASHBOARD_POLL_INTERVAL_MS = 1_000L
    }
}
