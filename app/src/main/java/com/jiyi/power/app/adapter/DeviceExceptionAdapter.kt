package com.jiyi.power.app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jiyi.power.app.bean.DeviceExceptionRecord
import com.jiyi.power.app.error.protectionIconResource
import com.jiyi.power.app.error.protectionLabel
import com.jiyi.power.databinding.ItemDeviceExceptionBinding

class DeviceExceptionAdapter(private val onItemClick: (DeviceExceptionRecord) -> Unit) :
    ListAdapter<DeviceExceptionRecord, DeviceExceptionAdapter.ExceptionViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ExceptionViewHolder(
        ItemDeviceExceptionBinding.inflate(LayoutInflater.from(parent.context), parent, false),
    )

    override fun onBindViewHolder(holder: ExceptionViewHolder, position: Int) =
        holder.bind(getItem(position))

    inner class ExceptionViewHolder(private val binding: ItemDeviceExceptionBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: DeviceExceptionRecord) = with(binding) {
            errorIcon.setImageResource(protectionIconResource(item.typeCode))
            errorTitle.text = protectionLabel(root.context, item)
            root.setOnClickListener { onItemClick(item) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<DeviceExceptionRecord>() {
        override fun areItemsTheSame(oldItem: DeviceExceptionRecord, newItem: DeviceExceptionRecord) =
            oldItem.index == newItem.index

        override fun areContentsTheSame(oldItem: DeviceExceptionRecord, newItem: DeviceExceptionRecord) =
            oldItem == newItem
    }
}
