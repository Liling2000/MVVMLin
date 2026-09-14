package com.jiyi.power.app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jiyi.power.R
import com.jiyi.power.app.bean.PowerChannel
import com.jiyi.power.databinding.ItemPowerChannelBinding

class PowerChannelAdapter(private val onPowerChanged: (Int, Int) -> Unit) :
    ListAdapter<PowerChannel, PowerChannelAdapter.ViewHolder>(DiffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemPowerChannelBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position), position)

    inner class ViewHolder(private val binding: ItemPowerChannelBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(channel: PowerChannel, position: Int) = with(binding) {
            textChannelName.text = channel.name
            textChannelPower.text =
                root.context.getString(R.string.custom_mode_channel_power_value, channel.power)
            textMinPower.text = channel.minPower.toString()
            textMaxPower.text = channel.maxPower.toString()
            seekChannelPower.max = channel.maxPower - channel.minPower
            seekChannelPower.progress = channel.power - channel.minPower
            buttonDecrease.setOnClickListener { onPowerChanged(position, -1) }
            buttonIncrease.setOnClickListener { onPowerChanged(position, 1) }
            seekChannelPower.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?, progress: Int, fromUser: Boolean
                ) {
                    if (fromUser) onPowerChanged(
                        position, channel.minPower + progress - channel.power
                    )
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
            })
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<PowerChannel>() {
        override fun areItemsTheSame(old: PowerChannel, new: PowerChannel) = old.name == new.name
        override fun areContentsTheSame(old: PowerChannel, new: PowerChannel) = old == new
    }
}
