package com.jiyi.power.app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import com.aleyn.mvvm.base.BaseActivity
import com.aleyn.mvvm.utils.MmkvManager
import com.blankj.utilcode.util.BarUtils
import com.jiyi.power.app.bean.LoginBean
import com.jiyi.power.databinding.ActivityStartBinding

class StartActivity : BaseActivity<ActivityStartBinding>() {
    private val handler = Handler(Looper.getMainLooper())
    private val routeTask = Runnable { routeToNextPage() }

    override fun initView(savedInstanceState: Bundle?) {
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        BarUtils.setNavBarVisibility(this, false)
        handler.postDelayed(routeTask, START_DELAY_MILLIS)
    }

    override fun initData() = Unit

    private fun routeToNextPage() {
        val loginInfo = MmkvManager.getObject<LoginBean>(LoginBean.LOGIN_INFO_KEY)
        val destination = if (loginInfo == null) LoginActivity::class.java else MainActivity::class.java
        startActivity(Intent(this, destination).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    override fun onDestroy() {
        handler.removeCallbacks(routeTask)
        super.onDestroy()
    }

    companion object {
        private const val START_DELAY_MILLIS = 2_000L
    }
}
