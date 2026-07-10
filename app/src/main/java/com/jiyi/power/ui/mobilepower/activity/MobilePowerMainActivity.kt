package com.jiyi.power.ui.mobilepower.activity

import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import com.aleyn.mvvm.widget.ComToolBar
import com.alibaba.android.arouter.facade.annotation.Route
import com.jiyi.power.R
import com.jiyi.power.ui.mobilepower.compat.MobilePowerBaseActivity
import com.jiyi.power.ui.mobilepower.fragment.MobilePowerMainFragment
import com.jiyi.power.ui.mobilepower.viewmodel.MainFragmentViewModel
import com.jiyi.power.ui.mobilepower.viewmodel.StatisticsFragmentViewModel

@Route(path = "/mobilepower/main")
class MobilePowerMainActivity : MobilePowerBaseActivity() {
    private val mainFragmentViewModel: MainFragmentViewModel by viewModels()
    private val statisticsFragmentViewModel: StatisticsFragmentViewModel by viewModels()
    private var currentTab = 0
    override fun getLayoutId(): Int = R.layout.activity_mobile_power_main
    override fun onInitView(bundle: Bundle?) {
        findViewById<ComToolBar?>(R.id.toolbar)?.setTitStr( getString(R.string.app_name))
        showFragment(MobilePowerMainFragment())
    }

    fun getCurrentSelectedTab(): Int = currentTab
    private fun showFragment(fragment: Fragment) {
        findViewById<FrameLayout?>(R.id.fl_contain)?.let {
            supportFragmentManager.beginTransaction().replace(R.id.fl_contain, fragment).commitAllowingStateLoss()
        }
    }

}