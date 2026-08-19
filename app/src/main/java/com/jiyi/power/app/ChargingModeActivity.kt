package com.jiyi.power.app

import android.content.Intent
import android.os.Bundle
import androidx.core.content.ContextCompat
import com.aleyn.mvvm.base.BaseActivity
import com.aleyn.mvvm.R as BaseR
import com.blankj.utilcode.util.BarUtils
import com.jiyi.power.R
import com.jiyi.power.app.common.ChargingPreferences
import com.jiyi.power.databinding.ActivityChargingModeBinding

class ChargingModeActivity : BaseActivity<ActivityChargingModeBinding>() {
    private val preferences by lazy { getSharedPreferences(ChargingPreferences.FILE_NAME, MODE_PRIVATE) }

    override fun initView(savedInstanceState: Bundle?) {
        val background = ContextCompat.getColor(this, R.color.color_f6f7f9)
        BarUtils.setStatusBarColor(this, background)
        BarUtils.setStatusBarLightMode(this, true)
        BarUtils.setNavBarColor(this, background)
        mBinding.toolbar.setLeftClickListener { finish() }
        mBinding.cardSmart.setOnClickListener { selectMode(ChargingPreferences.MODE_SMART) }
        mBinding.cardStandard.setOnClickListener { selectMode(ChargingPreferences.MODE_STANDARD) }
        mBinding.cardCustom.setOnClickListener {
            startActivity(Intent(this, CustomChargingModeActivity::class.java))
        }
        renderSelection(currentMode())
    }

    override fun onResume() {
        super.onResume()
        renderSelection(currentMode())
    }

    override fun initData() = Unit

    private fun currentMode(): Int = if (preferences.contains(ChargingPreferences.KEY_MODE)) {
        preferences.getInt(ChargingPreferences.KEY_MODE, ChargingPreferences.MODE_SMART)
    } else if (preferences.getBoolean(ChargingPreferences.KEY_LEGACY_SMART_MODE, true)) {
        ChargingPreferences.MODE_SMART
    } else ChargingPreferences.MODE_STANDARD

    private fun selectMode(mode: Int) {
        preferences.edit()
            .putInt(ChargingPreferences.KEY_MODE, mode)
            .putBoolean(ChargingPreferences.KEY_LEGACY_SMART_MODE, mode == ChargingPreferences.MODE_SMART)
            .apply()
        renderSelection(mode)
    }

    private fun renderSelection(mode: Int) {
        renderIndicator(mBinding.indicatorSmart, mode == ChargingPreferences.MODE_SMART)
        renderIndicator(mBinding.indicatorStandard, mode == ChargingPreferences.MODE_STANDARD)
        renderIndicator(mBinding.indicatorCustom, mode == ChargingPreferences.MODE_CUSTOM)
    }

    private fun renderIndicator(view: android.widget.ImageView, selected: Boolean) {
        view.setBackgroundResource(if (selected) R.drawable.bg_charge_selected else R.drawable.bg_charge_unselected)
        view.setImageResource(if (selected) R.mipmap.ic_check_blue else 0)
        if (selected) view.imageTintList = ContextCompat.getColorStateList(this, BaseR.color.color_ffffff)
    }
}
