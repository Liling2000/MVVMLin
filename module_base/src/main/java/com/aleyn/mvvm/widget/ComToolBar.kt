package com.aleyn.mvvm.widget

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.aleyn.mvvm.R
import com.aleyn.mvvm.databinding.ViewToolbarComBinding
import com.aleyn.mvvm.utils.DensityUtil

/**
 * 通用标题栏
 */
class ComToolBar : ConstraintLayout {
    private var mViewBinding: ViewToolbarComBinding? = null

    var mTitContent: String? = ""
    var mTitSize: Int? = -1
    var leftTit: String? = ""
    var rightTit: String? = ""
    var leftIcon: Drawable? = null
    var rightIcon: Drawable? = null
    var isShowLine: Boolean? = null

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    ) {
        initTypedArray(context, attrs)
        initView(context)
        init()
    }

    /**
     * 初始化内容
     */
    private fun init() {
        //设置标题
        setTitStr(mTitContent)

        //设置标题大小
        if (mTitSize != -1) {
            setTitSize(resources.getDimension(mTitSize!!))
        }

        //设置左图标
        setLeftIconDrawable(leftIcon)

        //设置右图标
        setRightIconDrawable(rightIcon)

        //设置左文字
        setLeftTitStr(leftTit)

        //设置右文字
        setRightTitStr(rightTit)

        //是否显示下划线
        setLineVisible(isShowLine)

    }

    private fun initView(context: Context) {
        val view = LayoutInflater.from(context).inflate(R.layout.view_toolbar_com, this, true)
        mViewBinding = ViewToolbarComBinding.bind(view)
    }

    /**
     * 初始化属性
     */
    private fun initTypedArray(context: Context, attrs: AttributeSet?) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ComToolBar)
        typedArray?.run {
            mTitContent = typedArray.getString(R.styleable.ComToolBar_tit_text)           //标题    内容
            mTitSize = typedArray.getResourceId(
                R.styleable.ComToolBar_tit_size, -1
            )    //标题    大小 (Use getResourceId since it was expected to be a dimension)
            leftTit = typedArray.getString(R.styleable.ComToolBar_tit_left_text)          //左标题  内容
            rightTit = typedArray.getString(R.styleable.ComToolBar_tit_right_text)        //右标题  内容
            leftIcon = typedArray.getDrawable(R.styleable.ComToolBar_left_icon)           //左图标  内容
            rightIcon = typedArray.getDrawable(R.styleable.ComToolBar_right_icon)         //右图标  内容
            isShowLine = typedArray.getBoolean(
                R.styleable.ComToolBar_show_line, false
            )          //是否显示下划线 默认不显示

            recycle()
        }
    }

    /**
     * 设置左侧标题文字
     */
    fun setLeftTitStr(tit: String?): ComToolBar {
        mViewBinding?.apply {
            if (!tit.isNullOrEmpty()) {
                ivLeftIcon.visibility = GONE
                tvLeftTit.visibility = VISIBLE
                tvLeftTit.text = tit
            }
        }
        return this
    }

    /**
     * 设置右侧标题文字
     */
    fun setRightTitStr(tit: String?): ComToolBar {
        mViewBinding?.apply {
            if (!tit.isNullOrEmpty()) {
                ivRightIcon.visibility = GONE
                tvRight.visibility = VISIBLE
                tvRight.text = tit
            }
        }
        return this
    }

    /**
     * 设置中间标题的内容
     */
    fun setTitStr(tit: String?): ComToolBar {
        mViewBinding?.tvTit?.text = tit ?: ""
        return this
    }

    /**
     * 设置toolBar的背景
     */
    fun setToolBarBackground(rid: Int) {
        mViewBinding?.layoutContent?.setBackgroundColor(rid)
    }

    /**
     * 设置左图标
     */
    fun setLeftIconDrawable(drawable: Drawable?): ComToolBar {
        mViewBinding?.apply {
            if (drawable != null) {
                ivLeftIcon.visibility = VISIBLE
                tvLeftTit.visibility = GONE
                ivLeftIcon.setImageDrawable(drawable)
            }
        }
        return this
    }

    /**
     * 设置右图标
     */
    fun setRightIconDrawable(drawable: Drawable?): ComToolBar {
        mViewBinding?.apply {
            if (drawable != null) {
                ivRightIcon.visibility = VISIBLE
                tvRight.visibility = GONE
                ivRightIcon.setImageDrawable(drawable)
            }
        }
        return this
    }

    /**
     * 设置右图标 可用于清除图片
     */
    fun setRightIconDrawable(resInt: Int): ComToolBar {
        mViewBinding?.apply {
            ivRightIcon.isVisible = resInt > 0
            tvRight.visibility = GONE
            ivRightIcon.setImageResource(resInt)
        }
        return this
    }

    /**
     * 设置按钮大小
     */
    fun setRightIconSize(width: Float, height: Float): ComToolBar {
        mViewBinding?.ivRightIcon?.layoutParams = mViewBinding?.ivRightIcon?.layoutParams?.apply {
            this.width = DensityUtil.dip2px(context, width)
            this.height = DensityUtil.dip2px(context, height)
        }
        return this
    }

    /**
     * 设置按钮大小
     */
    fun setRightIconSizePx(width: Int, height: Int): ComToolBar {
        mViewBinding?.ivRightIcon?.layoutParams = mViewBinding?.ivRightIcon?.layoutParams?.apply {
            this.width = width
            this.height = height
        }
        return this
    }

    /**
     * 设置右边的第二个图标的大小
     * @param width Int
     * @param height Int
     * @return ComToolBar
     */
    fun setSecondRightIconSizePx(width: Int, height: Int): ComToolBar {
        mViewBinding?.ivSecondRightIcon?.layoutParams =
            mViewBinding?.ivSecondRightIcon?.layoutParams?.apply {
                this.width = width
                this.height = height
            }
        return this
    }

    fun setTitleClickListener(click: OnClickListener): ComToolBar {
        mViewBinding?.tvTit?.setOnClickListener(click)
        return this
    }

    /**
     * 左边按钮的点击事件
     */
    fun setLeftClickListener(click: OnClickListener): ComToolBar {
        mViewBinding?.ivLeftIcon?.setOnClickListener(click)
        return this
    }

    /**
     * 左二按钮点击事件
     */
    fun setSecondLeftClickListener(click: OnClickListener): ComToolBar {
        mViewBinding?.ivLeftIcon2?.setOnClickListener(click)
        return this
    }

    /**
     * 右边按钮的点击事件
     */
    fun setRightIconClickListener(click: OnClickListener): ComToolBar {
        mViewBinding?.ivRightIcon?.setOnClickListener(click)
        return this
    }

    /**
     * 右边第二个按钮的点击事件
     */
    fun setSecondRightIconClickListener(click: OnClickListener): ComToolBar {
        mViewBinding?.ivSecondRightIcon?.setOnClickListener(click)
        return this
    }

    /**
     * 右边文字的点击事件
     */
    fun setRightTextClickListener(click: OnClickListener): ComToolBar {
        mViewBinding?.tvRight?.setOnClickListener(click)
        return this
    }


    /**
     * 设置中间标题的大小
     */
    fun setTitSize(size: Float): ComToolBar {
        mViewBinding?.tvTit?.textSize = size
        return this
    }

    /**
     * 设置右边标题的大小
     */
    fun setLeftTitSize(size: Float): ComToolBar {
        mViewBinding?.tvLeftTit?.textSize = size
        return this
    }

    /**
     * 设置右边标题的大小
     */
    fun setRightTitSize(size: Float): ComToolBar {
        mViewBinding?.tvRight?.textSize = size
        return this
    }

    /**
     * 设置右边标题的颜色
     */
    fun setRightTitColor(color: Int): ComToolBar {
        mViewBinding?.tvRight?.setTextColor(color)
        return this
    }


    /**
     * 获取中间文字控件
     */
    fun getTitTv(): TextView? {
        return mViewBinding?.tvTit
    }


    /**
     * 获取左侧文字控件
     */
    fun getTitLeftTv(): TextView? {
        return mViewBinding?.tvLeftTit
    }

    /**
     * 获取右侧文字控件
     */
    fun getTitRightTv(): TextView? {
        return mViewBinding?.tvRight
    }

    /**
     * 获取左侧图标控件
     */
    fun getLeftIconIv(): ImageView? {
        return mViewBinding?.ivLeftIcon
    }

    /**
     * 获取右侧图标控件
     */
    fun getRightIconIv(): ImageView? {
        return mViewBinding?.ivRightIcon
    }

    fun getSecondRightIconIv(): ImageView? {
        return mViewBinding?.ivSecondRightIcon
    }

    /**
     * 设置左侧图标是否可见
     */
    fun setLeftIconVisible(isVisible: Boolean): ComToolBar {
        mViewBinding?.ivLeftIcon?.visibility = if (isVisible) VISIBLE else INVISIBLE
        return this
    }

    fun setSecondLeftIconVisible(isVisible: Boolean): ComToolBar {
        mViewBinding?.ivLeftIcon2?.isVisible = isVisible
        return this
    }

    /**
     * 设置右侧图标是否可见
     */
    fun setRightIconVisible(isVisible: Boolean): ComToolBar {
        mViewBinding?.ivRightIcon?.visibility = if (isVisible) VISIBLE else GONE
        return this
    }

    /**
     * 设置右侧第二个图标是否可见
     */
    fun setSecondRightIconVisible(isVisible: Boolean): ComToolBar {
        mViewBinding?.ivSecondRightIcon?.visibility = if (isVisible) VISIBLE else INVISIBLE
        return this
    }

    /**
     * 设置右侧图标是否可见
     */
    fun setRightTitleVisible(isVisible: Boolean): ComToolBar {
        mViewBinding?.tvRight?.visibility = if (isVisible) VISIBLE else INVISIBLE
        return this
    }


    /**
     * 设置左侧点击事件和中间标题
     */
    fun setLeftAndTit(click: OnClickListener, tit: String): ComToolBar {
        setLeftClickListener(click)
        setTitStr(tit)
        return this
    }

    /**
     * 是否显示下划线
     */
    fun setLineVisible(showLine: Boolean?) {
        mViewBinding?.titleBarUnderline?.isVisible = showLine ?: false
    }
}
