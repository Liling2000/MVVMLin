package com.jiyi.power.app

import com.jiyi.power.app.common.RouterPath

import com.alibaba.android.arouter.facade.annotation.Route

import android.os.Bundle
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.aleyn.mvvm.base.BaseActivity
import com.aleyn.mvvm.widget.ContentWithUnitTextView
import com.jiyi.power.R
import com.jiyi.power.app.bean.BleDeviceStore
import com.jiyi.power.app.bean.PowerStatistics
import com.jiyi.power.app.bean.PowerStatisticsCalculator
import com.jiyi.power.app.bean.PortPowerStatisticsData
import com.jiyi.power.app.bean.StatisticsPort
import com.jiyi.power.app.ble.launchDevicePolling
import com.jiyi.power.app.viewmodel.PortPowerStatisticsViewModel
import com.jiyi.power.app.widget.PowerLineChartView
import com.jiyi.power.databinding.ActivityPortPowerStatisticsBinding
import kotlinx.coroutines.launch

@Route(path = RouterPath.PAGE_PORT_POWER_STATISTICS)
class PortPowerStatisticsActivity : BaseActivity<ActivityPortPowerStatisticsBinding>() {
    private val viewModel by viewModels<PortPowerStatisticsViewModel>()
    private val deviceSn by lazy {
        intent.getStringExtra(MobilePowerMainActivity.EXTRA_DEVICE_SN)
            ?: BleDeviceStore.getDevices().lastOrNull()?.bluetoothSn
    }
    private var selectedPort = StatisticsPort.C1
    private var statisticsData = PortPowerStatisticsData()

    override fun initView(savedInstanceState: Bundle?) {
        selectedPort = intent.getStringExtra(EXTRA_SELECTED_PORT)
            ?.let { value -> StatisticsPort.entries.firstOrNull { it.name == value } }
            ?: StatisticsPort.C1
        mBinding.toolbar.setLeftClickListener { finish() }
        configureValueViews()
        bindTabs()
    }

    override fun initObserve() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.statisticsData.collect { data ->
                    statisticsData = data
                    renderStatistics(
                        PowerStatisticsCalculator.calculate(data.totalDataList),
                        mBinding.totalPeak,
                        mBinding.totalAverage,
                        mBinding.totalChart,
                    )
                    renderSelectedPort()
                }
            }
        }
    }

    override fun initData() {
        viewModel.bindDevice(deviceSn)
        launchDevicePolling(deviceSn, POLLING_INTERVAL_MS) {
            viewModel.requestPortPowers(deviceSn)
        }
    }

    private fun configureValueViews() {
        listOf(mBinding.totalPeak, mBinding.totalAverage, mBinding.portPeak, mBinding.portAverage).forEach { view ->
            view.setTvUnit(getString(R.string.statistics_power_unit))
            view.setContentTextSize(25f)
            view.setUnitTextSize(12f)
            view.setContentTextColor(ContextCompat.getColor(this, R.color.color_191c1e))
            view.setUnitTextColor(ContextCompat.getColor(this, R.color.color_666a7c))
        }
    }

    private fun bindTabs() {
        mBinding.tabC1.setOnClickListener { selectPort(StatisticsPort.C1) }
        mBinding.tabC2.setOnClickListener { selectPort(StatisticsPort.C2) }
        mBinding.tabA1.setOnClickListener { selectPort(StatisticsPort.A1) }
    }

    private fun selectPort(port: StatisticsPort) {
        if (selectedPort == port) return
        selectedPort = port
        renderSelectedPort()
    }

    private fun renderSelectedPort() {
        val tabs = mapOf(
            StatisticsPort.C1 to mBinding.tabC1,
            StatisticsPort.C2 to mBinding.tabC2,
            StatisticsPort.A1 to mBinding.tabA1,
        )
        tabs.forEach { (port, view) -> renderTab(view, port == selectedPort) }
        renderStatistics(
            PowerStatisticsCalculator.calculate(statisticsData.dataList(selectedPort)),
            mBinding.portPeak,
            mBinding.portAverage,
            mBinding.portChart,
        )
    }

    private fun renderTab(view: TextView, selected: Boolean) {
        view.isSelected = selected
        view.setTextColor(
            ContextCompat.getColor(
                this,
                if (selected) R.color.color_004098 else R.color.color_454558,
            ),
        )
    }

    private fun renderStatistics(
        data: PowerStatistics,
        peakView: ContentWithUnitTextView,
        averageView: ContentWithUnitTextView,
        chart: PowerLineChartView
    ) {
        peakView.setTvContent(formatPower(data.peakPower))
        averageView.setTvContent(formatPower(data.averagePower))
        chart.setMaxY(data.maxY)
        chart.setYItemCount(data.yItemCount)
        chart.setXRange(0f, (PowerStatisticsCalculator.MAX_SAMPLE_COUNT - 1).toFloat())
        chart.setXLabels(emptyList())
        chart.setData(data.points)
    }

    private fun formatPower(value: Float) = String.format(java.util.Locale.US, "%.1f", value)

    companion object {
        const val EXTRA_SELECTED_PORT = "selected_port"
        private const val POLLING_INTERVAL_MS = 1_000L
    }
}
