package com.jiyi.power.app.adapter

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.jiyi.power.R
import com.jiyi.power.app.language.AppLanguages
import com.jiyi.power.app.language.LanguageOption
import com.jiyi.power.databinding.ItemLanguageBinding

class LanguageAdapter(
    private var selectedTag: String,
    private val onSelect: (LanguageOption) -> Unit,
) : RecyclerView.Adapter<LanguageAdapter.Holder>() {
    override fun getItemCount() = AppLanguages.items.size
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        ItemLanguageBinding.inflate(LayoutInflater.from(parent.context), parent, false),
    )
    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(AppLanguages.items[position], position)

    inner class Holder(private val binding: ItemLanguageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: LanguageOption, position: Int) = with(binding) {
            val selected = item.tag == selectedTag
            textLanguage.setText(item.label)
            textLanguage.setTextColor(ContextCompat.getColor(root.context,
                if (selected) R.color.color_191c1e else R.color.color_454558))
            textLanguage.setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)
            selectedDot.visibility = if (selected) View.VISIBLE else View.INVISIBLE
            selectionIndicator.isSelected = selected
            selectionIndicator.setImageResource(if (selected) R.mipmap.ic_check_light else 0)
            divider.visibility = if (position == itemCount - 1) View.GONE else View.VISIBLE
            root.isSelected = selected
            root.contentDescription = root.context.getString(
                if (selected) R.string.language_selected else R.string.language_not_selected,
                root.context.getString(item.label),
            )
            root.setOnClickListener {
                if (item.tag != selectedTag) {
                    val previous = AppLanguages.items.indexOfFirst { it.tag == selectedTag }
                    selectedTag = item.tag
                    if (previous >= 0) notifyItemChanged(previous)
                    notifyItemChanged(position)
                    onSelect(item)
                }
            }
        }
    }
}
