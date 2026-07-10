package com.jiyi.power.ui.mobilepower.adapter

import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.jiyi.power.R
import com.jiyi.power.ui.mobilepower.bean.StatisticsData

class MobileStatisticsAdapter(data: MutableList<StatisticsData?>?) :
    BaseQuickAdapter<StatisticsData?, BaseViewHolder>(R.layout.item_mobile_power_statistics, data) {
    private var isModify = false
    override fun convert(holder: BaseViewHolder, item: StatisticsData?) {
        item?.let {
            holder.getView<TextView>(R.id.tv_cell).text = item.cell.toString()
            holder.getView<TextView>(R.id.tv_voltage).text = item.voltage.toString()
            holder.getView<TextView>(R.id.tv_current).text = item.current.toString()
            holder.getView<ImageView>(R.id.img_repair).isVisible = item.repairNum >= 0
        }
    }
}