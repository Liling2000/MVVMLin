package com.jiyi.power.app

import com.alibaba.android.arouter.launcher.ARouter

import com.jiyi.power.app.common.RouterPath

import com.alibaba.android.arouter.facade.annotation.Route

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import coil.load
import coil.transform.CircleCropTransformation
import com.aleyn.mvvm.base.BaseActivity
import com.aleyn.mvvm.utils.MmkvManager
import com.blankj.utilcode.util.ToastUtils
import com.jiyi.power.R
import com.jiyi.power.app.bean.LoginBean
import com.jiyi.power.app.common.UserProfileStorage
import com.jiyi.power.databinding.ActivityLoginBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Route(path = RouterPath.PAGE_LOGIN)
class LoginActivity : BaseActivity<ActivityLoginBinding>() {
    private var avatarPath = ""
    private val avatarPicker =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri ?: return@registerForActivityResult
            saveAvatar(uri)
        }

    override fun initView(savedInstanceState: Bundle?) {
        mBinding.imageAvatar.setOnClickListener { avatarPicker.launch(arrayOf("image/*")) }
        mBinding.buttonGuestLogin.setOnClickListener { loginAsGuest() }
    }

    override fun initData() = Unit

    private fun loginAsGuest() {
        val inputName = mBinding.editName.text?.toString()?.trim().orEmpty()
        val loginInfo = LoginBean(
            id = System.currentTimeMillis(),
            name = inputName.ifEmpty { getString(R.string.login_default_guest_name) },
            avatarUrl = avatarPath,
        )
        MmkvManager.putObject(LoginBean.LOGIN_INFO_KEY, loginInfo)
        ARouter.getInstance().build(RouterPath.PAGE_MAIN)
            .withFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            .navigation(this)
        finish()
    }

    private fun saveAvatar(uri: Uri) {
        mBinding.buttonGuestLogin.isEnabled = false
        mBinding.imageAvatar.load(uri) {
            transformations(CircleCropTransformation())
        }
        lifecycleScope.launch {
            val avatarFile = withContext(Dispatchers.IO) {
                runCatching { UserProfileStorage.saveAvatar(applicationContext, uri) }
            }
            avatarFile.onSuccess { file ->
                avatarPath = file.absolutePath
                mBinding.imageAvatar.load(file) {
                    transformations(CircleCropTransformation())
                }
            }.onFailure {
                avatarPath = ""
                mBinding.imageAvatar.setImageResource(R.mipmap.ic_add_photo_outline)
                ToastUtils.showShort(R.string.login_avatar_save_failed)
            }
            mBinding.buttonGuestLogin.isEnabled = true
        }
    }
}
