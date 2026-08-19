package com.jiyi.power.app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jiyi.power.app.bean.DeviceExceptionRecord
import com.jiyi.power.app.error.descriptionResource
import com.jiyi.power.app.error.titleResource
import com.jiyi.power.databinding.ItemDeviceExceptionBinding

class DeviceExceptionAdapter :
    ListAdapter<DeviceExceptionRecord, DeviceExceptionAdapter.ExceptionViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ExceptionViewHolder(
        ItemDeviceExceptionBinding.inflate(LayoutInflater.from(parent.context), parent, false),
    )

    override fun onBindViewHolder(holder: ExceptionViewHolder, position: Int) =
        holder.bind(getItem(position))

    class ExceptionViewHolder(private val binding: ItemDeviceExceptionBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: DeviceExceptionRecord) = with(binding) {
            errorIcon.setImageResource(item.iconRes)
            errorTitle.setText(titleResource(item.titleType))
            errorDescription.setText(descriptionResource(item.descriptionType))
            errorTime.text = item.time
            errorStatus.setText(item.statusRes)
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<DeviceExceptionRecord>() {
        override fun areItemsTheSame(oldItem: DeviceExceptionRecord, newItem: DeviceExceptionRecord) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: DeviceExceptionRecord, newItem: DeviceExceptionRecord) =
            oldItem == newItem
    }
}
