package com.jiyi.power.app

import android.os.Bundle
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.aleyn.mvvm.base.BaseActivity
import com.aleyn.mvvm.widget.SwitchButtonKotlin
import com.blankj.utilcode.util.BarUtils
import com.blankj.utilcode.util.ToastUtils
import com.jiyi.power.R
import com.jiyi.power.app.common.ChargingPreferences
import com.jiyi.power.databinding.ActivityCustomChargingModeBinding

class CustomChargingModeActivity : BaseActivity<ActivityCustomChargingModeBinding>() {
    private val preferences by lazy { getSharedPreferences(ChargingPreferences.FILE_NAME, MODE_PRIVATE) }
    private var cutoff = 90

    override fun initView(savedInstanceState: Bundle?) {
        val background = ContextCompat.getColor(this, R.color.color_f6f7f9)
        BarUtils.setStatusBarColor(this, background)
        BarUtils.setStatusBarLightMode(this, true)
        BarUtils.setNavBarColor(this, background)
        restoreState()
        setupInteractions()
    }

    override fun initData() = Unit

    private fun restoreState() = with(mBinding) {
        val power = preferences.getInt(ChargingPreferences.KEY_POWER, 150).coerceIn(5, 300)
        cutoff = preferences.getInt(ChargingPreferences.KEY_CUTOFF, 90).takeIf { it in listOf(80, 90, 100) } ?: 90
        seekPower.progress = power - 5
        textPowerValue.text = getString(R.string.custom_mode_power_value, power)
        option80.text = getString(R.string.custom_mode_percent, 80)
        option90.text = getString(R.string.custom_mode_percent, 90)
        option100.text = getString(R.string.custom_mode_percent, 100)
        switchOverheat.isChecked = preferences.getBoolean(ChargingPreferences.KEY_OVERHEAT, true)
        switchDisconnect.isChecked = preferences.getBoolean(ChargingPreferences.KEY_DISCONNECT, true)
        switchNight.isChecked = preferences.getBoolean(ChargingPreferences.KEY_NIGHT, false)
        renderCutoff()
    }

    private fun setupInteractions() = with(mBinding) {
        toolbar.setLeftClickListener { finish() }
        seekPower.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                textPowerValue.text = getString(R.string.custom_mode_power_value, progress + 5)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
        option80.setOnClickListener { selectCutoff(80) }
        option90.setOnClickListener { selectCutoff(90) }
        option100.setOnClickListener { selectCutoff(100) }
        bindSwitch(rowOverheat, switchOverheat)
        bindSwitch(rowDisconnect, switchDisconnect)
        bindSwitch(rowNight, switchNight)
        buttonSave.setOnClickListener { saveAndFinish() }
    }

    private fun bindSwitch(row: android.view.View, button: SwitchButtonKotlin) {
        row.setOnClickListener { button.toggle() }
        button.setOnClickListener { button.toggle() }
    }

    private fun selectCutoff(value: Int) {
        cutoff = value
        renderCutoff()
    }

    private fun renderCutoff() {
        renderOption(mBinding.option80, cutoff == 80)
        renderOption(mBinding.option90, cutoff == 90)
        renderOption(mBinding.option100, cutoff == 100)
    }

    private fun renderOption(view: TextView, selected: Boolean) {
        view.setBackgroundResource(if (selected) R.drawable.bg_custom_option_selected else R.drawable.bg_custom_option_normal)
        view.setTextColor(ContextCompat.getColor(this, if (selected) R.color.color_0752ae else R.color.color_45475a))
    }

    private fun saveAndFinish() = with(mBinding) {
        preferences.edit()
            .putInt(ChargingPreferences.KEY_MODE, ChargingPreferences.MODE_CUSTOM)
            .putInt(ChargingPreferences.KEY_POWER, seekPower.progress + 5)
            .putInt(ChargingPreferences.KEY_CUTOFF, cutoff)
            .putBoolean(ChargingPreferences.KEY_OVERHEAT, switchOverheat.isChecked)
            .putBoolean(ChargingPreferences.KEY_DISCONNECT, switchDisconnect.isChecked)
            .putBoolean(ChargingPreferences.KEY_NIGHT, switchNight.isChecked)
            .putBoolean(ChargingPreferences.KEY_LEGACY_SMART_MODE, false)
            .apply()
        ToastUtils.showShort(R.string.custom_mode_saved)
        finish()
    }
}
