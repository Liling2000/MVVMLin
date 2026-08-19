package com.jiyi.power.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.aleyn.mvvm.base.BaseActivity
import com.aleyn.mvvm.utils.MmkvManager
import com.blankj.utilcode.util.BarUtils
import com.jiyi.power.R
import com.jiyi.power.app.bean.LoginBean
import com.jiyi.power.databinding.ActivityLoginBinding

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
        val background = ContextCompat.getColor(this, R.color.color_f6f7f9)
        BarUtils.setStatusBarColor(this, background)
        BarUtils.setStatusBarLightMode(this, true)
        BarUtils.setNavBarColor(this, background)
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
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}
