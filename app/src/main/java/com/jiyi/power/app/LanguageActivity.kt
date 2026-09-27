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
        super.onCreate(savedInstanceState)
    }

    override fun initSystemBars() {
        setSystemBars(statusBarColorRes = R.color.color_f7f9fb)
    }

    override fun initView(savedInstanceState: Bundle?) = with(mBinding) {
        languageList.layoutManager = LinearLayoutManager(this@LanguageActivity)
        languageList.itemAnimator = null
        renderLanguage(requireNotNull(LanguageManager.selectedTag))
    }

    override fun initData() = Unit

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Keep the Activity window and its edge-to-edge protection alive. Replacing only the
        // binding hierarchy updates localized resources without exposing the window background.
        rebindContent()
        initSystemBars()
        initView(null)
    }

    override fun onStart() {
        super.onStart()
        disableWindowTransitions()
    }

    override fun finish() {
        if (isFinishing) return
        disableWindowTransitions()
        super.finish()
        // LocaleManager dispatches an app-wide configuration change on Android 13+. Commit only
        // after this picker has begun closing so a selection cannot send the user back to Me.
        LanguageManager.applySelectedLocale(this)
    }

    private fun renderLanguage(tag: String): Unit {
        with(mBinding) {
            val localized = LanguageManager.localizedContext(this@LanguageActivity, tag)
            toolbar.setLeftClickListener { finish() }
            toolbar.setTitStr(localized.getString(R.string.me_switch_language))
            toolbar.getLeftIconIv().contentDescription = localized.getString(R.string.language_back)
            languageHint.text = localized.getString(R.string.language_choose_hint)
            languageList.adapter = LanguageAdapter(tag, localized) { item ->
                disableWindowTransitions()
                LanguageManager.selectWhilePickerIsOpen(item.tag)
                renderLanguage(item.tag)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun disableWindowTransitions() {
        window.setWindowAnimations(0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            overridePendingTransition(0, 0)
        }
    }
}
