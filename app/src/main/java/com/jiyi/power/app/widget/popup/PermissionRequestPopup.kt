package com.jiyi.power.app.widget.popup

import android.content.Context
import com.jiyi.power.R
import com.jiyi.power.databinding.PopupPermissionRequestBinding

class PermissionRequestPopup internal constructor(
    context: Context,
    private val onAgree: () -> Unit,
    private val onDisagree: () -> Unit,
) : BaseCenterPopup(context) {

    override fun getImplLayoutId(): Int = R.layout.popup_permission_request

    override fun getMaxWidth(): Int = dp(320)

    override fun onCreate() {
        super.onCreate()
        val binding = PopupPermissionRequestBinding.bind(popupContentView)
        binding.agreeButton.setOnClickListener {
            dismiss()
            onAgree()
        }
        binding.disagreeButton.setOnClickListener {
            dismiss()
            onDisagree()
        }
    }
}
