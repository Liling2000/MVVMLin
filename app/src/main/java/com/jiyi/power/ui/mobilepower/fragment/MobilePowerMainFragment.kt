package com.jiyi.power.ui.mobilepower.fragment

import android.os.Bundle
import androidx.fragment.app.activityViewModels
import com.aleyn.mvvm.base.BaseFragment
import com.aleyn.mvvm.extend.flowLaunch
import com.aleyn.mvvm.widget.ContentWithUnitTextView
import com.jiyi.power.databinding.FragmentMobilePowerMainBinding
import com.jiyi.power.ui.mobilepower.viewmodel.C1PortMetricsUiState
import com.jiyi.power.ui.mobilepower.viewmodel.MainFragmentViewModel
import java.util.Locale

class MobilePowerMainFragment : BaseFragment<FragmentMobilePowerMainBinding>() {
    private val viewModel: MainFragmentViewModel by activityViewModels()

    override fun initView(savedInstanceState: Bundle?) {
        super.initView(savedInstanceState)
        mBinding.tvCurrentC1.setTvUnit("A")
        mBinding.tvVoltageC1.setTvUnit("V")
        mBinding.tvPowerC1.setTvUnit("W")
    }

    override fun initObserve() {
        super.initObserve()
        flowLaunch {
            viewModel.c1PortMetrics.collect(::renderC1PortMetrics)
        }
    }

    private fun renderC1PortMetrics(state: C1PortMetricsUiState) {
        mBinding.tvCurrentC1.setNullableContent(state.currentMa) {
            String.format(Locale.US, "%.2f", it / 1000f)
        }
        mBinding.tvVoltageC1.setNullableContent(state.voltageMv) {
            String.format(Locale.US, "%.2f", it / 1000f)
        }
        mBinding.tvPowerC1.setNullableContent(state.powerW) {
            it.toString()
        }
    }

    private fun ContentWithUnitTextView.setNullableContent(value: Int?, formatter: (Int) -> String) {
        setTvContent(value?.let(formatter))
    }
}
