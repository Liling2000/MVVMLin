package com.jiyi.power.app.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jiyi.power.R
import com.jiyi.power.app.bean.TimeUnit
import com.jiyi.power.app.bean.TimerOption
import com.jiyi.power.app.bean.formatTime
import com.jiyi.power.databinding.ItemTimerOptionBinding

class TimerOptionAdapter(
    private val onClick: (TimerOption) -> Unit,
) : ListAdapter<TimerOption, TimerOptionAdapter.OptionViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = OptionViewHolder(
        ItemTimerOptionBinding.inflate(LayoutInflater.from(parent.context), parent, false),
    )

    override fun onBindViewHolder(holder: OptionViewHolder, position: Int) =
        holder.bind(getItem(position))

    inner class OptionViewHolder(private val binding: ItemTimerOptionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(option: TimerOption) = with(binding) {
            val context = root.context
            val hasCustomValue = option.isCustom && option.time > 0
            customContent.visibility = if (option.isCustom && !hasCustomValue) View.VISIBLE else View.GONE
            timeContent.visibility = if (!option.isCustom || hasCustomValue) View.VISIBLE else View.GONE
            root.setBackgroundResource(
                when {
                    option.selected -> R.drawable.bg_timer_option_selected
                    option.isCustom && !hasCustomValue -> R.drawable.bg_timer_option_custom
                    else -> R.drawable.bg_timer_option_normal
                },
            )
            selectedIcon.visibility = if (option.selected) View.VISIBLE else View.GONE
            if (!option.isCustom || hasCustomValue) {
                val text = formatTime(option.time)
                timeNumber.text = text.number
                timeUnit.setText(if (text.unit == TimeUnit.HOUR) R.string.timer_hour else R.string.timer_minute)
                timeNumber.setTextColor(ContextCompat.getColor(context, if (option.selected) R.color.color_0752ae else R.color.color_45475a))
            }
            root.setOnClickListener { onClick(option) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<TimerOption>() {
        override fun areItemsTheSame(oldItem: TimerOption, newItem: TimerOption) =
            oldItem.isCustom == newItem.isCustom && (oldItem.isCustom || oldItem.time == newItem.time)

        override fun areContentsTheSame(oldItem: TimerOption, newItem: TimerOption) = oldItem == newItem
    }
}
