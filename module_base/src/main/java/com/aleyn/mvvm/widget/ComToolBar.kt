package com.aleyn.mvvm.widget

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View.OnClickListener
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.aleyn.mvvm.R
import com.aleyn.mvvm.databinding.ViewToolbarComBinding

/** Common title bar backed by view_toolbar_com.xml. */
class ComToolBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding = ViewToolbarComBinding.inflate(
        LayoutInflater.from(context),
        this,
        true,
    )

    init {
        context.obtainStyledAttributes(attrs, R.styleable.ComToolBar).apply {
            setTitStr(getString(R.styleable.ComToolBar_tit_text))
            setTitleImageDrawable(getDrawable(R.styleable.ComToolBar_tit_image))
            if (hasValue(R.styleable.ComToolBar_tit_size)) {
                binding.tvTitle.setTextSize(
                    TypedValue.COMPLEX_UNIT_PX,
                    getDimension(R.styleable.ComToolBar_tit_size, binding.tvTitle.textSize),
                )
            }
            setLeftTitStr(getString(R.styleable.ComToolBar_tit_left_text))
            setLeftIconDrawable(getDrawable(R.styleable.ComToolBar_left_icon))
            setRightIconDrawable(getDrawable(R.styleable.ComToolBar_right_icon))
            setLineVisible(getBoolean(R.styleable.ComToolBar_show_line, false))
            recycle()
        }
    }

    fun setTitStr(title: String?): ComToolBar = apply {
        binding.tvTitle.text = title.orEmpty()
        if (!title.isNullOrEmpty()) {
            binding.tvTitle.isVisible = true
            binding.ivTitle.isVisible = false
        }
    }

    fun setTitleImageDrawable(drawable: Drawable?): ComToolBar = apply {
        binding.ivTitle.setImageDrawable(drawable)
        binding.ivTitle.isVisible = drawable != null
        binding.tvTitle.isVisible = drawable == null
    }

    fun setTitleImageResource(resId: Int): ComToolBar = apply {
        if (resId == 0) {
            setTitleImageDrawable(null)
        } else {
            binding.ivTitle.setImageResource(resId)
            binding.ivTitle.isVisible = true
            binding.tvTitle.isVisible = false
        }
    }

    fun setLeftTitStr(title: String?): ComToolBar = apply {
        binding.tvLeftTit.text = title.orEmpty()
        binding.tvLeftTit.isVisible = !title.isNullOrEmpty()
        if (!title.isNullOrEmpty()) binding.ivLeftIcon.isVisible = false
    }

    fun setLeftIconDrawable(drawable: Drawable?): ComToolBar = apply {
        if (drawable != null) {
            binding.ivLeftIcon.setImageDrawable(drawable)
            binding.ivLeftIcon.isVisible = true
            binding.tvLeftTit.isVisible = false
        }
    }

    fun setRightIconDrawable(drawable: Drawable?): ComToolBar = apply {
        binding.ivRightIcon.setImageDrawable(drawable)
        binding.ivRightIcon.isVisible = drawable != null
    }

    fun setRightIconDrawable(resId: Int): ComToolBar = apply {
        binding.ivRightIcon.isVisible = resId != 0
        if (resId != 0) binding.ivRightIcon.setImageResource(resId)
    }

    fun setTitleClickListener(listener: OnClickListener): ComToolBar = apply {
        binding.tvTitle.setOnClickListener(listener)
        binding.ivTitle.setOnClickListener(listener)
    }

    fun setLeftClickListener(listener: OnClickListener): ComToolBar = apply {
        binding.ivLeftIcon.setOnClickListener(listener)
        binding.tvLeftTit.setOnClickListener(listener)
    }

    fun setRightIconClickListener(listener: OnClickListener): ComToolBar = apply {
        binding.ivRightIcon.setOnClickListener(listener)
    }

    fun setTitSize(sizeSp: Float): ComToolBar = apply {
        binding.tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
    }

    fun setLeftTitSize(sizeSp: Float): ComToolBar = apply {
        binding.tvLeftTit.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
    }

    fun setToolBarBackground(color: Int) {
        binding.layoutContent.setBackgroundColor(color)
    }

    fun setLeftIconVisible(visible: Boolean): ComToolBar = apply {
        binding.ivLeftIcon.isVisible = visible
    }

    fun setRightIconVisible(visible: Boolean): ComToolBar = apply {
        binding.ivRightIcon.isVisible = visible
    }

    fun setLineVisible(visible: Boolean) {
        binding.titleBarUnderline.isVisible = visible
    }

    fun setLeftAndTit(listener: OnClickListener, title: String): ComToolBar = apply {
        setLeftClickListener(listener)
        setTitStr(title)
    }

    fun getTitTv(): TextView = binding.tvTitle
    fun getTitleImageIv(): ImageView = binding.ivTitle
    fun getTitLeftTv(): TextView = binding.tvLeftTit
    fun getLeftIconIv(): ImageView = binding.ivLeftIcon
    fun getRightIconIv(): ImageView = binding.ivRightIcon
}
