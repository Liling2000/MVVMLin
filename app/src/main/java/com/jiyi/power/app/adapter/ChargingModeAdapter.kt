package com.jiyi.power.app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jiyi.power.R
import com.jiyi.power.app.common.ChargingPreferences
import com.jiyi.power.databinding.ItemChargingModeBinding
import com.jiyi.power.databinding.ItemChargingModeTipBinding

class ChargingModeAdapter(
    private val onSelect: (Int) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private data class Mode(
        val id: Int,
        val title: Int,
        val description: Int,
        val icon: Int,
        val firstTag: Int,
        val secondTag: Int,
    )

    private val modes = listOf(
        Mode(
            ChargingPreferences.MODE_SMART, R.string.charging_mode_smart,
            R.string.charging_mode_smart_desc, R.mipmap.ic_auto_mode,
            R.string.charging_mode_smart_tag_fast, R.string.charging_mode_smart_tag_control,
        ),
        Mode(
            ChargingPreferences.MODE_STANDARD, R.string.charging_mode_standard,
            R.string.charging_mode_standard_desc, R.mipmap.ic_power_protection,
            R.string.charging_mode_standard_tag_health, R.string.charging_mode_standard_tag_night,
        ),
        Mode(
            ChargingPreferences.MODE_CUSTOM, R.string.charging_mode_custom,
            R.string.charging_mode_custom_desc, R.mipmap.device_home_eq,
            R.string.charging_mode_custom_tag_parameter, R.string.charging_mode_custom_tag_strategy,
        ),
    )
    private var selectedMode: Int? = null
    private var interactionEnabled = true

    fun selectMode(mode: Int?) {
        if (selectedMode == mode) return
        val previous = modes.indexOfFirst { it.id == selectedMode }
        selectedMode = mode
        val current = modes.indexOfFirst { it.id == mode }
        if (previous >= 0) notifyItemChanged(previous)
        if (current >= 0) notifyItemChanged(current)
    }

    fun setInteractionEnabled(enabled: Boolean) {
        if (interactionEnabled == enabled) return
        interactionEnabled = enabled
        notifyItemRangeChanged(0, modes.size)
    }

    override fun getItemCount() = modes.size + 1

    override fun getItemViewType(position: Int) = if (position < modes.size) 0 else 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == 0) {
            ModeViewHolder(ItemChargingModeBinding.inflate(inflater, parent, false))
        } else {
            TipViewHolder(ItemChargingModeTipBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ModeViewHolder) holder.bind(modes[position])
    }

    private inner class ModeViewHolder(private val binding: ItemChargingModeBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(mode: Mode) = with(binding) {
            textTitle.setText(mode.title)
            textDescription.setText(mode.description)
            iconMode.setImageResource(mode.icon)
            iconMode.contentDescription = root.context.getString(mode.title)
            textTagFirst.setText(mode.firstTag)
            textTagSecond.setText(mode.secondTag)
            val selected = mode.id == selectedMode
            root.isSelected = selected
            root.elevation = if (mode.id == ChargingPreferences.MODE_SMART) {
                3 * root.resources.displayMetrics.density
            } else 0f
            indicatorMode.isSelected = selected
            indicatorMode.contentDescription = root.context.getString(mode.title)
            indicatorMode.setBackgroundResource(
                if (selected) R.drawable.bg_charge_selected else R.drawable.bg_charge_unselected,
            )
            indicatorMode.setImageResource(if (selected) R.mipmap.ic_check_blue else 0)
            root.isEnabled = interactionEnabled
            root.setOnClickListener { if (interactionEnabled) onSelect(mode.id) }
        }
    }

    private class TipViewHolder(binding: ItemChargingModeTipBinding) :
        RecyclerView.ViewHolder(binding.root)
}
