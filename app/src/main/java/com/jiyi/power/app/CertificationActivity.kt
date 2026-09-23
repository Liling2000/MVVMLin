package com.jiyi.power.app

import com.alibaba.android.arouter.launcher.ARouter

import com.jiyi.power.app.common.RouterPath

import com.alibaba.android.arouter.facade.annotation.Route

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.aleyn.mvvm.base.BaseActivity
import com.jiyi.power.R
import com.jiyi.power.app.adapter.CertificationAdapter
import com.jiyi.power.databinding.ActivityCertificationBinding

@Route(path = RouterPath.PAGE_CERTIFICATION)
class CertificationActivity : BaseActivity<ActivityCertificationBinding>() {
    override fun initSystemBars() {
        setSystemBars(statusBarColorRes = R.color.color_f6f8fa)
    }

    override fun initView(savedInstanceState: Bundle?) = with(mBinding) {
        toolbar.setLeftClickListener { finish() }
        toolbar.getLeftIconIv().contentDescription = getString(R.string.certification_back)
        certificationList.layoutManager = LinearLayoutManager(this@CertificationActivity)
        certificationList.adapter = CertificationAdapter { certification ->
            ARouter.getInstance().build(RouterPath.PAGE_CERTIFICATION_DETAIL)
                .withString(CertificationDetailActivity.EXTRA_CERTIFICATION, certification.name)
                .navigation(this@CertificationActivity)
        }
    }

    override fun initData() = Unit
}
