package com.aleyn.mvvm.widget

import android.content.Context
import android.graphics.Color
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.aleyn.mvvm.R
import kotlin.math.roundToInt

/**
 * @author lilingke
 * @date 2023/3/27
 * @description 内容+单位控件
 */
class ContentWithUnitTextView : LinearLayout {
    private var mContentSize: Float = 0f //正文字体大小
    private var mUnitSize: Float = 0f //单位字体大小
    private var mContentColor = 0 //正文字体颜色
    private var mUnitColor = 0 //单位字体颜色
    private var mContent = "" //正文内容
    private var mUnit = "" //单位内容

    private var tvContent: TextView? = null
    private var tvUnit: TextView? = null

    private var mIsHideUnit = false

    constructor(context: Context?) : this(context, null)
    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    ) {
        if (attrs != null) {
            initArrrs(attrs)
        }
        initView()
    }

    /**
     * 初始化自定义属性
     * @param 属性获取接口
     */
    private fun initArrrs(attrs: AttributeSet) {
        context.obtainStyledAttributes(attrs, R.styleable.ContentWithUnitTextView).apply {
            mContentSize = getDimension(R.styleable.ContentWithUnitTextView_tv_content_size, 16f)
            mContentColor = getColor(
                R.styleable.ContentWithUnitTextView_tv_content_color, Color.parseColor("#111113")
            )
            mContent = getString(R.styleable.ContentWithUnitTextView_tv_content).toString()
            mUnitSize = getDimension(R.styleable.ContentWithUnitTextView_tv_unit_size, 12f)
            mUnitColor = getColor(
                R.styleable.ContentWithUnitTextView_tv_unit_color, Color.parseColor("#111113")
            )
            mUnit = getString(R.styleable.ContentWithUnitTextView_tv_unit).toString()
            if (TextUtils.isEmpty(mUnit)) {
                mUnit = ""
            }
            recycle()
        }
    }

    /**
     * 初始化自定义View
     */
    private fun initView() {
        val view = inflate(context, R.layout.content_with_unit_view, this)
        tvContent = view.findViewById(R.id.tv_content)
        tvUnit = view.findViewById(R.id.tv_unit)
        setContent()
        setUnit()
    }

    /**
     * 设置内容属性
     */
    private fun setContent() {
        tvContent?.setTextSize(TypedValue.COMPLEX_UNIT_PX, mContentSize)
        tvContent?.setTextColor(mContentColor)
        tvContent?.text = mContent
    }

    /**
     * 设置单位属性
     */
    private fun setUnit() {
        tvUnit?.setTextSize(TypedValue.COMPLEX_UNIT_PX, mUnitSize)
        tvUnit?.setTextColor(mUnitColor)
        tvUnit?.text = mUnit
    }

    /**
     * 设置单位显示
     * @param isVisibility true:显示 false:隐藏
     */
    private fun setTvUnitVisibility(isVisibility: Boolean) {
        tvUnit?.isVisible = isVisibility && !mIsHideUnit
    }

    /**
     * 设置正文内容
     * @param content 正文内容
     */
    fun setTvContent(content: String?) {
        if (TextUtils.isEmpty(content)) {
            //内容为空,则内容正文显示"--" 隐藏单位
            tvContent?.text = "--"
        } else {
            //显示正文和单位
            tvContent?.text = content
        }
        setTvUnitVisibility(!TextUtils.isEmpty(content))
    }

    /**
     * 设置内容文本颜色
     * @param color 色值
     */
    fun setTvTextColor(color: Int) {
        tvContent?.setTextColor(context.getColor(color))
    }

    /**
     * 设置单位
     * @param unit 标题
     */
    fun setTvUnit(unit: String) {
        tvUnit?.text = unit
    }

    /**
     * 设置内容和单位的颜色
     */
    fun setContentColor(color: Int) {
        tvContent?.setTextColor(context.getColor(color))
        tvUnit?.setTextColor(context.getColor(color))
    }

    fun setIsHideUnit(isHide: Boolean) {
        mIsHideUnit = isHide
    }

    fun refreshLayout(isOnline: Boolean) {
        if (!isOnline) {
            setTvContent(null)
            tvContent?.setTextColor(context.getColor(R.color.c_ffb6b8c0))
        } else {
            tvContent?.setTextColor(context.getColor(R.color.c_111113))
        }
    }
}

fun ContentWithUnitTextView.setCelsius(celsius: Float?, type: Boolean) {
    if (celsius == null) {
        this.setTvContent(null)
        return
    }

    if (type) {
        var value = (celsius * 10).roundToInt() / 10f
        this.setTvContent("$value")
        this.setTvUnit("℃")
    } else {
        var num = celsius * 1.8f + 32
        var value = (num * 10).roundToInt() / 10f
        this.setTvContent("$value")
        this.setTvUnit("℉")
    }
}