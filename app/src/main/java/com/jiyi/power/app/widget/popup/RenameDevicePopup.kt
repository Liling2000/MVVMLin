package com.jiyi.power.app.widget.popup

import android.content.Context
import android.text.InputFilter
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.jiyi.power.R
import com.jiyi.power.databinding.PopupRenameDeviceBinding

class RenameDevicePopup internal constructor(
    context: Context,
    private val currentName: String,
    private val onConfirm: (String) -> Unit,
) : BaseCenterPopup(context) {

    override fun getImplLayoutId(): Int = R.layout.popup_rename_device

    override fun getMaxWidth(): Int = dp(342)

    override fun onCreate() {
        super.onCreate()
        val binding = PopupRenameDeviceBinding.bind(contentView)
        binding.nameInput.apply {
            filters = arrayOf(InputFilter.LengthFilter(MAX_NAME_LENGTH))
            setText(currentName)
            setSelection(text?.length ?: 0)
            doAfterTextChanged { binding.nameError.isVisible = false }
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    confirm(binding)
                    true
                } else {
                    false
                }
            }
            postDelayed({
                requestFocus()
                (context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
                    ?.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
            }, KEYBOARD_DELAY_MS)
        }
        binding.cancelButton.setOnClickListener { dismiss() }
        binding.confirmButton.setOnClickListener { confirm(binding) }
    }

    private fun confirm(binding: PopupRenameDeviceBinding) {
        val name = binding.nameInput.text?.toString()?.trim().orEmpty()
        if (name.isEmpty()) {
            binding.nameError.isVisible = true
            binding.nameInput.requestFocus()
            return
        }
        dismiss()
        onConfirm(name)
    }

    private companion object {
        const val MAX_NAME_LENGTH = 10
        const val KEYBOARD_DELAY_MS = 250L
    }
}
