package com.jiyi.power.app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jiyi.power.R
import com.jiyi.power.app.bean.BatteryCellInfo
import com.jiyi.power.databinding.ItemBatteryCellBinding

class BatteryCellAdapter :
    ListAdapter<BatteryCellInfo, BatteryCellAdapter.CellViewHolder>(DiffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = CellViewHolder(
        ItemBatteryCellBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: CellViewHolder, position: Int) =
        holder.bind(getItem(position), position == itemCount - 1)

    class CellViewHolder(private val binding: ItemBatteryCellBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: BatteryCellInfo, isLast: Boolean) = with(binding) {
            textCellName.text =
                root.context.getString(R.string.battery_cell_voltage_name, item.index)
            textCellVoltage.text =
                item.voltageMv?.let { root.context.getString(R.string.battery_voltage_mv, it) }
                    ?: root.context.getString(R.string.battery_empty_value)
            divider.visibility = if (isLast) android.view.View.GONE else android.view.View.VISIBLE
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<BatteryCellInfo>() {
        override fun areItemsTheSame(oldItem: BatteryCellInfo, newItem: BatteryCellInfo) =
            oldItem.index == newItem.index

        override fun areContentsTheSame(oldItem: BatteryCellInfo, newItem: BatteryCellInfo) =
            oldItem == newItem
    }
}
