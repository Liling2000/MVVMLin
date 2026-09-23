package com.jiyi.power.app

import android.content.res.Configuration
import android.os.Bundle
import android.os.Build
import androidx.recyclerview.widget.LinearLayoutManager
import com.aleyn.mvvm.base.BaseActivity
import com.alibaba.android.arouter.facade.annotation.Route
import com.jiyi.power.R
import com.jiyi.power.app.adapter.LanguageAdapter
import com.jiyi.power.app.common.RouterPath
import com.jiyi.power.app.language.LanguageManager
import com.jiyi.power.databinding.ActivityLanguageBinding

@Route(path = RouterPath.PAGE_LANGUAGE)
class LanguageActivity : BaseActivity<ActivityLanguageBinding>() {
    override fun onCreate(savedInstanceState: Bundle?) {
        disableWindowTransitions()
        overridePendingTransition(0, 0)
        super.onCreate(savedInstanceState)
    }

    override fun initSystemBars() {
        setSystemBars(statusBarColorRes = R.color.color_f7f9fb)
    }

    override fun initView(savedInstanceState: Bundle?) = with(mBinding) {
        toolbar.setLeftClickListener { finish() }
        toolbar.getLeftIconIv().contentDescription = getString(R.string.language_back)
        languageList.layoutManager = LinearLayoutManager(this@LanguageActivity)
        languageList.itemAnimator = null
        languageList.adapter = LanguageAdapter(requireNotNull(LanguageManager.selectedTag)) { item ->
            disableWindowTransitions()
            LanguageManager.select(this@LanguageActivity, item.tag)
        }
    }

    override fun initData() = Unit

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setContentView(initBinding())
        initSystemBars()
        initView(null)
    }

    override fun onStart() {
        super.onStart()
        overridePendingTransition(0, 0)
    }

    private fun disableWindowTransitions() {
        window.setWindowAnimations(0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        }
    }
}
