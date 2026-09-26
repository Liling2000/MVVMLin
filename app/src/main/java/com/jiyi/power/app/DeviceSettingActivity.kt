package com.jiyi.power.app

import com.alibaba.android.arouter.launcher.ARouter

import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.aleyn.mvvm.base.BaseActivity
import com.alibaba.android.arouter.facade.annotation.Route
import com.blankj.utilcode.util.ToastUtils
import com.jiyi.power.R
import com.jiyi.power.app.ble.BleConnectionCoordinator
import com.jiyi.power.app.common.ChargingPreferences
import com.jiyi.power.app.common.RouterPath
import com.jiyi.power.app.viewmodel.DeviceSettingViewModel
import com.jiyi.power.app.viewmodel.ChargingModeViewModel
import com.jiyi.power.app.widget.popup.AppPopupManager
import com.jiyi.power.databinding.ActivityDeviceSettingBinding
import kotlinx.coroutines.launch

@Route(path = RouterPath.PAGE_DEVICE_SETTING)
class DeviceSettingActivity : BaseActivity<ActivityDeviceSettingBinding>() {

    private val viewModel by viewModels<DeviceSettingViewModel>()
    private val chargingModeViewModel by viewModels<ChargingModeViewModel>()

    private val preferences by lazy { getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE) }
    private val deviceSn by lazy { intent.getStringExtra(MobilePowerMainActivity.EXTRA_DEVICE_SN) }

    override fun initView(savedInstanceState: Bundle?) {
        renderStoredValues()
        setupClicks()
    }

    override fun initData() = Unit

    override fun onResume() {
        super.onResume()
        renderStoredValues()
        refreshChargingMode()
    }

    private fun renderStoredValues() = with(mBinding) {
        rowDeviceName.setRightTextValue(preferences.getString(KEY_NAME, getString(R.string.device_setting_default_name)))
        val mode = if (preferences.contains(ChargingPreferences.KEY_MODE)) {
            preferences.getInt(ChargingPreferences.KEY_MODE, ChargingPreferences.MODE_SMART)
        } else if (preferences.getBoolean(KEY_SMART_MODE, true)) ChargingPreferences.MODE_SMART else ChargingPreferences.MODE_STANDARD
        rowChargeMode.setRightTextValue(getString(when (mode) {
            ChargingPreferences.MODE_STANDARD -> R.string.charging_mode_standard
            ChargingPreferences.MODE_CUSTOM -> R.string.charging_mode_custom
            else -> R.string.charging_mode_smart
        }))
    }

    private fun setupClicks() = with(mBinding) {
        toolbar.setLeftClickListener { finish() }
        rowChargeMode.setOnClickListener {
            ARouter.getInstance().build(RouterPath.PAGE_ROUTE_CHARGING_MODE)
                .withString(MobilePowerMainActivity.EXTRA_DEVICE_SN, deviceSn)
                .navigation(this@DeviceSettingActivity)
        }
        rowDeviceName.setOnClickListener { showRenameDialog() }
        rowCertification.setOnClickListener {
            ARouter.getInstance().build(RouterPath.PAGE_CERTIFICATION)
                .navigation(this@DeviceSettingActivity)
        }
        rowFactoryReset.setOnClickListener { showFactoryResetDialog() }
        buttonDeleteDevice.setOnClickListener { showDeleteDialog() }
    }

    private fun showRenameDialog() {
        val input = EditText(this).apply {
            setText(mBinding.rowDeviceName.getRightTextValue())
            hint = getString(R.string.device_setting_name_hint)
            inputType = InputType.TYPE_CLASS_TEXT
            filters = arrayOf(InputFilter.LengthFilter(24))
            setSelectAllOnFocus(true)
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.device_setting_edit_name_title)
            .setView(input)
            .setNegativeButton(R.string.device_setting_cancel, null)
            .setPositiveButton(R.string.device_setting_confirm, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = input.text.toString().trim()
                if (name.isEmpty()) {
                    input.error = getString(R.string.device_setting_name_empty)
                } else {
                    preferences.edit().putString(KEY_NAME, name).apply()
                    mBinding.rowDeviceName.setRightTextValue(name)
                    dialog.dismiss()
                }
            }
        }
        dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        dialog.show()
    }

    private fun showFactoryResetDialog() {
        AppPopupManager.showRestoreFactorySettings(
            context = this,
            onConfirm = {
                viewModel.bindDevice(deviceSn)
                if (viewModel.restoreFactorySettings()) {
                    preferences.edit().clear().apply()
                    renderStoredValues()
                    ToastUtils.showShort(R.string.device_setting_factory_reset_success)
                } else ToastUtils.showShort(R.string.power_command_failed)
            },
        )
    }

    private fun refreshChargingMode() {
        chargingModeViewModel.bindDevice(deviceSn)
        lifecycleScope.launch {
            val state = chargingModeViewModel.readDeviceState() ?: return@launch
            val mode = state.mode ?: return@launch
            if (mode != ChargingPreferences.MODE_CUSTOM) {
                CustomChargingModeRepository.clearSelection()
            }
            preferences.edit()
                .putInt(ChargingPreferences.KEY_MODE, mode)
                .putBoolean(
                    ChargingPreferences.KEY_LEGACY_SMART_MODE,
                    mode == ChargingPreferences.MODE_SMART,
                )
                .apply()
            val label = when (mode) {
                ChargingPreferences.MODE_STANDARD -> getString(R.string.charging_mode_standard)
                ChargingPreferences.MODE_CUSTOM -> state.customPower
                    ?.let {
                        CustomChargingModeRepository.resolveSelected(it.c1Power, it.c2Power)?.name
                    }
                    ?: run {
                        CustomChargingModeRepository.clearSelection()
                        getString(R.string.charging_mode_custom)
                    }
                else -> getString(R.string.charging_mode_smart)
            }
            mBinding.rowChargeMode.setRightTextValue(label)
        }
    }

    private fun showDeleteDialog() {
        AppPopupManager.showDeleteDevice(
            context = this,
            onConfirm = {
                BleConnectionCoordinator.disconnectAndRemoveDevice(deviceSn)
                preferences.edit().clear().apply()
                ToastUtils.showShort(R.string.device_setting_delete_success)
                ARouter.getInstance().build(RouterPath.PAGE_MAIN)
                    .withFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .navigation(this)
                finish()
            },
        )
    }

    companion object {
        private const val PREFERENCES_NAME = "device_setting"
        private const val KEY_NAME = "device_name"
        private const val KEY_SMART_MODE = "smart_mode"
    }
}
