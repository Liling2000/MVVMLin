package com.aleyn.mvvm.widget

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.widget.Checkable
import androidx.core.animation.doOnEnd
import com.orhanobut.logger.Logger
import androidx.core.graphics.toColorInt

import androidx.core.content.withStyledAttributes
import com.aleyn.mvvm.R

@SuppressLint("CustomViewStyleable")
class SwitchButtonKotlin @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), Checkable {

    private var checked = false

    /**
     * 当前动画进度
     * 0f = 关闭
     * 1f = 开启
     */
    private var progress = 0f

    /**
     * 目标状态
     */
    private var targetChecked = false

    // ===== 可配置属性 =====
    private var uncheckedColor = "#C6C6C6".toColorInt()
    private var checkedColor = "#000000".toColorInt()
    private var checkedThumbColor = "#ffffff".toColorInt()
    private var uncheckedThumbColor = Color.WHITE

    private var radius = -1f
    private var enableEffect = true

    private var animator: ValueAnimator? = null

    private var onCheckedChangeListener: ((Boolean) -> Unit)? = null
    private var onClickListener: (() -> Unit)? = null

    init {
        isClickable = true

        attrs?.let {
            context.withStyledAttributes(it, R.styleable.SwitchButtonKotlin) {
                uncheckedColor = getColor(
                    R.styleable.SwitchButtonKotlin_sb_uncheckedColor, uncheckedColor
                )

                checkedColor = getColor(
                    R.styleable.SwitchButtonKotlin_sb_checkedColor, checkedColor
                )

                checkedThumbColor = getColor(
                    R.styleable.SwitchButtonKotlin_sb_thumbColor, checkedThumbColor
                )

                uncheckedThumbColor = getColor(
                    R.styleable.SwitchButtonKotlin_sb_un_thumbColor, uncheckedThumbColor
                )

                radius = getDimension(
                    R.styleable.SwitchButtonKotlin_sb_radius, -1f
                )

                enableEffect = getBoolean(
                    R.styleable.SwitchButtonKotlin_sb_enableEffect, true
                )
            }
        }
    }

    override fun isChecked(): Boolean = checked

    override fun toggle() {
        setChecked(!checked)
    }

    override fun setChecked(checked: Boolean) {
        Logger.e("SwitchButtonKotlin setChecked = $checked")
        // 状态一致且没动画直接返回
        if (this.checked == checked && animator?.isRunning != true) {
            Logger.e("SwitchButtonKotlin setChecked = $checked return")
            return
        }

        this.checked = checked
        this.targetChecked = checked

        animateToState(checked)
    }

    private fun animateToState(checked: Boolean) {
        animator?.cancel()

        val end = if (checked) 1f else 0f

        if (!enableEffect) {
            progress = end
            invalidate()
            onCheckedChangeListener?.invoke(this.checked)
            return
        }

        animator = ValueAnimator.ofFloat(progress, end).apply {
            duration = 180

            addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
            }

            doOnEnd {
                progress = end
                invalidate()
                onCheckedChangeListener?.invoke(this@SwitchButtonKotlin.checked)
            }

            start()
        }
    }

    fun setEnableEffect(enable: Boolean) {
        enableEffect = enable
    }

    fun setOnCheckedChangeListener(listener: (Boolean) -> Unit) {
        onCheckedChangeListener = listener
    }

    fun setOnClickListener(listener: () -> Unit) {
        onClickListener = listener
    }

    override fun performClick(): Boolean {
        onClickListener?.invoke()
        return super.performClick()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        drawTrack(canvas)
        drawThumb(canvas)
    }

    private fun drawTrack(canvas: Canvas) {
        val radius = height / 2f

        val color = ArgbEvaluator().evaluate(
            progress, uncheckedColor, checkedColor
        ) as Int

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            this.color = color
        }

        canvas.drawRoundRect(
            0f, 0f, width.toFloat(), height.toFloat(), radius, radius, paint
        )
    }

    private fun drawThumb(canvas: Canvas) {
        val radius = height / 2f - 2.5f.dp

        val startX = height / 2f
        val endX = width - height / 2f

        val cx = startX + (endX - startX) * progress

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = checkedThumbColor
            style = Paint.Style.FILL
        }

        canvas.drawCircle(cx, height / 2f, radius, paint)
    }

    private val Float.dp: Float
        get() = this * resources.displayMetrics.density
}