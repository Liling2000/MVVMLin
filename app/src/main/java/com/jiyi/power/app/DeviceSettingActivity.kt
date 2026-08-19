package com.jiyi.power.app

import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.aleyn.mvvm.base.BaseActivity
import com.alibaba.android.arouter.facade.annotation.Route
import com.blankj.utilcode.util.BarUtils
import com.blankj.utilcode.util.ToastUtils
import com.jiyi.power.R
import com.jiyi.power.app.bean.BleDeviceStore
import com.jiyi.power.app.common.ChargingPreferences
import com.jiyi.power.app.common.RouterPath
import com.jiyi.power.databinding.ActivityDeviceSettingBinding

@Route(path = RouterPath.DEVICE_SETTING)
class DeviceSettingActivity : BaseActivity<ActivityDeviceSettingBinding>() {

    private val preferences by lazy { getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE) }
    private val deviceSn by lazy { intent.getStringExtra(MobilePowerMainActivity.EXTRA_DEVICE_SN) }

    override fun initView(savedInstanceState: Bundle?) {
        val background = ContextCompat.getColor(this, R.color.color_f6f7f9)
        BarUtils.setStatusBarColor(this, background)
        BarUtils.setStatusBarLightMode(this, true)
        BarUtils.setNavBarColor(this, background)
        renderStoredValues()
        setupClicks()
    }

    override fun initData() = Unit

    override fun onResume() {
        super.onResume()
        renderStoredValues()
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
        rowChargeMode.setOnClickListener { startActivity(Intent(this@DeviceSettingActivity, ChargingModeActivity::class.java)) }
        rowDeviceName.setOnClickListener { showRenameDialog() }
        rowCertification.setOnClickListener {
            AlertDialog.Builder(this@DeviceSettingActivity)
                .setTitle(R.string.device_setting_certification)
                .setMessage(R.string.device_setting_certification_detail)
                .setPositiveButton(R.string.device_setting_confirm, null)
                .show()
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
        AlertDialog.Builder(this)
            .setTitle(R.string.device_setting_factory_reset)
            .setMessage(R.string.device_setting_factory_reset_message)
            .setNegativeButton(R.string.device_setting_cancel, null)
            .setPositiveButton(R.string.device_setting_confirm) { _, _ ->
                preferences.edit().clear().apply()
                renderStoredValues()
                ToastUtils.showShort(R.string.device_setting_factory_reset_success)
            }.show()
    }

    private fun showDeleteDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.device_setting_delete)
            .setMessage(R.string.device_setting_delete_message)
            .setNegativeButton(R.string.device_setting_cancel, null)
            .setPositiveButton(R.string.device_setting_delete) { _, _ ->
                BleDeviceStore.removeDevice(deviceSn)
                preferences.edit().clear().apply()
                ToastUtils.showShort(R.string.device_setting_delete_success)
                startActivity(Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                })
                finish()
            }.show()
    }

    companion object {
        private const val PREFERENCES_NAME = "device_setting"
        private const val KEY_NAME = "device_name"
        private const val KEY_SMART_MODE = "smart_mode"
    }
}
