package com.jiyi.power.app

import com.alibaba.android.arouter.launcher.ARouter

import com.jiyi.power.app.common.RouterPath

import com.alibaba.android.arouter.facade.annotation.Route

import android.content.Intent
import android.content.ActivityNotFoundException
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import com.aleyn.mvvm.base.BaseActivity
import com.aleyn.mvvm.utils.MmkvManager
import com.blankj.utilcode.util.BarUtils
import com.jiyi.power.app.bean.LoginBean
import com.jiyi.power.R
import com.jiyi.power.app.widget.popup.AppPopupManager
import com.jiyi.power.databinding.ActivityStartBinding

@Route(path = RouterPath.PAGE_START)
class StartActivity : BaseActivity<ActivityStartBinding>() {
    private val handler = Handler(Looper.getMainLooper())
    private val routeTask = Runnable { routeToNextPage() }
    private var privacyPromptShown = false

    override fun initSystemBars() {
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        BarUtils.setNavBarVisibility(this, false)
    }

    override fun initView(savedInstanceState: Bundle?) {
        if (MmkvManager.getBoolean(KEY_PRIVACY_AGREEMENT_ACCEPTED)) {
            scheduleNextPage()
        }
    }

    override fun initData() = Unit

    override fun onPostResume() {
        super.onPostResume()
        if (!MmkvManager.getBoolean(KEY_PRIVACY_AGREEMENT_ACCEPTED) && !privacyPromptShown) {
            privacyPromptShown = true
            showPrivacyAgreement()
        }
    }

    private fun showPrivacyAgreement() {
        AppPopupManager.showPrivacyAgreement(
            context = this,
            onAgreementClick = { openPrivacyPolicy() },
            onAgree = {
                MmkvManager.putBoolean(KEY_PRIVACY_AGREEMENT_ACCEPTED, true)
                scheduleNextPage()
            },
            onDisagree = {
                handler.removeCallbacks(routeTask)
                finishAndRemoveTask()
            },
        )
    }

    private fun scheduleNextPage() {
        handler.removeCallbacks(routeTask)
        handler.postDelayed(routeTask, START_DELAY_MILLIS)
    }

    private fun openPrivacyPolicy() {
        try {
            startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.about_privacy_policy_url))),
            )
        } catch (_: ActivityNotFoundException) {
            // 没有可处理网页的应用时，继续停留在协议弹窗。
        } catch (_: SecurityException) {
            // 系统禁止打开链接时，继续停留在协议弹窗。
        }
    }

    private fun routeToNextPage() {
        val loginInfo = MmkvManager.getObject<LoginBean>(LoginBean.LOGIN_INFO_KEY)
        val destination = if (loginInfo == null) RouterPath.PAGE_LOGIN else RouterPath.PAGE_MAIN
        ARouter.getInstance().build(destination)
            .withFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            .navigation(this)
        finish()
    }

    override fun onDestroy() {
        handler.removeCallbacks(routeTask)
        super.onDestroy()
    }

    companion object {
        private const val START_DELAY_MILLIS = 1_000L
        private const val KEY_PRIVACY_AGREEMENT_ACCEPTED = "privacy_agreement_accepted"
    }
}
