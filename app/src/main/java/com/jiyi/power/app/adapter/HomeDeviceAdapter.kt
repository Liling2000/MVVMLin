package com.jiyi.power.app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jiyi.power.databinding.ItemHomeAddDeviceBinding
import com.jiyi.power.databinding.ItemHomeDeviceBinding

sealed interface HomeDeviceItem {
    data class Device(
        val name: String,
        val description: String,
        val power: String,
        val status: String,
        @DrawableRes val imageRes: Int
    ) : HomeDeviceItem

    data object AddDevice : HomeDeviceItem
}

class HomeDeviceAdapter(private val onItemClick: (HomeDeviceItem.Device?) -> Unit) :
    ListAdapter<HomeDeviceItem, RecyclerView.ViewHolder>(DeviceDiffCallback) {
    override fun getItemViewType(position: Int) =
        if (getItem(position) is HomeDeviceItem.Device) TYPE_DEVICE else TYPE_ADD

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_DEVICE) DeviceViewHolder(
            ItemHomeDeviceBinding.inflate(
                inflater, parent, false
            )
        )
        else AddDeviceViewHolder(ItemHomeAddDeviceBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is DeviceViewHolder -> holder.bind(getItem(position) as HomeDeviceItem.Device)
            is AddDeviceViewHolder -> holder.binding.root.setOnClickListener { onItemClick(null) }
        }
    }

    inner class DeviceViewHolder(private val binding: ItemHomeDeviceBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: HomeDeviceItem.Device) = with(binding) {
            imageDevice.setImageResource(item.imageRes)
            textDeviceName.text = item.name
            textDeviceDesc.text = item.description
            textDevicePower.text = item.power
            textDeviceStatus.text = item.status
            root.setOnClickListener { onItemClick(item) }
        }
    }

    class AddDeviceViewHolder(val binding: ItemHomeAddDeviceBinding) :
        RecyclerView.ViewHolder(binding.root)

    private companion object {
        const val TYPE_DEVICE = 0
        const val TYPE_ADD = 1
        val DeviceDiffCallback = object : DiffUtil.ItemCallback<HomeDeviceItem>() {
            override fun areItemsTheSame(oldItem: HomeDeviceItem, newItem: HomeDeviceItem) =
                oldItem is HomeDeviceItem.AddDevice && newItem is HomeDeviceItem.AddDevice || oldItem is HomeDeviceItem.Device && newItem is HomeDeviceItem.Device && oldItem.name == newItem.name

            override fun areContentsTheSame(oldItem: HomeDeviceItem, newItem: HomeDeviceItem) =
                oldItem == newItem
        }
    }
}
