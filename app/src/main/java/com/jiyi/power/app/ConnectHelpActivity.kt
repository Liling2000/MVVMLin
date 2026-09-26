package com.jiyi.power.app

import android.os.Bundle
import com.alibaba.android.arouter.facade.annotation.Route
import com.aleyn.mvvm.base.BaseActivity
import com.jiyi.power.R
import com.jiyi.power.app.common.RouterPath
import com.jiyi.power.databinding.ActivityConnectHelpBinding

@Route(path = RouterPath.PAGE_HELP)
class ConnectHelpActivity : BaseActivity<ActivityConnectHelpBinding>() {

    override fun initSystemBars() {
        setSystemBars(statusBarColorRes = R.color.color_f6f8fa)
    }

    override fun initView(savedInstanceState: Bundle?) {
        mBinding.toolbar.setLeftClickListener { finish() }
        mBinding.toolbar.getLeftIconIv().contentDescription = getString(R.string.help_back)
    }

    override fun initData() = Unit
}
