package com.jiyi.power.ui.mobilepower.adapter

import android.util.Log
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.jiyi.power.R
import com.jiyi.power.ui.mobilepower.bean.TimeData

/**
 * @date 2023/6/1
 * @description
 */
class MobilePowerTimeChooseAdapter(data: MutableList<TimeData>?) : BaseQuickAdapter<TimeData, BaseViewHolder>(R.layout.item_mobile_power_time_choose, data) {
    private val TAG = "MobilePowerTimeChooseAdapter"
    private var mSelectPosition = -1
    override fun convert(holder: BaseViewHolder, item: TimeData) {
        Log.e(TAG, "mSelectPosition is $mSelectPosition")
        if (holder.adapterPosition == mSelectPosition) {
            //选中
            holder.getView<FrameLayout>(R.id.cl_root_view).setBackgroundResource(R.drawable.shape_gradient_r16_fcf150)
            holder.getView<TextView>(R.id.tv_time_left).setTextColor(context.resources.getColor(R.color.c_181a20))
            holder.getView<TextView>(R.id.tv_unit_left).setTextColor(context.resources.getColor(R.color.c_181a20))
            holder.getView<TextView>(R.id.tv_time_right).setTextColor(context.resources.getColor(R.color.c_181a20))
            holder.getView<TextView>(R.id.tv_unit_right).setTextColor(context.resources.getColor(R.color.c_181a20))
        } else {
            //未选中
            holder.getView<FrameLayout>(R.id.cl_root_view).setBackgroundResource(R.drawable.shape_r16_f2f4f8)
            holder.getView<TextView>(R.id.tv_time_left).setTextColor(context.resources.getColor(R.color.c_8c9098))
            holder.getView<TextView>(R.id.tv_unit_left).setTextColor(context.resources.getColor(R.color.c_8c9098))
            holder.getView<TextView>(R.id.tv_time_right).setTextColor(context.resources.getColor(R.color.c_8c9098))
            holder.getView<TextView>(R.id.tv_unit_right).setTextColor(context.resources.getColor(R.color.c_8c9098))
        }

        Log.e(TAG, "position is ${holder.adapterPosition}, time = ${item.time}")
        //时间显示
        if (item.time in 1..90) {
            holder.getView<LinearLayout>(R.id.layout_time).isVisible = true
            holder.getView<TextView>(R.id.tv_time_right).isVisible = false
            holder.getView<TextView>(R.id.tv_unit_right).isVisible = false
            holder.getView<ImageView>(R.id.iv_add_time).isVisible = false
            holder.getView<TextView>(R.id.tv_time_left).text = item.time.toString()
            holder.getView<TextView>(R.id.tv_unit_left).text = context.resources.getString(R.string.str_time_minutes)
        } else if (item.time > 90) {
            holder.getView<LinearLayout>(R.id.layout_time).isVisible = true
            holder.getView<ImageView>(R.id.iv_add_time).isVisible = false
            holder.getView<TextView>(R.id.tv_time_right).isVisible = isNeedShowMin(item.time)
            holder.getView<TextView>(R.id.tv_unit_right).isVisible = isNeedShowMin(item.time)
            holder.getView<TextView>(R.id.tv_time_left).text = getHour(item.time)
            holder.getView<TextView>(R.id.tv_unit_left).text = context.resources.getString(R.string.str_time_hours)
            holder.getView<TextView>(R.id.tv_time_right).text = getMin(item.time)
            holder.getView<TextView>(R.id.tv_unit_right).text = context.resources.getString(R.string.str_time_minutes)
        } else {
            //时间未赋值 默认显示+号
            holder.getView<ImageView>(R.id.iv_add_time).isVisible = true
            holder.getView<LinearLayout>(R.id.layout_time).isVisible = false
        }
    }

    /**
     * 设置选中的item
     */
    fun setChooseItem(position: Int, time: Int) {
        if (position in 0 until itemCount) {
            mSelectPosition = position
            if (time > 0) {
                getItem(position).time = time
            }
            notifyDataSetChanged()
        }
    }

    /**
     * 分钟转换小时
     * @param min 分钟数
     * @return 小时
     */
    private fun getHour(min: Int): String {
        val h = min / 60
        return "$h"
    }

    /**
     * 分钟转换小时后剩余分钟数
     * @param min 分钟数
     * @return 不够转小时的分钟数据
     */
    private fun getMin(min: Int): String {
        val m = min % 60
        return "$m"
    }

    /**
     * 判断时间转换小时后是否还需要显示分钟数
     * @param min 分钟数
     * @return true:需显示分钟 false:不显示
     */
    private fun isNeedShowMin(min: Int): Boolean {
        return min.let {
            it % 60 != 0
        }
    }
}