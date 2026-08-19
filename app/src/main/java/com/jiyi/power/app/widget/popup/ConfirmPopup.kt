package com.jiyi.power.app.widget.popup

import android.content.Context
import androidx.annotation.StringRes
import com.jiyi.power.R
import com.jiyi.power.databinding.PopupConfirmBinding

internal data class ConfirmPopupConfig(
    @StringRes val titleRes: Int,
    @StringRes val messageRes: Int,
    @StringRes val positiveRes: Int,
    val destructive: Boolean = false,
    val onConfirm: () -> Unit,
    val onCancel: () -> Unit,
)

class ConfirmPopup internal constructor(
    context: Context,
    private val config: ConfirmPopupConfig,
) : BaseCenterPopup(context) {

    override fun getImplLayoutId(): Int = R.layout.popup_confirm

    override fun getMaxWidth(): Int = dp(342)

    override fun onCreate() {
        super.onCreate()
        val binding = PopupConfirmBinding.bind(popupContentView)
        binding.popupTitle.setText(config.titleRes)
        binding.popupMessage.setText(config.messageRes)
        binding.positiveButton.setText(config.positiveRes)
        binding.positiveButton.setBackgroundResource(
            if (config.destructive) R.drawable.bg_popup_button_danger else R.drawable.bg_popup_button_primary,
        )
        binding.cancelButton.setOnClickListener {
            dismiss()
            config.onCancel()
        }
        binding.positiveButton.setOnClickListener {
            dismiss()
            config.onConfirm()
        }
    }
}
