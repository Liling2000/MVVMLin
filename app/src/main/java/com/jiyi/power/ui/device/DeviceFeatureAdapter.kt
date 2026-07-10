package com.jiyi.power.ui.device

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jiyi.power.databinding.ItemDeviceFeatureBinding

class DeviceFeatureAdapter(
    private val onFeatureClick: (DeviceFeature) -> Unit = {}
) : ListAdapter<DeviceFeature, DeviceFeatureAdapter.FeatureViewHolder>(FeatureDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeatureViewHolder {
        val binding = ItemDeviceFeatureBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FeatureViewHolder(binding, onFeatureClick)
    }

    override fun onBindViewHolder(holder: FeatureViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class FeatureViewHolder(
        private val binding: ItemDeviceFeatureBinding,
        private val onFeatureClick: (DeviceFeature) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DeviceFeature) = with(binding) {
            ivFeatureIcon.setImageResource(item.iconRes)
            tvFeatureTitle.text = item.title
            root.setOnClickListener { onFeatureClick(item) }
        }
    }

    private object FeatureDiffCallback : DiffUtil.ItemCallback<DeviceFeature>() {
        override fun areItemsTheSame(oldItem: DeviceFeature, newItem: DeviceFeature): Boolean {
            return oldItem.type == newItem.type
        }

        override fun areContentsTheSame(oldItem: DeviceFeature, newItem: DeviceFeature): Boolean {
            return oldItem == newItem
        }
    }
}