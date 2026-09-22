package com.jiyi.power.app

import android.os.Bundle
import com.aleyn.mvvm.base.BaseActivity
import com.aleyn.mvvm.widget.NavigateTabBar
import com.jiyi.power.R
import com.jiyi.power.app.fragment.HomeFragment
import com.jiyi.power.app.fragment.MeFragment
import com.jiyi.power.databinding.ActivityMainBinding

class MainActivity : BaseActivity<ActivityMainBinding>() {
    override fun initSystemBars() {
        setSystemBars(statusBarColorRes = R.color.color_f7f8fa)
    }

    override fun initView(savedInstanceState: Bundle?) {
        initNavigationBar()
    }

    override fun initData() = Unit

    private fun initNavigationBar() = with(mBinding.bottomNavigation) {
        addTab(
            HomeFragment::class.java, NavigateTabBar.TabParam(
                R.mipmap.ic_common_lightning_gray,
                R.mipmap.ic_common_lightning_blue,
                getString(R.string.home_tab_device),
                true
            ), R.layout.comui_tab_view1
        )
        addTab(
            MeFragment::class.java, NavigateTabBar.TabParam(
                R.mipmap.ic_common_profile_outline,
                R.mipmap.ic_common_profile_selected,
                getString(R.string.home_tab_me),
                true
            ), R.layout.comui_tab_view1
        )
        setTabSelectListener(object : NavigateTabBar.OnTabSelectedListener {
            override fun onTabSelected(holder: NavigateTabBar.ViewHolder?) = showFragment(holder)
        })
    }

}
