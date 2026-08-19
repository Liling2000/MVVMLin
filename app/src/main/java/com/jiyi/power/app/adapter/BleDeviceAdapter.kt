package com.jiyi.power.app.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jiyi.power.R
import com.jiyi.power.app.viewmodel.BleScanDevice
import com.jiyi.power.databinding.ItemBleScanDeviceBinding

class BleDeviceAdapter(private val onItemClick: (BleScanDevice) -> Unit) :
    ListAdapter<BleScanDevice, BleDeviceAdapter.DeviceViewHolder>(DeviceDiffCallback) {

    private var connectingSn: String? = null

    fun setConnectingDevice(sn: String?) {
        val previous = connectingSn
        connectingSn = sn
        currentList.indexOfFirst { it.sn == previous }.takeIf { it >= 0 }?.let(::notifyItemChanged)
        currentList.indexOfFirst { it.sn == sn }.takeIf { it >= 0 }?.let(::notifyItemChanged)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        return DeviceViewHolder(
            ItemBleScanDeviceBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DeviceViewHolder(private val binding: ItemBleScanDeviceBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: BleScanDevice) = with(binding) {
            ivDevice.setImageResource(R.mipmap.ic_s_device)
            tvDeviceName.text = item.name
            val connecting = item.sn == connectingSn
            progressConnecting.visibility = if (connecting) View.VISIBLE else View.GONE
            imageArrow.visibility = if (connecting) View.GONE else View.VISIBLE
            root.isEnabled = !connecting
            root.setOnClickListener { onItemClick(item) }
        }
    }

    private object DeviceDiffCallback : DiffUtil.ItemCallback<BleScanDevice>() {
        override fun areItemsTheSame(oldItem: BleScanDevice, newItem: BleScanDevice) =
            oldItem.sn == newItem.sn

        override fun areContentsTheSame(oldItem: BleScanDevice, newItem: BleScanDevice) =
            oldItem == newItem
    }
}
