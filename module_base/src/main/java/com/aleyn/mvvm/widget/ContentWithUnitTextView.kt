package com.aleyn.mvvm.widget

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.aleyn.mvvm.R
import kotlin.math.roundToInt

/** Displays a value and its unit with independently configurable text styles. */
class ContentWithUnitTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {
    private var contentSize = sp(16f)
    private var unitSize = sp(12f)
    private var contentColor = Color.parseColor("#111113")
    private var unitColor = Color.parseColor("#111113")
    private var content = ""
    private var unit = ""
    private var hideUnit = false
    private var showUnitWhenEmpty = false

    private lateinit var contentView: TextView
    private lateinit var unitView: TextView

    init {
        if (attrs != null) readAttributes(attrs)
        initView()
    }

    private fun readAttributes(attrs: AttributeSet) {
        context.obtainStyledAttributes(attrs, R.styleable.ContentWithUnitTextView).apply {
            contentSize = getDimension(R.styleable.ContentWithUnitTextView_tv_content_size, contentSize)
            contentColor = getColor(R.styleable.ContentWithUnitTextView_tv_content_color, contentColor)
            content = getString(R.styleable.ContentWithUnitTextView_tv_content).orEmpty()
            unitSize = getDimension(R.styleable.ContentWithUnitTextView_tv_unit_size, unitSize)
            unitColor = getColor(R.styleable.ContentWithUnitTextView_tv_unit_color, unitColor)
            unit = getString(R.styleable.ContentWithUnitTextView_tv_unit).orEmpty()
            showUnitWhenEmpty = getBoolean(
                R.styleable.ContentWithUnitTextView_tv_show_unit_when_empty,
                false,
            )
            recycle()
        }
    }

    private fun initView() {
        inflate(context, R.layout.content_with_unit_view, this)
        contentView = findViewById(R.id.tv_content)
        unitView = findViewById(R.id.tv_unit)
        contentView.setTextSize(TypedValue.COMPLEX_UNIT_PX, contentSize)
        contentView.setTextColor(contentColor)
        unitView.setTextSize(TypedValue.COMPLEX_UNIT_PX, unitSize)
        unitView.setTextColor(unitColor)
        unitView.text = unit
        setTvContent(content.ifEmpty { null })
    }

    /** Null or empty content is rendered as "--". */
    fun setTvContent(value: String?) {
        content = value.orEmpty()
        contentView.text = value?.takeIf(String::isNotEmpty) ?: "--"
        updateUnitVisibility()
    }

    fun setTvUnit(value: String) {
        unit = value
        unitView.text = value
        updateUnitVisibility()
    }

    fun setContentTextSize(sizeSp: Float) {
        contentView.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
    }

    fun setUnitTextSize(sizeSp: Float) {
        unitView.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
    }

    fun setContentTextColor(color: Int) = contentView.setTextColor(color)

    fun setUnitTextColor(color: Int) = unitView.setTextColor(color)

    fun setShowUnitWhenEmpty(show: Boolean) {
        showUnitWhenEmpty = show
        updateUnitVisibility()
    }

    /** Kept for source compatibility; [colorRes] must be a color resource. */
    fun setTvTextColor(colorRes: Int) = contentView.setTextColor(context.getColor(colorRes))

    /** Kept for source compatibility; [colorRes] must be a color resource. */
    fun setContentColor(colorRes: Int) {
        val color = context.getColor(colorRes)
        contentView.setTextColor(color)
        unitView.setTextColor(color)
    }

    fun setIsHideUnit(isHide: Boolean) {
        hideUnit = isHide
        updateUnitVisibility()
    }

    fun refreshLayout(isOnline: Boolean) {
        if (!isOnline) {
            setTvContent(null)
            contentView.setTextColor(context.getColor(R.color.color_ffb6b8c0))
        } else {
            contentView.setTextColor(context.getColor(R.color.color_111113))
        }
    }

    private fun updateUnitVisibility() {
        unitView.isVisible = !hideUnit && unit.isNotEmpty() &&
            (content.isNotEmpty() || showUnitWhenEmpty)
    }

    private fun sp(value: Float): Float = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        value,
        resources.displayMetrics,
    )
}

fun ContentWithUnitTextView.setCelsius(celsius: Float?, type: Boolean) {
    if (celsius == null) {
        setTvContent(null)
        return
    }
    if (type) {
        setTvContent("${(celsius * 10).roundToInt() / 10f}")
        setTvUnit("℃")
    } else {
        setTvContent("${((celsius * 1.8f + 32) * 10).roundToInt() / 10f}")
        setTvUnit("℉")
    }
}
