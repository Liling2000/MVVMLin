package com.jiyi.power.app.widget.popup

import android.content.Context
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.View
import androidx.core.content.ContextCompat
import com.jiyi.power.R
import com.jiyi.power.databinding.PopupPrivacyAgreementBinding

class PrivacyAgreementPopup internal constructor(
    context: Context,
    private val onAgreementClick: () -> Unit,
    private val onAgree: () -> Unit,
    private val onDisagree: () -> Unit,
) : BaseCenterPopup(context) {

    override fun getImplLayoutId(): Int = R.layout.popup_privacy_agreement

    override fun getMaxWidth(): Int = dp(342)

    override fun getMaxHeight(): Int = (resources.displayMetrics.heightPixels * 0.88f).toInt()

    override fun onCreate() {
        super.onCreate()
        val binding = PopupPrivacyAgreementBinding.bind(popupContentView)
        binding.agreementIntroduction.text = buildIntroduction()
        binding.agreementIntroduction.movementMethod = LinkMovementMethod.getInstance()
        binding.agreeButton.setOnClickListener {
            dismiss()
            onAgree()
        }
        binding.disagreeButton.setOnClickListener {
            dismiss()
            onDisagree()
        }
    }

    private fun buildIntroduction(): SpannableString {
        val fullText = context.getString(R.string.popup_privacy_intro)
        val linkText = context.getString(R.string.popup_privacy_link_text)
        val start = fullText.indexOf(linkText)
        return SpannableString(fullText).apply {
            if (start >= 0) {
                val end = start + linkText.length
                setSpan(ForegroundColorSpan(ContextCompat.getColor(context, R.color.color_1717d8)), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(object : ClickableSpan() {
                    override fun onClick(widget: View) = onAgreementClick()

                    override fun updateDrawState(ds: TextPaint) {
                        ds.color = ContextCompat.getColor(context, R.color.color_1717d8)
                        ds.isUnderlineText = false
                    }
                }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }
}
