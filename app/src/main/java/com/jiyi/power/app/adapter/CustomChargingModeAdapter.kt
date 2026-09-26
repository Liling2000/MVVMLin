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
        if (modes == items && selectedId == selected) return
        val oldModes = modes
        val oldSelectedId = selectedId
        modes = items
        selectedId = selected
        if (oldModes == items) {
            listOf(oldSelectedId, selectedId)
                .distinct()
                .map { id -> modes.indexOfFirst { it.id == id } }
                .filter { it >= 0 }
                .forEach { notifyItemChanged(it, PAYLOAD_SELECTION) }
        } else {
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemCustomChargingModeBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
    )

    override fun getItemCount() = modes.size
    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(modes[position])
    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(PAYLOAD_SELECTION)) {
            holder.bindSelection(modes[position])
        } else {
            holder.bind(modes[position])
        }
    }

    inner class ViewHolder(private val binding: ItemCustomChargingModeBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(mode: CustomChargingMode) = with(binding) {
            textModeName.text = mode.name
            textC1Power.text = root.context.getString(R.string.custom_mode_output_power, mode.c1Power)
            textC2Power.text = root.context.getString(R.string.custom_mode_output_power, mode.c2Power)
            textAPower.text = root.context.getString(R.string.custom_mode_output_power, mode.aPower)

            bindSelection(mode)
            checkMode.setOnClickListener { onSelect(mode) }
            root.setOnClickListener { onEdit(mode) }
        }

        fun bindSelection(mode: CustomChargingMode) {
            binding.checkMode.setImageResource(
                if (mode.id == selectedId) R.mipmap.ic_check_pre
                else R.drawable.bg_charge_unselected,
            )
        }
    }

    private companion object {
        const val PAYLOAD_SELECTION = "selection"
    }
}
