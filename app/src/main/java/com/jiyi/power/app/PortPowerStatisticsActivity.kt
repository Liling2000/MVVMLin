package com.jiyi.power.app

import android.os.Bundle
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.aleyn.mvvm.base.BaseActivity
import com.aleyn.mvvm.R as BaseR
import com.aleyn.mvvm.widget.ContentWithUnitTextView
import com.blankj.utilcode.util.BarUtils
import com.jiyi.power.R
import com.jiyi.power.app.bean.PowerStatistics
import com.jiyi.power.app.bean.PowerStatisticsMockData
import com.jiyi.power.app.widget.PowerLineChartView
import com.jiyi.power.databinding.ActivityPortPowerStatisticsBinding

class PortPowerStatisticsActivity : BaseActivity<ActivityPortPowerStatisticsBinding>() {
    private var selectedPort = "C1"

    override fun initView(savedInstanceState: Bundle?) {
        val background = ContextCompat.getColor(this, R.color.color_f6f7f9)
        BarUtils.setStatusBarColor(this, background)
        BarUtils.setStatusBarLightMode(this, true)
        BarUtils.setNavBarColor(this, background)
        mBinding.toolbar.setLeftClickListener { finish() }
        configureValueViews()
        bindTabs()
        renderStatistics(PowerStatisticsMockData.total, mBinding.totalPeak, mBinding.totalAverage, mBinding.totalChart)
        renderSelectedPort()
    }

    override fun initData() = Unit

    private fun configureValueViews() {
        listOf(mBinding.totalPeak, mBinding.totalAverage, mBinding.portPeak, mBinding.portAverage).forEach { view ->
            view.setTvUnit(getString(R.string.statistics_power_unit))
            view.setContentTextSize(25f)
            view.setUnitTextSize(12f)
            view.setContentTextColor(ContextCompat.getColor(this, BaseR.color.color_17181c))
            view.setUnitTextColor(ContextCompat.getColor(this, R.color.color_666a7c))
        }
    }

    private fun bindTabs() {
        mBinding.tabC1.setOnClickListener { selectPort("C1") }
        mBinding.tabC2.setOnClickListener { selectPort("C2") }
        mBinding.tabC3.setOnClickListener { selectPort("C3") }
        mBinding.tabA1.setOnClickListener { selectPort("A1") }
    }

    private fun selectPort(port: String) {
        if (selectedPort == port) return
        selectedPort = port
        renderSelectedPort()
    }

    private fun renderSelectedPort() {
        val tabs = mapOf("C1" to mBinding.tabC1, "C2" to mBinding.tabC2, "C3" to mBinding.tabC3, "A1" to mBinding.tabA1)
        tabs.forEach { (port, view) -> renderTab(view, port == selectedPort) }
        PowerStatisticsMockData.ports[selectedPort]?.let {
            renderStatistics(it, mBinding.portPeak, mBinding.portAverage, mBinding.portChart)
        }
    }

    private fun renderTab(view: TextView, selected: Boolean) {
        view.setBackgroundResource(if (selected) R.drawable.bg_statistics_tab_selected else android.R.color.transparent)
        view.setTextColor(ContextCompat.getColor(this, if (selected) R.color.color_0752ae else R.color.color_55596a))
        view.elevation = if (selected) resources.displayMetrics.density * 2f else 0f
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
        chart.setXRange(0f, 24f)
        chart.setXLabels(PowerStatisticsMockData.xLabels)
        chart.setData(data.points)
    }

    private fun formatPower(value: Float) = String.format(java.util.Locale.US, "%.1f", value)
}
