package com.jiyi.power.app.widget.popup

import android.content.Context
import com.jiyi.power.R
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.core.BasePopupView

object AppPopupManager {
    fun showBluetoothServiceRequest(
        context: Context,
        onOpenSettings: () -> Unit,
        onCancel: () -> Unit = {},
    ): ConfirmPopup = showConfirm(
        context,
        ConfirmPopupConfig(
            titleRes = R.string.popup_bluetooth_title,
            messageRes = R.string.popup_bluetooth_message,
            positiveRes = R.string.popup_go_to_settings,
            onConfirm = onOpenSettings,
            onCancel = onCancel,
        ),
    )

    fun showDeleteDevice(
        context: Context,
        onConfirm: () -> Unit,
        onCancel: () -> Unit = {},
    ): ConfirmPopup = showConfirm(
        context,
        ConfirmPopupConfig(
            titleRes = R.string.popup_delete_title,
            messageRes = R.string.popup_delete_message,
            positiveRes = R.string.popup_confirm,
            destructive = true,
            onConfirm = onConfirm,
            onCancel = onCancel,
        ),
    )

    fun showRestoreFactorySettings(
        context: Context,
        onConfirm: () -> Unit,
        onCancel: () -> Unit = {},
    ): ConfirmPopup = showConfirm(
        context,
        ConfirmPopupConfig(
            titleRes = R.string.popup_restore_title,
            messageRes = R.string.popup_restore_message,
            positiveRes = R.string.popup_confirm,
            onConfirm = onConfirm,
            onCancel = onCancel,
        ),
    )

    fun showPermissionRequest(
        context: Context,
        onAgree: () -> Unit,
        onDisagree: () -> Unit = {},
    ): PermissionRequestPopup = show(
        context,
        PermissionRequestPopup(context, onAgree, onDisagree),
        dismissible = false,
    )

    fun showPrivacyAgreement(
        context: Context,
        onAgreementClick: () -> Unit,
        onAgree: () -> Unit,
        onDisagree: () -> Unit = {},
    ): PrivacyAgreementPopup = show(
        context,
        PrivacyAgreementPopup(context, onAgreementClick, onAgree, onDisagree),
        dismissible = false,
    )

    private fun showConfirm(context: Context, config: ConfirmPopupConfig): ConfirmPopup =
        show(context, ConfirmPopup(context, config))

    private fun <T : BasePopupView> show(
        context: Context,
        popup: T,
        dismissible: Boolean = true,
    ): T {
        XPopup.Builder(context)
            .dismissOnTouchOutside(dismissible)
            .dismissOnBackPressed(dismissible)
            .enableDrag(false)
            .animationDuration(250)
            .asCustom(popup)
            .show()
        return popup
    }
}
