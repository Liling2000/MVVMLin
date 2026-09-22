package com.jiyi.power.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.recyclerview.widget.LinearLayoutManager
import com.aleyn.mvvm.base.BaseActivity
import com.alibaba.android.arouter.facade.annotation.Route
import com.jiyi.power.R
import com.jiyi.power.app.adapter.CustomChargingModeAdapter
import com.jiyi.power.app.common.ChargingPreferences
import com.jiyi.power.app.common.RouterPath
import com.jiyi.power.app.viewmodel.ChargingModeViewModel
import com.jiyi.power.databinding.ActivityCustomChargingModeListBinding

@Route(path = RouterPath.ROUTE_MODE_LIST)
class CustomChargingModeListActivity : BaseActivity<ActivityCustomChargingModeListBinding>() {
    private val viewModel by viewModels<ChargingModeViewModel>()
    private val modeAdapter = CustomChargingModeAdapter(
        onSelect = { mode -> selectMode(mode.id) },
        onEdit = { mode -> openEditor(mode.id) },
    )

    override fun initSystemBars() {
        setSystemBars(statusBarColorRes = R.color.custom_mode_page_background)
    }

    override fun initView(savedInstanceState: Bundle?) = with(mBinding) {
        toolbar.setLeftClickListener { finish() }
        recyclerModes.layoutManager = LinearLayoutManager(this@CustomChargingModeListActivity)
        recyclerModes.adapter = modeAdapter
        buttonAddMode.setOnClickListener { openEditor(null) }
    }

    override fun onResume() {
        super.onResume(); renderModes()
    }

    override fun initData() {
        viewModel.bindDevice(intent.getStringExtra(MobilePowerMainActivity.EXTRA_DEVICE_SN))
    }

    private fun renderModes() = modeAdapter.submitList(
        CustomChargingModeRepository.getAll(),
        CustomChargingModeRepository.selectedId(),
    )

    private fun selectMode(id: Long) {
        if (viewModel.uiState.value.isBusy) return
        lifecycleScope.launch {
            if (!viewModel.applyMode(ChargingPreferences.MODE_CUSTOM)) {
                com.blankj.utilcode.util.ToastUtils.showShort(R.string.power_command_failed)
                return@launch
            }
            CustomChargingModeRepository.select(id)
            getSharedPreferences(ChargingPreferences.FILE_NAME, MODE_PRIVATE).edit()
                .putInt(ChargingPreferences.KEY_MODE, ChargingPreferences.MODE_CUSTOM)
                .putBoolean(ChargingPreferences.KEY_LEGACY_SMART_MODE, false).apply()
            renderModes()
        }
    }

    private fun openEditor(id: Long?) {
        startActivity(Intent(this, CustomChargingModeActivity::class.java).apply {
            id?.let { putExtra(CustomChargingModeActivity.EXTRA_MODE_ID, it) }
        })
    }
}
