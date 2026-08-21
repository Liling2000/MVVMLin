package com.jiyi.power.app

import android.os.Bundle
import com.aleyn.mvvm.base.BaseActivity
import com.aleyn.mvvm.widget.NavigateTabBar
import com.blankj.utilcode.util.BarUtils
import com.blankj.utilcode.util.ColorUtils
import com.jiyi.power.R
import com.jiyi.power.app.fragment.HomeFragment
import com.jiyi.power.app.fragment.MeFragment
import com.jiyi.power.databinding.ActivityMainBinding

class MainActivity : BaseActivity<ActivityMainBinding>() {
    override fun initView(savedInstanceState: Bundle?) {
        BarUtils.setStatusBarColor(this, ColorUtils.getColor(R.color.color_f7f8fa))
        BarUtils.setStatusBarLightMode(this, true)
        BarUtils.setNavBarColor(this, ColorUtils.getColor(R.color.color_f7f8fa))
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
