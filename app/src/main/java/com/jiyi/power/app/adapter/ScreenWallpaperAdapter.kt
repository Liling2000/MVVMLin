package com.jiyi.power.app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.jiyi.power.app.bean.WallpaperItem
import com.jiyi.power.databinding.ItemScreenWallpaperBinding

/** 壁纸网格共用一个 ViewHolder，选中态在每次 bind 时完整恢复，避免复用残留。 */
class ScreenWallpaperAdapter(
    private val onClick: (WallpaperItem) -> Unit,
) : RecyclerView.Adapter<ScreenWallpaperAdapter.ViewHolder>() {
    private var items: List<WallpaperItem> = emptyList()
    private var selectedId: Int = -1

    fun submit(items: List<WallpaperItem>, selectedId: Int) {
        this.items = items
        this.selectedId = selectedId
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemScreenWallpaperBinding.inflate(LayoutInflater.from(parent.context), parent, false),
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(items[position])
    override fun getItemCount() = items.size

    inner class ViewHolder(private val binding: ItemScreenWallpaperBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: WallpaperItem) = with(binding) {
            imageWallpaper.setImageResource(item.previewRes)
            val selected = item.id == selectedId
            // 外层绿色底色配合图片的 2dp 内缩，形成设计稿中的完整选中描边。
            root.setBackgroundResource(if (selected) com.jiyi.power.R.drawable.bg_screen_wallpaper_selected else android.R.color.transparent)
            imageSelected.isVisible = selected
            root.setOnClickListener { onClick(item) }
        }
    }
}
