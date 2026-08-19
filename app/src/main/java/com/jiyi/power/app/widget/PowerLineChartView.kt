package com.jiyi.power.app.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.jiyi.power.R
import com.jiyi.power.app.bean.ChartPoint
import kotlin.math.max

class PowerLineChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val chartRect = RectF()
    private val linePath = Path()
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.color_8f94a6)
        textSize = sp(10f)
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.color_eef0f4)
        strokeWidth = dp(1f)
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.color_0054b8)
        strokeWidth = dp(2f)
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private var points: List<ChartPoint> = emptyList()
    private var maxY = 400f
    private var yItemCount = 5
    private var minX = 0f
    private var maxX = 24f
    private var xLabels = listOf("00:00", "06:00", "12:00", "18:00", "24:00")

    fun setData(value: List<ChartPoint>) {
        points = value.sortedBy(ChartPoint::x); invalidate()
    }

    fun setMaxY(value: Float) {
        maxY = max(value, 1f); invalidate()
    }

    fun setYItemCount(value: Int) {
        yItemCount = value.coerceAtLeast(2); invalidate()
    }

    fun setXRange(min: Float, max: Float) {
        minX = min; maxX = if (max > min) max else min + 1f; invalidate()
    }

    fun setXLabels(value: List<String>) {
        xLabels = value; requestLayout(); invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        calculateChartRect()
        drawGridLines(canvas)
        drawYAxisLabels(canvas)
        drawXAxisLabels(canvas)
        drawLine(canvas)
    }

    private fun calculateChartRect() {
        val widestY = max(labelPaint.measureText(formatY(maxY)), labelPaint.measureText("0"))
        val firstHalf = xLabels.firstOrNull()?.let { labelPaint.measureText(it) / 2f } ?: 0f
        val lastHalf = xLabels.lastOrNull()?.let { labelPaint.measureText(it) / 2f } ?: 0f
        chartRect.set(
            paddingLeft + widestY + dp(12f),
            paddingTop + labelPaint.textSize,
            width - paddingRight - lastHalf,
            height - paddingBottom - labelPaint.textSize - dp(10f)
        )
        chartRect.left = max(chartRect.left, paddingLeft + firstHalf)
    }

    private fun drawGridLines(canvas: Canvas) {
        val intervals = yItemCount - 1
        repeat(yItemCount) { index ->
            val y = chartRect.bottom - chartRect.height() * index / intervals
            canvas.drawLine(chartRect.left, y, chartRect.right, y, gridPaint)
        }
    }

    private fun drawYAxisLabels(canvas: Canvas) {
        val intervals = yItemCount - 1
        labelPaint.textAlign = Paint.Align.RIGHT
        repeat(yItemCount) { index ->
            val value = maxY * index / intervals
            val y = chartRect.bottom - chartRect.height() * index / intervals
            val baseline = y - (labelPaint.ascent() + labelPaint.descent()) / 2f
            canvas.drawText(formatY(value), chartRect.left - dp(9f), baseline, labelPaint)
        }
    }

    private fun drawXAxisLabels(canvas: Canvas) {
        if (xLabels.isEmpty()) return
        val intervals = (xLabels.size - 1).coerceAtLeast(1)
        val baseline = chartRect.bottom + dp(18f)
        xLabels.forEachIndexed { index, text ->
            val x = chartRect.left + chartRect.width() * index / intervals
            labelPaint.textAlign = when (index) {
                0 -> Paint.Align.LEFT
                xLabels.lastIndex -> Paint.Align.RIGHT
                else -> Paint.Align.CENTER
            }
            canvas.drawText(text, x, baseline, labelPaint)
        }
    }

    private fun drawLine(canvas: Canvas) {
        if (points.isEmpty()) return
        linePath.reset()
        points.forEachIndexed { index, point ->
            val x = mapXToCanvas(point.x)
            val y = mapYToCanvas(point.y)
            if (index == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
        }
        if (points.size == 1) canvas.drawCircle(
            mapXToCanvas(points[0].x),
            mapYToCanvas(points[0].y),
            dp(2f),
            linePaint
        )
        else canvas.drawPath(linePath, linePaint)
    }

    private fun mapXToCanvas(value: Float): Float =
        chartRect.left + ((value - minX) / (maxX - minX)).coerceIn(0f, 1f) * chartRect.width()

    private fun mapYToCanvas(value: Float): Float =
        chartRect.bottom - (value / maxY).coerceIn(0f, 1f) * chartRect.height()

    private fun formatY(value: Float): String = if (value == 0f) "0" else "${value.toInt()}W"
    private fun dp(value: Float) = value * resources.displayMetrics.density
    private fun sp(value: Float) = value * resources.displayMetrics.scaledDensity
}
