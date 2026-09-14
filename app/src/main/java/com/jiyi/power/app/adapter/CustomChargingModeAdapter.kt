package com.jiyi.power.app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.jiyi.power.R
import com.jiyi.power.app.bean.CustomChargingMode
import com.jiyi.power.databinding.ItemCustomChargingModeBinding

class CustomChargingModeAdapter(
    private val onSelect: (CustomChargingMode) -> Unit,
    private val onEdit: (CustomChargingMode) -> Unit,
) : RecyclerView.Adapter<CustomChargingModeAdapter.ViewHolder>() {
    private var modes = emptyList<CustomChargingMode>()
    private var selectedId = -1L
    fun submitList(items: List<CustomChargingMode>, selected: Long) {
        modes = items; selectedId = selected; notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemCustomChargingModeBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
    )

    override fun getItemCount() = modes.size
    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(modes[position])
    inner class ViewHolder(private val binding: ItemCustomChargingModeBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(mode: CustomChargingMode) = with(binding) {
            textModeName.text = mode.name
            textC1.text =
                root.context.getString(R.string.custom_mode_channel_output, "C1", mode.c1Power)
            textC2.text =
                root.context.getString(R.string.custom_mode_channel_output, "C2", mode.c2Power)
            textA.text =
                root.context.getString(R.string.custom_mode_channel_output, "A", mode.aPower)
            val selected = mode.id == selectedId
            checkMode.setBackgroundResource(
                if (selected) R.drawable.bg_custom_mode_selected_indicator
                else R.drawable.bg_custom_mode_unselected_indicator,
            )
            checkMode.setImageResource(if (selected) R.mipmap.ic_check_blue else 0)
            ImageViewCompat.setImageTintList(
                checkMode,
                if (selected) ContextCompat.getColorStateList(root.context, R.color.custom_mode_card_background) else null,
            )
            checkMode.setOnClickListener { onSelect(mode) }
            root.setOnClickListener { onEdit(mode) }
        }
    }
}
