package com.jiyi.power.app

import com.jiyi.power.app.common.RouterPath

import com.alibaba.android.arouter.facade.annotation.Route

import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.aleyn.mvvm.base.BaseActivity
import com.blankj.utilcode.util.ToastUtils
import com.jiyi.power.R
import com.jiyi.power.app.adapter.PowerChannelAdapter
import com.jiyi.power.app.common.ChargingPreferences
import com.jiyi.power.app.viewmodel.ChargingModeViewModel
import com.jiyi.power.app.viewmodel.CustomChargingModeViewModel
import com.jiyi.power.databinding.ActivityCustomChargingModeBinding
import kotlinx.coroutines.launch

@Route(path = RouterPath.PAGE_CUSTOM_CHARGING_MODE)
class CustomChargingModeActivity : BaseActivity<ActivityCustomChargingModeBinding>() {
    companion object {
        const val EXTRA_MODE_ID = "extra_custom_mode_id"
    }

    private val viewModel: CustomChargingModeViewModel by viewModels()
    private val chargingModeViewModel: ChargingModeViewModel by viewModels()
    private val channelAdapter =
        PowerChannelAdapter { index, delta -> viewModel.adjustPower(index, delta) }
    private var modeId: Long? = null

    override fun initSystemBars() {
        setSystemBars(statusBarColorRes = R.color.color_f6f7f9)
    }

    override fun initView(savedInstanceState: Bundle?) {
        with(mBinding) {
            modeId = intent.getLongExtra(EXTRA_MODE_ID, -1L).takeIf { it > 0 }
            toolbar.setLeftClickListener { finish() }
            toolbar.setRightIconClickListener { saveMode() }
            recyclerChannels.layoutManager = LinearLayoutManager(this@CustomChargingModeActivity)
            recyclerChannels.adapter = channelAdapter
            viewModel.load(modeId)
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.channels.collect { channels ->
                        channelAdapter.submitList(channels)
                        textTotalPower.text = getString(
                            R.string.custom_mode_total_power_value, channels.sumOf { it.power })
                    }
                }
            }
            modeId?.let {
                CustomChargingModeRepository.findById(it)
                    ?.let { mode -> editModeName.setText(mode.name) }
            }
        }
    }

    override fun initData() = Unit

    private fun saveMode() {
        val name = mBinding.editModeName.text?.toString()?.trim().orEmpty()
        if (name.isBlank()) {
            mBinding.editModeName.error = getString(R.string.custom_mode_name_required)
            return
        }
        lifecycleScope.launch {
            val editingModeId = modeId
            val isCreating = editingModeId == null
            val isEditingSelectedMode = editingModeId != null &&
                CustomChargingModeRepository.selectedId() == editingModeId
            val shouldApplyToDevice = isCreating || isEditingSelectedMode

            if (shouldApplyToDevice) {
                showLoading(getString(R.string.setting_in_progress))
                try {
                    val deviceSn = intent.getStringExtra(MobilePowerMainActivity.EXTRA_DEVICE_SN)
                    viewModel.bindDevice(deviceSn)
                    chargingModeViewModel.bindDevice(deviceSn)
                    if (!viewModel.applyPower() ||
                        !chargingModeViewModel.applyMode(ChargingPreferences.MODE_CUSTOM)) {
                        ToastUtils.showShort(R.string.power_command_failed)
                        return@launch
                    }
                } finally {
                    dismissLoading()
                }
            }

            val mode = viewModel.toMode(editingModeId, name)
            CustomChargingModeRepository.save(mode)
            if (isCreating) CustomChargingModeRepository.select(mode.id)
            if (shouldApplyToDevice) saveCustomModePreference()
            ToastUtils.showShort(R.string.custom_mode_saved)
            finish()
        }
    }

    private fun saveCustomModePreference() {
        getSharedPreferences(ChargingPreferences.FILE_NAME, MODE_PRIVATE).edit()
            .putInt(ChargingPreferences.KEY_MODE, ChargingPreferences.MODE_CUSTOM)
            .putBoolean(ChargingPreferences.KEY_LEGACY_SMART_MODE, false)
            .apply()
    }
}
