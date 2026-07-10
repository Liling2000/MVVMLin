package com.jiyi.power.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.aleyn.mvvm.base.BaseActivity
import com.aleyn.mvvm.widget.NavigateTabBar
import com.blankj.utilcode.util.BarUtils
import com.blankj.utilcode.util.ColorUtils
import com.blankj.utilcode.util.PermissionUtils
import com.jiyi.power.R
import com.jiyi.power.databinding.ActivityMainBinding
import com.jiyi.power.ui.mobilepower.fragment.HomeFragment
import com.jiyi.power.ui.mobilepower.fragment.MeFragment
import com.jiyi.power.ui.project.ProjectFragment

class MainActivity : BaseActivity<ActivityMainBinding>() {

    override fun initView(savedInstanceState: Bundle?) {
        BarUtils.setStatusBarColor(this, ColorUtils.getColor(R.color.colorPrimary))

        initNavigationBar()
        PermissionUtils.permission(*PermissionUtils.getPermissions().toTypedArray())
            .callback(object : PermissionUtils.FullCallback {
                override fun onGranted(granted: MutableList<String>) {

                }

                override fun onDenied(
                    forever: MutableList<String>, denied: MutableList<String>
                ) {

                }

            }).request()
    }

    override fun initData() {

    }

    private fun initNavigationBar() {
        mBinding.bottomNavigation.addTab(
            HomeFragment::class.java, NavigateTabBar.TabParam(
                R.drawable.tab_car_selected, R.drawable.tab_car_selected, "首页", true
            ), R.layout.comui_tab_view1
        )

        mBinding.bottomNavigation.addTab(
            ProjectFragment::class.java, NavigateTabBar.TabParam(
                R.drawable.tab_shop_selected, R.drawable.tab_shop_selected, "项目", true
            ), R.layout.comui_tab_view1
        )

        mBinding.bottomNavigation.addTab(
            MeFragment::class.java, NavigateTabBar.TabParam(
                R.drawable.tab_me_selected, R.drawable.tab_me_selected, "我的", true
            ), R.layout.comui_tab_view1
        )

        mBinding.bottomNavigation.setTabSelectListener(object :
            NavigateTabBar.OnTabSelectedListener {
            override fun onTabSelected(holder: NavigateTabBar.ViewHolder?) {
                mBinding.bottomNavigation.showFragment(holder)
            }
        })
    }
}
