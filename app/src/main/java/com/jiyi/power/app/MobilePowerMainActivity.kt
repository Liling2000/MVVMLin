package com.jiyi.power.app

import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.aleyn.mvvm.base.BaseActivity
import com.aleyn.mvvm.R as BaseR
import com.aleyn.mvvm.extend.flowLaunch
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.blankj.utilcode.util.BarUtils
import com.blankj.utilcode.util.ToastUtils
import com.jiyi.power.R
import com.jiyi.power.app.bean.BleDeviceStore
import com.jiyi.power.app.common.RouterPath
import com.jiyi.power.databinding.ActivityMobilePowerMainBinding
import com.jiyi.power.databinding.ItemPowerPortBinding
import com.jiyi.power.app.bean.MobilePowerHomeInfoBean
import com.jiyi.power.app.bean.MobilePowerPortInfo
import com.jiyi.power.app.bean.MobilePowerPortType
import com.jiyi.power.app.bean.PortMetrics
import com.jiyi.power.app.ble.BleConnectionCoordinator
import com.jiyi.power.app.ble.BleConnectionEvent
import com.jiyi.power.app.ble.BleIoEvent
import com.jiyi.power.app.ble.DeviceConnectionState
import com.jiyi.power.app.viewmodel.MainFragmentViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.Locale

@Route(path = RouterPath.MOBILE_POWER_MAIN)
class MobilePowerMainActivity : BaseActivity<ActivityMobilePowerMainBinding>() {
    private val viewModel: MainFragmentViewModel by viewModels()
    private var selectedPort = Port.C1
    private var renderingSwitch = false
    private var pendingLowCurrentMode: Boolean? = null
    private val deviceSn: String? by lazy {
        intent.getStringExtra(EXTRA_DEVICE_SN) ?: BleDeviceStore.getDevices()
            .lastOrNull()?.bluetoothSn
    }

    override fun initView(savedInstanceState: Bundle?) {
        BarUtils.setStatusBarColor(this, ContextCompat.getColor(this, R.color.color_f6f7f9))
        BarUtils.setStatusBarLightMode(this, true)
        BarUtils.setNavBarColor(this, ContextCompat.getColor(this, R.color.color_f6f7f9))
        setupStaticContent()
        setupClicks()
        setupBleNotifications()
        render(null)
    }

    override fun initObserve() {
        flowLaunch {
            viewModel.homeInfo.collect(::render)
        }
    }

    override fun initData() {
        val connected = BleConnectionCoordinator.connectionStates.value.any { (sn, state) ->
            sn.equals(deviceSn, ignoreCase = true) && state == DeviceConnectionState.CONNECTED
        }
        if (connected) viewModel.requestDashboard(deviceSn)
    }

    private fun setupStaticContent() = with(mBinding) {
        setupPortCard(cardC1, R.string.power_port_c1)
        setupPortCard(cardC2, R.string.power_port_c2)
        setupPortCard(cardC3, R.string.power_port_c3)
        setupPortCard(cardA1, R.string.power_port_a1)

        detailCable.textLabel.setText(R.string.power_cable_info)
        detailCable.textValue.setText(R.string.power_cable_value)
        detailCable.imageIcon.setImageResource(R.mipmap.ic_power_cable)
        detailProtocol.textLabel.setText(R.string.power_charge_protocol)
        detailProtocol.imageIcon.setImageResource(R.mipmap.ic_lightning)
        detailModel.textLabel.setText(R.string.power_device_model)
        detailModel.imageIcon.setImageResource(R.mipmap.ic_mobile_device)

        detailCycleCount.textLabel.setText(R.string.power_cycle_count)
        detailCapacity.textLabel.setText(R.string.power_current_capacity)
    }

    private fun setupClicks() = with(mBinding) {
        toolbar.setLeftClickListener { finish() }
        toolbar.setRightIconClickListener {
            ARouter.getInstance().build(RouterPath.DEVICE_SETTING)
                .withString(EXTRA_DEVICE_SN, deviceSn).navigation()
        }
        cardC1.root.setOnClickListener { selectPort(Port.C1) }
        cardC2.root.setOnClickListener { selectPort(Port.C2) }
        cardC3.root.setOnClickListener { selectPort(Port.C3) }
        cardA1.root.setOnClickListener { selectPort(Port.A1) }
        tabC1.setOnClickListener { selectPort(Port.C1) }
        tabC2.setOnClickListener { selectPort(Port.C2) }
        tabC3.setOnClickListener { selectPort(Port.C3) }
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
            ARouter.getInstance().build(ROUTE_THEME).navigation()
        }
        cardBatteryInfo.setOnClickListener {
            startActivity(android.content.Intent(this@MobilePowerMainActivity, BatteryInfoActivity::class.java))
        }
    }

    private fun setupPortCard(binding: ItemPowerPortBinding, labelRes: Int) {
        binding.textPort.setText(labelRes)
    }

    private fun setupBleNotifications() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    BleConnectionCoordinator.ioEvents.collect { event ->
                        when (event) {
                            is BleIoEvent.Notification -> if (
                                deviceSn == null || event.sn.equals(deviceSn, ignoreCase = true)
                            ) {
                                viewModel.onBleDataReceive(
                                    event.data.joinToString("") { byte ->
                                        "%02X".format(byte.toInt() and 0xFF)
                                    },
                                )
                            }

                            is BleIoEvent.WriteResult -> if (
                                event.sn.equals(deviceSn, ignoreCase = true) &&
                                event.data?.getOrNull(2)?.toInt()?.and(0xFF) == 0x34
                            ) {
                                if (!event.success) {
                                    pendingLowCurrentMode = null
                                    ToastUtils.showShort(R.string.power_command_failed)
                                }
                                delay(150L)
                                viewModel.requestDashboard(deviceSn)
                                mBinding.switchLowCurrent.isEnabled = true
                            }

                            else -> Unit
                        }
                    }
                }
                launch {
                    BleConnectionCoordinator.connectionEvents.collect { event ->
                        when (event) {
                            is BleConnectionEvent.Connected -> {
                                if (event.device.bluetoothSn.equals(deviceSn, ignoreCase = true)) {
                                    viewModel.requestDashboard(deviceSn)
                                }
                            }

                            is BleConnectionEvent.Disconnected -> {
                                if (deviceSn == null || event.sn.equals(deviceSn, ignoreCase = true)) {
                                    ToastUtils.showShort(R.string.power_device_disconnected_notice)
                                }
                            }
                        }
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

        renderPortCard(cardC1, Port.C1, info.port(MobilePowerPortType.C1)?.metrics)
        renderPortCard(cardC2, Port.C2, info.port(MobilePowerPortType.C2)?.metrics)
        renderPortCard(cardC3, Port.C3, null)
        renderPortCard(cardA1, Port.A1, info.port(MobilePowerPortType.USB_A)?.metrics)
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
        textBatteryHealth.setTvContent((battery?.healthPercent ?: 0).toString())
        detailCycleCount.textValue.text = getString(
            R.string.power_cycle_count_value,
            battery?.cycleCount ?: 0,
        )
        detailCapacity.textValue.text = getString(R.string.power_capacity_value, 25000)
    }

    private fun renderPortCard(binding: ItemPowerPortBinding, port: Port, metrics: PortMetrics?) {
        val hasOutput = (metrics?.powerW ?: 0) > 0 || (metrics?.currentMa ?: 0) > 0
        binding.root.setBackgroundResource(
            if (hasOutput) R.drawable.bg_power_port_active else R.drawable.bg_power_port_inactive,
        )
        binding.textStatus.text =
            getString(if (hasOutput) R.string.power_output else R.string.power_disconnected)
        binding.textStatus.setTextColor(
            ContextCompat.getColor(
                this, if (hasOutput) R.color.color_0752ae else R.color.color_9a9da7
            ),
        )
        binding.textPort.setTextColor(
            ContextCompat.getColor(
                this, if (hasOutput) R.color.color_0752ae else R.color.color_9a9da7
            ),
        )
        val powerColor = ContextCompat.getColor(
            this, if (hasOutput) R.color.color_191c1e else R.color.color_9a9da7
        )
        binding.textPower.setContentTextColor(powerColor)
        binding.textPower.setUnitTextColor(powerColor)
        binding.textDevice.setTextColor(
            ContextCompat.getColor(
                this,
                if (hasOutput) R.color.color_77798d else R.color.color_9a9da7
            ),
        )
        binding.textMetrics.setTextColor(
            ContextCompat.getColor(
                this,
                if (hasOutput) R.color.color_a5a6aa else R.color.color_9a9da7
            ),
        )
        binding.textPower.setTvContent(
            metrics?.powerW?.let { String.format(Locale.US, "%.1f", it.toFloat()) })
        binding.textDevice.text = when {
            !hasOutput -> getString(R.string.power_disconnected)
            port == Port.C1 -> getString(R.string.power_device_c1_name)
            port == Port.C2 -> getString(R.string.power_device_c2_name)
            else -> getString(R.string.power_device_a1_name)
        }
        binding.textMetrics.text = getString(
            R.string.power_value_voltage_current,
            (metrics?.voltageMv ?: 0) / 1000f,
            (metrics?.currentMa ?: 0) / 1000f,
        )
    }

    private fun renderPortSelection() = with(mBinding) {
        listOf(
            Port.C1 to tabC1,
            Port.C2 to tabC2,
            Port.C3 to tabC3,
            Port.A1 to tabA1,
        ).forEach { (port, tab) ->
            val selected = port == selectedPort
            tab.setBackgroundResource(
                if (selected) R.drawable.bg_power_chip_selected else R.drawable.bg_power_chip_normal,
            )
            tab.setTextColor(
                ContextCompat.getColor(
                    this@MobilePowerMainActivity,
                    if (selected) BaseR.color.color_ffffff else R.color.color_77798d,
                ),
            )
        }
    }

    private fun renderPortDetails(info: MobilePowerHomeInfoBean?) = with(mBinding) {
        val metrics = when (selectedPort) {
            Port.C1 -> info.port(MobilePowerPortType.C1)?.metrics
            Port.C2 -> info.port(MobilePowerPortType.C2)?.metrics
            Port.C3 -> null
            Port.A1 -> info.port(MobilePowerPortType.USB_A)?.metrics
        }
        detailProtocol.textValue.text =
            metrics?.protocol?.text ?: getString(R.string.power_unknown_value)
        detailModel.textValue.text = when (selectedPort) {
            Port.C1 -> getString(R.string.power_model_c1)
            Port.C2 -> getString(R.string.power_model_c2)
            Port.C3, Port.A1 -> getString(R.string.power_unknown_value)
        }
    }

    private fun MobilePowerHomeInfoBean?.port(type: MobilePowerPortType): MobilePowerPortInfo? =
        this?.ports?.firstOrNull { it.type == type }

    private enum class Port { C1, C2, C3, A1 }

    companion object {
        const val EXTRA_DEVICE_SN = "device_sn"
        private const val ROUTE_THEME = "/mobilepower/theme_choose"
    }
}
