package com.aleyn.mvvm.widget

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.aleyn.mvvm.R
import com.aleyn.mvvm.databinding.ViewMyInformationBinding

/** Reusable settings row whose complete view hierarchy is maintained in XML. */
class MyInformationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding = ViewMyInformationBinding.inflate(
        LayoutInflater.from(context),
        this,
        true,
    )

    init {
        isClickable = true
        isFocusable = true
        applyAttributes(attrs, defStyleAttr)
    }

    private fun applyAttributes(attrs: AttributeSet?, defStyleAttr: Int) {
        context.obtainStyledAttributes(attrs, R.styleable.MyInformationView, defStyleAttr, 0)
            .use { array ->
                bindLeftIcon(array)
                bindLeftText(array)
                bindRightText(array)
                bindArrow(array)
                binding.viewDivider.isVisible =
                    array.getBoolean(R.styleable.MyInformationView_is_show_line, false)

                val contentBackground =
                    array.getResourceId(R.styleable.MyInformationView_background, 0)
                if (contentBackground != 0) binding.informationContent.setBackgroundResource(
                    contentBackground
                )
            }
    }

    private fun bindLeftIcon(array: TypedArray) {
        val icon = array.getResourceId(R.styleable.MyInformationView_left_icon, 0)
        binding.imageLeftIcon.isVisible = icon != 0
        if (icon != 0) binding.imageLeftIcon.setImageResource(icon)
    }

    private fun bindLeftText(array: TypedArray) = with(binding.textLeft) {
        text = array.getText(R.styleable.MyInformationView_left_text)
        setTextColor(
            array.getColor(
                R.styleable.MyInformationView_left_text_color,
                color(R.color.color_17181c)
            )
        )
        applyTextSize(this, array, R.styleable.MyInformationView_left_text_size, 17)
    }

    private fun bindRightText(array: TypedArray) = with(binding.textRight) {
        val value = array.getText(R.styleable.MyInformationView_right_text)
        text = value
        isVisible = !value.isNullOrEmpty()
        setTextColor(
            array.getColor(
                R.styleable.MyInformationView_right_text_color,
                color(R.color.color_777b8d)
            )
        )
        applyTextSize(this, array, R.styleable.MyInformationView_right_text_size, 14)
    }

    private fun bindArrow(array: TypedArray) = with(binding.imageArrow) {
        val icon = array.getResourceId(R.styleable.MyInformationView_right_arrow_icon, 0)
        if (icon != 0) setImageResource(icon)
        visibility = if (array.getBoolean(
                R.styleable.MyInformationView_is_show_arrow_right, true
            )
        ) View.VISIBLE else View.GONE
    }

    private fun applyTextSize(view: TextView, array: TypedArray, index: Int, defaultSp: Int) {
        if (array.hasValue(index)) {
            view.setTextSize(TypedValue.COMPLEX_UNIT_PX, array.getDimension(index, sp(defaultSp)))
        }
    }

    private fun color(resource: Int) = ContextCompat.getColor(context, resource)

    private fun sp(value: Int) = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        value.toFloat(),
        resources.displayMetrics,
    )

    fun setLeftText(value: String?) {
        binding.textLeft.text = value.orEmpty()
    }

    fun setLeftTextValue(value: String?) = setLeftText(value)

    fun setRightText(value: String?) = setRightTextValue(value)

    fun setRightTextValue(value: String?) {
        binding.textRight.text = value.orEmpty()
        binding.textRight.isVisible = !value.isNullOrEmpty()
    }

    fun getRightTextValue(): String = binding.textRight.text.toString()

    fun setRightTextColor(color: Int) = binding.textRight.setTextColor(color)

    fun setRightTextVisibility(visibility: Int) {
        binding.textRight.visibility = visibility
    }
}
