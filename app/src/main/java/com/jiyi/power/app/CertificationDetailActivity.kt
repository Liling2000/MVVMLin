package com.jiyi.power.app

import com.jiyi.power.app.common.RouterPath

import com.alibaba.android.arouter.facade.annotation.Route

import android.os.Bundle
import com.aleyn.mvvm.base.BaseActivity
import com.jiyi.power.R
import com.jiyi.power.app.bean.Certification
import com.jiyi.power.databinding.ActivityCertificationDetailBinding

@Route(path = RouterPath.PAGE_CERTIFICATION_DETAIL)
class CertificationDetailActivity : BaseActivity<ActivityCertificationDetailBinding>() {
    override fun initSystemBars() {
        setSystemBars(statusBarColorRes = R.color.color_f6f8fa)
    }

    override fun initView(savedInstanceState: Bundle?) {
        val certification = Certification.entries.firstOrNull {
            it.name == intent.getStringExtra(EXTRA_CERTIFICATION)
        } ?: run {
            finish()
            return
        }
        with(mBinding) {
            toolbar.setLeftClickListener { finish() }
            toolbar.getLeftIconIv().contentDescription = getString(R.string.certification_back)
            textTitle.setText(certification.titleRes)
            textDescription.setText(certification.descriptionRes)
        }
    }

    override fun initData() = Unit

    companion object {
        const val EXTRA_CERTIFICATION = "extra_certification"
    }
}
