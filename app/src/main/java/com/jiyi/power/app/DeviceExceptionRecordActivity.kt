package com.jiyi.power.app

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.aleyn.mvvm.base.BaseActivity
import com.jiyi.power.R
import com.jiyi.power.app.adapter.DeviceExceptionAdapter
import com.jiyi.power.app.bean.DeviceExceptionRecord
import com.jiyi.power.app.error.protectionDetail
import com.jiyi.power.app.error.protectionTitleResource
import com.jiyi.power.app.viewmodel.DeviceExceptionUiState
import com.jiyi.power.app.viewmodel.DeviceExceptionViewModel
import com.jiyi.power.databinding.ActivityDeviceExceptionRecordBinding
import kotlinx.coroutines.launch

class DeviceExceptionRecordActivity : BaseActivity<ActivityDeviceExceptionRecordBinding>() {
    private val viewModel by viewModels<DeviceExceptionViewModel>()
    private val adapter = DeviceExceptionAdapter(::showProtectionDetail)

    override fun initSystemBars() {
        setSystemBars(statusBarColorRes = R.color.color_f7f9fb)
    }

    override fun initView(savedInstanceState: Bundle?) {
        mBinding.toolbar.setLeftClickListener { finish() }
        mBinding.errorList.layoutManager = LinearLayoutManager(this)
        mBinding.errorList.adapter = adapter
        mBinding.errorList.itemAnimator = null
        mBinding.buttonRecheck.setOnClickListener { viewModel.refresh() }
        observeState()
    }

    override fun initData() {
        viewModel.bindDevice(intent.getStringExtra(MobilePowerMainActivity.EXTRA_DEVICE_SN))
    }

    override fun onStart() {
        super.onStart()
        viewModel.refresh()
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.uiState.collect(::render) }
        }
    }

    private fun render(state: DeviceExceptionUiState) = with(mBinding) {
        adapter.submitList(state.records)
        buttonRecheck.isEnabled = !state.isLoading
        buttonRecheck.alpha = if (state.isLoading) 0.6f else 1f
        textRecheck.setText(if (state.isLoading) R.string.protection_checking else R.string.protection_recheck)
        queryMessage.isVisible = state.messageRes != null
        state.messageRes?.let(queryMessage::setText)
        logSummary.text = state.totalCount?.let {
            getString(R.string.protection_log_progress, state.records.size, it)
        } ?: getString(R.string.protection_log_history_notice)
        disabledNotice.isVisible = state.disabled == true
    }

    private fun showProtectionDetail(item: DeviceExceptionRecord) {
        AlertDialog.Builder(this)
            .setTitle(protectionTitleResource(item.typeCode))
            .setMessage(protectionDetail(this, item))
            .setPositiveButton(R.string.protection_detail_close, null)
            .show()
    }
}
