package com.jiyi.power.app

import com.alibaba.android.arouter.launcher.ARouter

import com.jiyi.power.app.common.RouterPath

import com.alibaba.android.arouter.facade.annotation.Route

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import com.aleyn.mvvm.base.BaseActivity
import com.aleyn.mvvm.utils.MmkvManager
import com.jiyi.power.R
import com.jiyi.power.app.bean.LoginBean
import com.jiyi.power.databinding.ActivityLoginBinding

@Route(path = RouterPath.PAGE_LOGIN)
class LoginActivity : BaseActivity<ActivityLoginBinding>() {
    private var avatarUri: Uri? = null
    private val avatarPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        runCatching {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        avatarUri = uri
        mBinding.imageAvatar.setImageURI(uri)
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
            avatarUrl = avatarUri?.toString().orEmpty()
        )
        MmkvManager.putObject(LoginBean.LOGIN_INFO_KEY, loginInfo)
        ARouter.getInstance().build(RouterPath.PAGE_MAIN)
            .withFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            .navigation(this)
        finish()
    }
}
