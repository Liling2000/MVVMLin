package com.jiyi.power.ui.device

import android.graphics.Rect
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.android.arouter.facade.annotation.Route
import com.aleyn.mvvm.base.BaseVMActivity
import com.alibaba.android.arouter.launcher.ARouter
import com.blankj.utilcode.util.BarUtils
import com.jiyi.power.R
import com.jiyi.power.databinding.ActivityDeviceHomeBinding
import com.jiyi.power.router.RouterPath
import kotlinx.coroutines.launch

@Route(path = RouterPath.DEVICE_HOME)
class DeviceHomeActivity : BaseVMActivity<DeviceHomeViewModel, ActivityDeviceHomeBinding>() {

    private val featureAdapter = DeviceFeatureAdapter()

    override fun initView(savedInstanceState: Bundle?) {
        BarUtils.setStatusBarColor(this, ContextCompat.getColor(this, R.color.device_header_start))
        mBinding.ivBack.setOnClickListener { finish() }
        mBinding.rvFeatures.apply {
            layoutManager = GridLayoutManager(this@DeviceHomeActivity, FEATURE_SPAN_COUNT)
            adapter = featureAdapter
            isNestedScrollingEnabled = false
            itemAnimator = null
            addItemDecoration(
                FeatureGridSpacingDecoration(
                    FEATURE_SPAN_COUNT,
                    resources.getDimensionPixelSize(R.dimen.dp_12)
                )
            )
        }
        mBinding.modeGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rb_mode_normal -> viewModel.selectNoiseMode(NoiseMode.NORMAL)
                R.id.rb_mode_transparency -> viewModel.selectNoiseMode(NoiseMode.TRANSPARENCY)
                R.id.rb_mode_anc -> viewModel.selectNoiseMode(NoiseMode.ANC)
            }
        }
        mBinding.switchDolby.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setDolbyEnabled(isChecked)
        }
    }

    override fun initObserve() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    override fun initData() {
        viewModel.loadDevice(intent.getStringExtra(EXTRA_DEVICE_NAME) ?: DEFAULT_DEVICE_NAME)
    }

    private fun render(state: DeviceHomeUiState) = with(mBinding) {
        tvDeviceName.text = state.deviceName
        tvBattery.text = state.batteryText
        tvConnection.text = state.connectionText
        featureAdapter.submitList(state.features)
        val targetModeId = when (state.selectedNoiseMode) {
            NoiseMode.NORMAL -> R.id.rb_mode_normal
            NoiseMode.TRANSPARENCY -> R.id.rb_mode_transparency
            NoiseMode.ANC -> R.id.rb_mode_anc
        }
        if (modeGroup.checkedRadioButtonId != targetModeId) {
            modeGroup.check(targetModeId)
        }
        if (switchDolby.isChecked != state.dolbyEnabled) {
            switchDolby.isChecked = state.dolbyEnabled
        }
        tvDolbyState.text = if (state.dolbyEnabled) "已开启" else "已关闭"
    }

    private class FeatureGridSpacingDecoration(
        private val spanCount: Int,
        private val spacing: Int
    ) : RecyclerView.ItemDecoration() {

        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            val position = parent.getChildAdapterPosition(view)
            if (position == RecyclerView.NO_POSITION) return
            val column = position % spanCount
            outRect.left = column * spacing / spanCount
            outRect.right = spacing - (column + 1) * spacing / spanCount
            if (position >= spanCount) {
                outRect.top = spacing
            }
        }
    }

    companion object {
        const val EXTRA_DEVICE_NAME = "device_name"
        private const val DEFAULT_DEVICE_NAME = "Inspire XH1"
        private const val FEATURE_SPAN_COUNT = 2
    }
}