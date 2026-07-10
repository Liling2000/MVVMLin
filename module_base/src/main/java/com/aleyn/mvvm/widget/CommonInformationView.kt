package com.aleyn.mvvm.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.aleyn.mvvm.R
import com.aleyn.mvvm.databinding.LayoutCommonInformationBinding
import com.aleyn.mvvm.utils.DensityUtil
import com.orhanobut.logger.Logger


/**
 * @des
 * @user liling
 * @Date 2023/5/22
 * 无法适配那种左边的标题内容很长，右边的文案内容也同样很长需要换行的item，
 * 因为左边的textview长度不固定，右边的textview长度也不固定，所以无法确定位置
 */
class CommonInformationView : LinearLayout {
    private val TAG = "CommonInformationView"
    private var mBackgroundColor = 0
    private var mCornerRadius = 0
    private var mCornerRadius_TL = 0
    private var mCornerRadius_TR = 0
    private var mCornerRadius_BL = 0
    private var mCornerRadius_BR = 0
    private var mHorizontalMargin = 0
    private var mVerticalMargin = 0

    private var mViewBinding: LayoutCommonInformationBinding? = null
    private var mSwitchListener: OnSwitchButtonClickListener? = null


    constructor(context: Context?) : this(context, null)
    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    ) {
        init(context, attrs, defStyleAttr)
    }

    private fun init(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) {
        var view = LayoutInflater.from(context).inflate(R.layout.layout_common_information, this)
        mViewBinding = LayoutCommonInformationBinding.bind(view)

        val typedArray = context?.obtainStyledAttributes(attrs, R.styleable.CommonInformationView)
        typedArray?.let {
            mBackgroundColor =
                it.getColor(R.styleable.CommonInformationView_rv_backgroundColor, Color.TRANSPARENT)

            mCornerRadius = DensityUtil.px2dip(
                context,
                it.getDimensionPixelSize(R.styleable.CommonInformationView_rv_cornerRadius, 0)
                    .toFloat()
            )

            mCornerRadius_TL =
                it.getDimensionPixelSize(R.styleable.CommonInformationView_rv_cornerRadius_TL, 0)
            mCornerRadius_TR =
                it.getDimensionPixelSize(R.styleable.CommonInformationView_rv_cornerRadius_TR, 0)
            mCornerRadius_BL =
                it.getDimensionPixelSize(R.styleable.CommonInformationView_rv_cornerRadius_BL, 0)
            mCornerRadius_BR =
                it.getDimensionPixelSize(R.styleable.CommonInformationView_rv_cornerRadius_BR, 0)

            //水平方向的margin
            mHorizontalMargin = it.getDimensionPixelSize(
                R.styleable.CommonInformationView_rv_horizontal_margin, 0
            )

            //竖直方向的margin
            mVerticalMargin = it.getDimensionPixelSize(
                R.styleable.CommonInformationView_rv_vertical_margin, 0
            )

            //左边的图标
            val leftIcon = it.getResourceId(R.styleable.CommonInformationView_rv_leftIcon, 0)
            if (leftIcon != 0) {
                mViewBinding?.ivLeft?.setImageResource(leftIcon)
                mViewBinding?.ivLeft?.isGone = false
            }

            //左边标题
            val leftText = it.getResourceId(R.styleable.CommonInformationView_rv_leftText, 0)
            val leftTextColor = it.getColor(R.styleable.CommonInformationView_rv_leftTextColor, 0)
            val leftSize = it.getDimensionPixelSize(
                R.styleable.CommonInformationView_rv_leftTextSize, 0
            )
            if (leftText != 0) {
                mViewBinding?.tvTitle?.text = context.resources.getString(leftText, "")
            }
            if (leftTextColor != 0) {
                mViewBinding?.tvTitle?.setTextColor(leftTextColor)
            }
            if (leftSize > 0) {
                mViewBinding?.tvTitle?.textSize =
                    DensityUtil.px2dip(context, leftSize.toFloat()).toFloat()
            }

            //左边提示语
            val leftTextTip = it.getResourceId(R.styleable.CommonInformationView_rv_leftTextTip, 0)
            val leftTextTipColor =
                it.getColor(R.styleable.CommonInformationView_rv_leftTextTipColor, 0)
            val leftTipSize = it.getDimensionPixelSize(
                R.styleable.CommonInformationView_rv_leftTextTipSize, 0
            )
            if (leftTextTip != 0) {
                mViewBinding?.tvTip?.text = context.resources.getString(leftTextTip, "")
                mViewBinding?.tvTip?.isGone = false
            } else {
                mViewBinding?.tvTip?.isGone = true
            }
            if (leftTextTipColor != 0) {
                mViewBinding?.tvTip?.setTextColor(leftTextTipColor)
            }
            if (leftTipSize > 0) {
                mViewBinding?.tvTip?.textSize =
                    DensityUtil.px2sp(context, leftTipSize.toFloat()).toFloat()
            }

            //右边内容
            val rightText = it.getResourceId(R.styleable.CommonInformationView_rv_rightText, 0)
            val rightTextColor = it.getColor(R.styleable.CommonInformationView_rv_rightTextColor, 0)
            val rightSize = it.getDimensionPixelSize(
                R.styleable.CommonInformationView_rv_rightTextSize, 0
            )
            if (rightText != 0) {
                mViewBinding?.tvRightContent?.text = context.resources.getString(rightText, "")
            }
            if (rightTextColor != 0) {
                mViewBinding?.tvRightContent?.setTextColor(rightTextColor)
            }
            if (rightSize > 0) {
                mViewBinding?.tvRightContent?.textSize =
                    DensityUtil.px2sp(context, rightSize.toFloat()).toFloat()
            }
            var rightTextMaxWidth = it.getDimensionPixelSize(
                R.styleable.CommonInformationView_rv_rightTextMaxWidth, 0
            )
            if (rightTextMaxWidth > 0) {
                mViewBinding?.tvRightContent?.maxWidth = rightTextMaxWidth
            }

            //是否显示向右的按钮
            val isShowArrowRight =
                it.getBoolean(R.styleable.CommonInformationView_rv_isShowArrowRight, true)
            mViewBinding?.ivArrow?.isGone = !isShowArrowRight

            //向右的按钮图标
            val arrowRightIcon =
                it.getResourceId(R.styleable.CommonInformationView_rv_arrowRightIcon, 0)
            if (arrowRightIcon != 0) {
                mViewBinding?.ivArrow?.setImageResource(arrowRightIcon)
            }

            //是否显示SwitchButton
            val isShowSwitchButton =
                it.getBoolean(R.styleable.CommonInformationView_rv_isShowSwitchButton, false)
            if (isShowSwitchButton) {
                mViewBinding?.cbSwitch?.isGone = false
                mViewBinding?.layoutRight?.isGone = true
            }

            //是否checkBox
            val isShowCheckBox =
                it.getBoolean(R.styleable.CommonInformationView_rv_isShowCheckBox, false)
            if (isShowCheckBox) {
                mViewBinding?.cbCheck?.isGone = false
                mViewBinding?.cbSwitch?.isGone = true
                mViewBinding?.layoutRight?.isGone = true
            }

            //是否显示底部分割线
            val isShowLine = it.getBoolean(R.styleable.CommonInformationView_rv_isShowLine, false)
            mViewBinding?.viewLine?.isGone = !isShowLine

            //是否标题右边的帮助按钮
            val isShowHelp =
                it.getBoolean(R.styleable.CommonInformationView_rv_isShowHelpIcon, false)
            mViewBinding?.imgHelp?.isGone = !isShowHelp

            it.recycle()
        }

        refreshLayout()
        initListener()
    }

    private fun refreshLayout() {
        if (mHorizontalMargin > 0 || mVerticalMargin > 0) {
            val params = mViewBinding?.layoutChildContent?.layoutParams as MarginLayoutParams
            if (mHorizontalMargin > 0) {
                params.leftMargin = mHorizontalMargin
                params.rightMargin = mHorizontalMargin
            }

            if (mVerticalMargin > 0) {
                params.topMargin = mVerticalMargin
                params.bottomMargin = mVerticalMargin
            }
            mViewBinding?.layoutChildContent?.layoutParams = params
        }

        setLayoutBackgroundColor(mBackgroundColor)
        if (mCornerRadius > 0) {
            setCornerRadius(mCornerRadius)
        } else {
            if (mCornerRadius_TL > 0) setCornerRadius_TL(mCornerRadius_TL)
            if (mCornerRadius_TR > 0) setCornerRadius_TR(mCornerRadius_TR)
            if (mCornerRadius_BL > 0) setCornerRadius_BL(mCornerRadius_BL)
            if (mCornerRadius_BR > 0) setCornerRadius_BR(mCornerRadius_BR)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initListener() {
        mViewBinding?.cbSwitch?.setOnClickListener {
            mSwitchListener?.onSwitchClick()
        }
    }

    fun setLayoutBackgroundColor(backgroundColor: Int) {
        mBackgroundColor = backgroundColor
        mViewBinding?.layoutContent?.delegate?.backgroundColor = mBackgroundColor
    }

    fun setCornerRadius(cornerRadius: Int) {
        mCornerRadius = cornerRadius
        mViewBinding?.layoutContent?.delegate?.cornerRadius = mCornerRadius
        Logger.e("LLK setCornerRadius  cornerRadius = ${mViewBinding?.layoutContent?.delegate?.cornerRadius}")
    }

    fun setCornerRadius_TL(cornerRadius_TL: Int) {
        mCornerRadius_TL = cornerRadius_TL
        mViewBinding?.layoutContent?.delegate?.cornerRadius_TL = mCornerRadius_TL
    }

    fun setCornerRadius_TR(cornerRadius_TR: Int) {
        mCornerRadius_TR = cornerRadius_TR
        mViewBinding?.layoutContent?.delegate?.cornerRadius_TR = cornerRadius_TR
    }

    fun setCornerRadius_BL(cornerRadius_BL: Int) {
        mCornerRadius_BL = cornerRadius_BL
        mViewBinding?.layoutContent?.delegate?.cornerRadius_BL = cornerRadius_BL
    }

    fun setCornerRadius_BR(cornerRadius_BR: Int) {
        mCornerRadius_BR = cornerRadius_BR
        mViewBinding?.layoutContent?.delegate?.cornerRadius_BR = cornerRadius_BR
    }

    fun setLeftTextColor(color: Int) {
        mViewBinding?.tvTitle?.setTextColor(color)
    }

    fun setLeftTextValue(text: String?) {
        mViewBinding?.tvTitle?.text = text ?: ""
    }

    fun setRightTextVisibility(visibility: Int) {
//        text_right.visibility = visibility
    }

    fun setTextTipColor(color: Int) {
        mViewBinding?.tvTip?.setTextColor(color)
    }

    fun setLeftTextTip(text: String?) {
        mViewBinding?.tvTip?.text = text ?: ""
    }

    fun setRightTextValue(text: String?) {
        mViewBinding?.tvRightContent?.text = text ?: ""
    }

    fun getRightTextValue(): String {
        return mViewBinding?.tvRightContent?.text?.toString() ?: ""
    }

    fun setImageOvealVisible(isVisible: Boolean) {
        mViewBinding?.ivOveal?.isGone = !isVisible
    }

    fun getSwitchCheckState(): Boolean {
        return mViewBinding?.cbSwitch?.isSelected == true
    }

    fun setSwitchCheckState(isCheck: Boolean) {
        mViewBinding?.cbSwitch?.isSelected = isCheck
    }

    fun setSwitchEnableState(isEnable: Boolean) {
        mViewBinding?.cbSwitch?.isEnabled = isEnable
    }

    fun setContentEnableState(isEnable: Boolean) {
        mViewBinding?.rootView?.isEnabled = isEnable
    }

    fun getCheckBoxCheckState(): Boolean {
        return mViewBinding?.cbCheck?.isSelected == true
    }

    fun setCheckBoxCheckState(isCheck: Boolean) {
        mViewBinding?.cbCheck?.isSelected = isCheck
    }

    fun setCheckBoxEnableState(isEnable: Boolean) {
        mViewBinding?.rootView?.isEnabled = isEnable
    }

    fun getHelpImage(): ImageView? {
        return mViewBinding?.imgHelp
    }

    /**
     * 是显示switch按钮还是向右按钮
     * @param isShow true: 显示switch按钮， false：显示向右按钮
     */
    fun showSwitchCheckView(isShow: Boolean) {
        mViewBinding?.apply {
            cbSwitch.isVisible = isShow
            layoutRight.isVisible = !isShow
            ivArrow.isVisible = !isShow
        }
    }

    interface OnSwitchButtonClickListener {
        fun onSwitchClick()
    }

    fun setOnSwitchButtonClickListener(listener: OnSwitchButtonClickListener) {
        mSwitchListener = listener
    }

}