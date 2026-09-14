package com.aleyn.mvvm.widget

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.aleyn.mvvm.R

abstract class AbsRoundImageView @JvmOverloads constructor(
    context: Context?, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatImageView(context!!, attrs, defStyleAttr) {
    /**
     * 图片可视区
     */
    protected var roundPath: Path? = null

    /**
     * 图片边框
     */
    protected var borderPath: Path? = null

    /**
     * 边框宽度
     */
    protected var borderWidth = 0f

    /**
     * 边框颜色
     */
    protected var borderColor = 0
    private var borderPaint: Paint? = null
    protected open fun initAttrs(attrs: AttributeSet?) {
        context?.obtainStyledAttributes(attrs, R.styleable.AbsRoundImageView)?.let {
            borderWidth = it.getDimension(R.styleable.AbsRoundImageView_riv_borderWidth, 0f)
            borderColor = it.getColor(R.styleable.AbsRoundImageView_riv_borderColor, 0)
            it.recycle()
        }
    }

    private fun init() {
        roundPath = Path()
        borderPath = Path()
        borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        borderPaint!!.strokeWidth = borderWidth
        scaleType = ScaleType.CENTER_CROP
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed) {
            initBorderPath()
            initRoundPath()
        }
    }

    /**
     * 初始化边框Path
     */
    protected abstract fun initBorderPath()

    /**
     * 初始化图片区域Path
     */
    protected abstract fun initRoundPath()

    /**
     * 获取图片区域纯颜色Bitmap
     * @return
     */
    protected val roundBitmap: Bitmap
        protected get() {
            val bitmap = Bitmap.createBitmap(
                width, height, Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.color = Color.WHITE
            canvas.drawPath(roundPath!!, paint)
            return bitmap
        }

    private fun drawBorder(canvas: Canvas) {
        borderPaint!!.style = Paint.Style.STROKE
        borderPaint!!.color = borderColor
        canvas.drawPath(borderPath!!, borderPaint!!)
    }

    override fun onDraw(canvas: Canvas) {
        // 先交给 AppCompatImageView 按 scaleType（这里是 CENTER_CROP）居中缩放并绘制，
        // 再裁剪圆角区域。原实现直接以原图尺寸从左上角绘制，导致图片偏移和被截断。
        val saveCount = canvas.save()
        roundPath?.let(canvas::clipPath)
        super.onDraw(canvas)
        canvas.restoreToCount(saveCount)
        drawBorder(canvas)
    }

    init {
        initAttrs(attrs)
        init()
    }
}
