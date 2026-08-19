package com.jiyi.power.app.fragment

import android.content.Intent
import android.provider.Settings
import android.os.Bundle
import com.blankj.utilcode.util.ToastUtils
import com.aleyn.mvvm.base.BaseVMFragment
import com.jiyi.power.R
import com.jiyi.power.app.viewmodel.MeViewModel
import com.jiyi.power.databinding.MeFragmentBinding

class MeFragment : BaseVMFragment<MeViewModel, MeFragmentBinding>() {

    companion object {
        fun newInstance() = MeFragment()
    }

    override fun initView(savedInstanceState: Bundle?) = with(mBinding) {
        itemAccount.setOnClickListener {
            showUnavailable(R.string.me_account_management)
        }
        itemLanguage.setOnClickListener {
            showUnavailable(R.string.me_switch_language)
        }
        itemNotifications.setOnClickListener {
            openNotificationSettings()
        }
        itemFeedback.setOnClickListener {
            showUnavailable(R.string.me_feedback)
        }
        itemAbout.setOnClickListener {
            showUnavailable(R.string.me_about_app)
        }
        itemCustomerService.setOnClickListener {
            showUnavailable(R.string.me_customer_service)
        }
        textUserAgreement.setOnClickListener {
            showUnavailable(R.string.me_user_agreement)
        }
        textPrivacyPolicy.setOnClickListener {
            showUnavailable(R.string.me_privacy_policy)
        }
    }

    override fun initObserve() = Unit

    override fun lazyLoadData() = Unit

    private fun openNotificationSettings() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
        }
        runCatching { startActivity(intent) }
            .onFailure { showUnavailable(R.string.me_notifications) }
    }

    private fun showUnavailable(titleRes: Int) {
        ToastUtils.showShort(
            getString(R.string.me_feature_unavailable, getString(titleRes)),
        )
    }
}
