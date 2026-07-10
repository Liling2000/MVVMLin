package com.jiyi.power.ui.mobilepower.compat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.viewbinding.ViewBinding
import com.aleyn.mvvm.base.BaseActivity
import com.blankj.utilcode.util.ToastUtils

abstract class MobilePowerBaseActivity : BaseActivity<ViewBinding>() {

    abstract fun getLayoutId(): Int

    open fun onInitView(bundle: Bundle?) {}

    open fun onEvent() {}

    override fun initBinding(): View {
        return LayoutInflater.from(this).inflate(getLayoutId(), null, false)
    }

    override fun initView(savedInstanceState: Bundle?) {
        onInitView(savedInstanceState)
        onEvent()
    }

    override fun initData() {
    }

    open fun showDialog() {
        showLoading()
    }

    open fun dismissDialog() {
        dismissLoading()
    }

    open fun timeOutSet() {
    }

    open fun cancelTimeOut() {
    }

    open fun timeOutLogic() {
    }

    fun toastShow(message: String?) {
        if (!message.isNullOrBlank()) ToastUtils.showShort(message)
    }

    fun toastShow(resId: Int) {
        ToastUtils.showShort(resId)
    }
}