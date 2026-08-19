package com.jiyi.power.app

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.aleyn.mvvm.base.BaseActivity
import com.blankj.utilcode.util.BarUtils
import com.jiyi.power.R
import com.jiyi.power.app.adapter.DeviceExceptionAdapter
import com.jiyi.power.app.viewmodel.DeviceExceptionUiState
import com.jiyi.power.app.viewmodel.DeviceExceptionViewModel
import com.jiyi.power.databinding.ActivityDeviceExceptionRecordBinding
import kotlinx.coroutines.launch

class DeviceExceptionRecordActivity : BaseActivity<ActivityDeviceExceptionRecordBinding>() {
    private val viewModel by viewModels<DeviceExceptionViewModel>()
    private val adapter = DeviceExceptionAdapter()

    override fun initView(savedInstanceState: Bundle?) {
        val background = ContextCompat.getColor(this, R.color.color_f6f7f9)
        BarUtils.setStatusBarColor(this, background)
        BarUtils.setStatusBarLightMode(this, true)
        BarUtils.setNavBarColor(this, background)
        mBinding.buttonBack.setOnClickListener { finish() }
        mBinding.errorList.layoutManager = LinearLayoutManager(this)
        mBinding.errorList.adapter = adapter
        observeState()
    }

    override fun initData() = viewModel.loadRecords()

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.uiState.collect(::render) }
        }
    }

    private fun render(state: DeviceExceptionUiState) = with(mBinding) {
        adapter.submitList(state.records)
        errorList.visibility = if (state.records.isEmpty()) View.GONE else View.VISIBLE
        emptyState.visibility = if (state.records.isEmpty()) View.VISIBLE else View.GONE
    }
}
