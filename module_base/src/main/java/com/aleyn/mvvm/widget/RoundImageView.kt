package com.aleyn.mvvm.widget

import android.content.Context
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import com.aleyn.mvvm.R

class RoundImageView @JvmOverloads constructor(
    context: Context?, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AbsRoundImageView(context, attrs, defStyleAttr) {
    private var radius = 0f
    private var leftTopRadius = 0f
    private var rightTopRadius = 0f
    private var rightBottomRadius = 0f
    private var leftBottomRadius = 0f
    override fun initAttrs(attrs: AttributeSet?) {
        super.initAttrs(attrs)
        attrs?.let { attr ->
            context?.obtainStyledAttributes(attr, R.styleable.RoundImageView)?.let {
                radius = it.getDimension(R.styleable.RoundImageView_riv_radius, 0f)
                leftTopRadius =
                    it.getDimension(R.styleable.RoundImageView_riv_leftTopRadius, radius)
                rightTopRadius =
                    it.getDimension(R.styleable.RoundImageView_riv_rightTopRadius, radius)
                rightBottomRadius =
                    it.getDimension(R.styleable.RoundImageView_riv_rightBottomRadius, radius)
                leftBottomRadius =
                    it.getDimension(R.styleable.RoundImageView_riv_leftBottomRadius, radius)
                it.recycle()
            }
        }

    }

    override fun initRoundPath() {
        roundPath?.reset()
        val width = width
        val height = height
        leftTopRadius = leftTopRadius.coerceAtMost(width.coerceAtMost(height) * 0.5f)
        rightTopRadius = rightTopRadius.coerceAtMost(width.coerceAtMost(height) * 0.5f)
        rightBottomRadius = rightBottomRadius.coerceAtMost(width.coerceAtMost(height) * 0.5f)
        leftBottomRadius = leftBottomRadius.coerceAtMost(width.coerceAtMost(height) * 0.5f)
        val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
        roundPath?.addRoundRect(
            rect, floatArrayOf(
                leftTopRadius,
                leftTopRadius,
                rightTopRadius,
                rightTopRadius,
                rightBottomRadius,
                rightBottomRadius,
                leftBottomRadius,
                leftBottomRadius
            ), Path.Direction.CW
        )
    }

    override fun initBorderPath() {
        borderPath?.reset()
        /**
         * 乘以0.5会导致border在圆角处不能包裹原图
         */
        val halfBorderWidth = borderWidth * 0.35f
        val width = width
        val height = height
        leftTopRadius = leftTopRadius.coerceAtMost(width.coerceAtMost(height) * 0.5f)
        rightTopRadius = rightTopRadius.coerceAtMost(width.coerceAtMost(height) * 0.5f)
        rightBottomRadius = rightBottomRadius.coerceAtMost(width.coerceAtMost(height) * 0.5f)
        leftBottomRadius = leftBottomRadius.coerceAtMost(width.coerceAtMost(height) * 0.5f)
        val rect = RectF(
            halfBorderWidth, halfBorderWidth, width - halfBorderWidth, height - halfBorderWidth
        )
        borderPath?.addRoundRect(
            rect, floatArrayOf(
                leftTopRadius,
                leftTopRadius,
                rightTopRadius,
                rightTopRadius,
                rightBottomRadius,
                rightBottomRadius,
                leftBottomRadius,
                leftBottomRadius
            ), Path.Direction.CW
        )
    }

    init {
        initAttrs(attrs!!)
    }
}