package com.jiyi.power.app

import android.bluetooth.BluetoothDevice
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
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
import com.jiyi.power.app.bean.MobilePowerSnapshot
import com.jiyi.power.app.bean.PortMetrics
import com.jiyi.power.app.viewmodel.MainFragmentViewModel
import com.liling.ble.constant.BleConstant
import com.liling.ble.listener.BleDataListener
import com.liling.ble.manager.Ble
import java.util.Locale

@Route(path = RouterPath.MOBILE_POWER_MAIN)
class MobilePowerMainActivity : BaseActivity<ActivityMobilePowerMainBinding>() {

    private val viewModel: MainFragmentViewModel by viewModels()
    private var selectedPort = Port.C1
    private var renderingSwitch = false
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
            viewModel.dashboardSnapshot.collect(::render)
        }
    }

    override fun initData() {
        viewModel.requestDashboard(deviceSn)
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
                ToastUtils.showShort(
                    getString(
                        R.string.power_low_current_updated,
                        getString(if (checked) R.string.power_enabled else R.string.power_disabled),
                    ),
                )
            } else {
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
        Ble.getBleApi().setOnBleDataListener(object : BleDataListener {
            override fun onBleNotify(device: BluetoothDevice, data: ByteArray) {
                if (deviceSn != null && device.address != deviceSn) return
                viewModel.onBleDataReceive(
                    data.joinToString(separator = "") { byte -> "%02X".format(byte.toInt() and 0xFF) },
                )
            }

            override fun sendConnectState(device: BluetoothDevice, state: Int) {
                if (deviceSn != null && device.address != deviceSn) return
                if (state == BleConstant.BleConnectState.stateDisconnected) {
                    runOnUiThread {
                        ToastUtils.showShort(R.string.power_device_disconnected_notice)
                    }
                }
            }

            override fun sendWriteMsg(device: BluetoothDevice, state: Int) = Unit
            override fun mtuResponse(sn: String, state: Int, mtu: Int) = Unit
            override fun onRssiResponse(sn: String, model: String, rssi: Int) = Unit
        })
    }

    private fun selectPort(port: Port) {
        selectedPort = port
        renderPortSelection()
        renderPortDetails(viewModel.dashboardSnapshot.value)
    }

    private fun render(snapshot: MobilePowerSnapshot?) = with(mBinding) {
        val battery = snapshot?.battery
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

        renderPortCard(cardC1, Port.C1, snapshot?.c1)
        renderPortCard(cardC2, Port.C2, snapshot?.c2)
        renderPortCard(cardC3, Port.C3, null)
        renderPortCard(cardA1, Port.A1, snapshot?.usbA)
        renderPortSelection()
        renderPortDetails(snapshot)

        renderingSwitch = true
        switchLowCurrent.setEnableEffect(false)
        switchLowCurrent.isChecked = snapshot?.settings?.lowCurrentMode == true
        switchLowCurrent.setEnableEffect(true)
        renderingSwitch = false
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

    private fun renderPortDetails(snapshot: MobilePowerSnapshot?) = with(mBinding) {
        val metrics = when (selectedPort) {
            Port.C1 -> snapshot?.c1
            Port.C2 -> snapshot?.c2
            Port.C3 -> null
            Port.A1 -> snapshot?.usbA
        }
        detailProtocol.textValue.text =
            metrics?.protocol?.text ?: getString(R.string.power_unknown_value)
        detailModel.textValue.text = when (selectedPort) {
            Port.C1 -> getString(R.string.power_model_c1)
            Port.C2 -> getString(R.string.power_model_c2)
            Port.C3, Port.A1 -> getString(R.string.power_unknown_value)
        }
    }

    private enum class Port { C1, C2, C3, A1 }

    companion object {
        const val EXTRA_DEVICE_SN = "device_sn"
        private const val ROUTE_THEME = "/mobilepower/theme_choose"
    }
}
