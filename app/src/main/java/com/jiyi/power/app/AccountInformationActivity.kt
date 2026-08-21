package com.jiyi.power.app

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.FrameLayout
import android.util.TypedValue
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.aleyn.mvvm.base.BaseActivity
import com.aleyn.mvvm.utils.MmkvManager
import com.blankj.utilcode.util.BarUtils
import com.blankj.utilcode.util.ToastUtils
import com.jiyi.power.R
import com.jiyi.power.app.bean.LoginBean
import com.jiyi.power.databinding.ActivityAccountInformationBinding

class AccountInformationActivity : BaseActivity<ActivityAccountInformationBinding>() {

    override fun initView(savedInstanceState: Bundle?) = with(mBinding) {
        val background = ContextCompat.getColor(this@AccountInformationActivity, R.color.account_page_background)
        BarUtils.setStatusBarColor(this@AccountInformationActivity, background)
        BarUtils.setStatusBarLightMode(this@AccountInformationActivity, true)
        BarUtils.setNavBarColor(this@AccountInformationActivity, background)
        toolbar.setLeftClickListener { finish() }
        itemUserName.setOnClickListener {
            showEditDialog(R.string.account_edit_user_name, itemUserName.getLeftSubTextValue(), InputType.TYPE_CLASS_TEXT, ::updateUserName)
        }
        itemPhone.setOnClickListener {
            showEditDialog(R.string.account_edit_phone, phone(), InputType.TYPE_CLASS_PHONE) {
                MmkvManager.putString(KEY_PHONE, it); renderAccount()
            }
        }
        itemEmail.setOnClickListener {
            showEditDialog(R.string.account_edit_email, email(), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS) {
                MmkvManager.putString(KEY_EMAIL, it); renderAccount()
            }
        }
        itemPassword.setOnClickListener { ToastUtils.showShort(R.string.account_password_unavailable) }
        itemCancellation.setOnClickListener { showCancellationConfirmation() }
    }

    override fun initData() = renderAccount()

    override fun onResume() {
        super.onResume()
        renderAccount()
    }

    private fun phone() = MmkvManager.getString(KEY_PHONE, getString(R.string.account_default_phone))
    private fun email() = MmkvManager.getString(KEY_EMAIL, getString(R.string.account_default_email))

    private fun renderAccount() = with(mBinding) {
        val user = MmkvManager.getObject<LoginBean>(LoginBean.LOGIN_INFO_KEY)
        itemUserName.setLeftSubTextValue(user?.name ?: getString(R.string.account_default_user_name))
        itemPhone.setLeftSubTextValue(phone())
        itemEmail.setLeftSubTextValue(email())
    }

    private fun updateUserName(name: String) {
        val user = MmkvManager.getObject<LoginBean>(LoginBean.LOGIN_INFO_KEY) ?: return
        MmkvManager.putObject(LoginBean.LOGIN_INFO_KEY, user.copy(name = name))
        renderAccount()
    }

    private fun showEditDialog(titleRes: Int, value: String, inputType: Int, onSave: (String) -> Unit) {
        val input = EditText(this).apply {
            setText(value); setSelection(text.length); this.inputType = inputType; setSingleLine(true)
        }
        val padding = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 24f, resources.displayMetrics
        ).toInt()
        val container = FrameLayout(this).apply {
            setPadding(padding, 0, padding, 0)
            addView(input, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT))
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle(titleRes).setView(container)
            .setNegativeButton(R.string.account_cancel, null)
            .setPositiveButton(R.string.account_save, null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val newValue = input.text.toString().trim()
                if (newValue.isEmpty()) input.error = getString(R.string.account_input_required)
                else { onSave(newValue); dialog.dismiss() }
            }
        }
        dialog.show()
    }

    private fun showCancellationConfirmation() {
        AlertDialog.Builder(this)
            .setTitle(R.string.account_cancellation_confirm_title)
            .setMessage(R.string.account_cancellation_confirm_message)
            .setNegativeButton(R.string.account_cancel, null)
            .setPositiveButton(R.string.account_confirm_cancellation) { _, _ -> cancelAccount() }
            .show()
    }

    private fun cancelAccount() {
        MmkvManager.remove(LoginBean.LOGIN_INFO_KEY)
        MmkvManager.remove(KEY_PHONE)
        MmkvManager.remove(KEY_EMAIL)
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    companion object {
        private const val KEY_PHONE = "account_phone"
        private const val KEY_EMAIL = "account_email"
    }
}
