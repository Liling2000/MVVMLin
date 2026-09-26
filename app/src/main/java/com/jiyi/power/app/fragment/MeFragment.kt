package com.jiyi.power.app.fragment

import com.jiyi.power.app.common.RouterPath

import com.alibaba.android.arouter.launcher.ARouter

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import coil.load
import coil.transform.CircleCropTransformation
import com.aleyn.mvvm.utils.MmkvManager
import com.blankj.utilcode.util.ToastUtils
import com.aleyn.mvvm.base.BaseVMFragment
import com.jiyi.power.R
import com.jiyi.power.app.bean.LoginBean
import com.jiyi.power.app.common.UserProfileStorage
import com.jiyi.power.app.language.AppLanguages
import com.jiyi.power.app.language.LanguageManager
import com.jiyi.power.app.viewmodel.MeViewModel
import com.jiyi.power.databinding.MeFragmentBinding
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MeFragment : BaseVMFragment<MeViewModel, MeFragmentBinding>() {

    private val avatarPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        saveAvatar(uri)
    }

    companion object {
        fun newInstance() = MeFragment()
    }

    override fun initView(savedInstanceState: Bundle?) = with(mBinding) {
        imageProfile.setOnClickListener { avatarPicker.launch(arrayOf("image/*")) }

        itemLanguage.setOnClickListener {
            ARouter.getInstance().build(RouterPath.PAGE_LANGUAGE).navigation(requireActivity())
        }

        itemAbout.setOnClickListener {
            ARouter.getInstance().build(RouterPath.PAGE_ABOUT).navigation(requireActivity())
        }

        textUserAgreement.setOnClickListener {
            showUnavailable(R.string.me_user_agreement)
        }
        textPrivacyPolicy.setOnClickListener {
            showUnavailable(R.string.me_privacy_policy)
        }
    }

    override fun initObserve() = Unit

    override fun onResume() {
        super.onResume()
        val language = AppLanguages.items.first { it.tag == LanguageManager.selectedTag }
        mBinding.itemLanguage.setRightTextValue(getString(language.label))
        renderProfile()
    }

    override fun lazyLoadData() = Unit

    private fun renderProfile() = with(mBinding) {
        val profile = MmkvManager.getObject<LoginBean>(LoginBean.LOGIN_INFO_KEY)
        textUserName.text = profile?.name
            ?.takeIf { it.isNotBlank() }
            ?: getString(R.string.login_default_guest_name)

        val avatar = profile?.avatarUrl.orEmpty()
        val avatarFile = avatar.takeIf { it.isNotBlank() }?.let(::File)?.takeIf(File::isFile)
        val imageData: Any? = avatarFile ?: avatar
            .takeIf { it.isNotBlank() }
            ?.let(Uri::parse)

        if (imageData == null) {
            imageProfile.setImageResource(R.mipmap.ic_user_profile)
        } else {
            imageProfile.load(imageData) {
                crossfade(true)
                placeholder(R.mipmap.ic_user_profile)
                error(R.mipmap.ic_user_profile)
                transformations(CircleCropTransformation())
            }
        }
    }

    private fun saveAvatar(uri: Uri) = with(mBinding) {
        imageProfile.isEnabled = false
        imageProfile.load(uri) {
            transformations(CircleCropTransformation())
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val avatarFile = withContext(Dispatchers.IO) {
                runCatching { UserProfileStorage.saveAvatar(requireContext().applicationContext, uri) }
            }
            avatarFile.onSuccess { file ->
                val currentProfile = MmkvManager.getObject<LoginBean>(LoginBean.LOGIN_INFO_KEY)
                val updatedProfile = currentProfile?.copy(avatarUrl = file.absolutePath)
                    ?: LoginBean(
                        id = System.currentTimeMillis(),
                        name = getString(R.string.login_default_guest_name),
                        avatarUrl = file.absolutePath,
                    )
                MmkvManager.putObject(LoginBean.LOGIN_INFO_KEY, updatedProfile)
                imageProfile.load(file) {
                    transformations(CircleCropTransformation())
                }
            }.onFailure {
                renderProfile()
                ToastUtils.showShort(R.string.login_avatar_save_failed)
            }
            imageProfile.isEnabled = true
        }
    }

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
