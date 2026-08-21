package com.jiyi.power.app

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.aleyn.mvvm.base.BaseActivity
import com.blankj.utilcode.util.BarUtils
import com.blankj.utilcode.util.ToastUtils
import com.jiyi.power.BuildConfig
import com.jiyi.power.R
import com.jiyi.power.databinding.ActivityAboutBinding

class AboutActivity : BaseActivity<ActivityAboutBinding>() {

    override fun initView(savedInstanceState: Bundle?) = with(mBinding) {
        BarUtils.setStatusBarColor(this@AboutActivity, getColor(R.color.about_page_background))
        BarUtils.setStatusBarLightMode(this@AboutActivity, true)
        BarUtils.setNavBarColor(this@AboutActivity, getColor(R.color.about_page_background))

        val version = getString(R.string.about_version_format, BuildConfig.VERSION_NAME)
        textVersion.text = version
        itemAppVersion.setRightTextValue(version)

        toolbar.setLeftClickListener { finish() }
        itemAppVersion.setOnClickListener {
            ToastUtils.showShort(getString(R.string.about_current_version, version))
        }
        itemWebsite.setOnClickListener { openUri(getString(R.string.about_website_url)) }
        itemPhone.setOnClickListener {
            openIntent(Intent(Intent.ACTION_DIAL, Uri.parse(getString(R.string.about_phone_uri))))
        }
        itemEmail.setOnClickListener {
            openIntent(Intent(Intent.ACTION_SENDTO, Uri.parse(getString(R.string.about_email_uri))))
        }
        itemFiling.setOnClickListener { openUri(getString(R.string.about_filing_url)) }
        textUserAgreement.setOnClickListener { openUri(getString(R.string.about_user_agreement_url)) }
        textPrivacyPolicy.setOnClickListener { openUri(getString(R.string.about_privacy_policy_url)) }
    }

    override fun initData() = Unit

    private fun openUri(uri: String) {
        openIntent(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
    }

    private fun openIntent(intent: Intent) {
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            ToastUtils.showShort(R.string.about_no_handler)
        } catch (_: SecurityException) {
            ToastUtils.showShort(R.string.about_no_handler)
        }
    }
}
