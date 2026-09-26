package com.jiyi.power.app

import com.aleyn.mvvm.utils.MmkvManager
import com.jiyi.power.R
import com.jiyi.power.app.bean.WallpaperItem

/** Local-only wallpaper selection until device image transfer is implemented. */
object ScreenWallpaperRepository {
    private const val KEY_SELECTED_WALLPAPER_ID = "selected_screen_wallpaper_id"

    val wallpapers = listOf(
        WallpaperItem(1, R.mipmap.ic_power_banner_space, R.mipmap.ic_power_banner_space),
        WallpaperItem(2, R.mipmap.ic_power_banner_panther, R.mipmap.ic_power_banner_panther),
        WallpaperItem(3, R.mipmap.ic_power_banner_deer, R.mipmap.ic_power_banner_deer),
        WallpaperItem(4, R.mipmap.ic_power_banner_flower, R.mipmap.ic_power_banner_flower),
    )

    fun selected(): WallpaperItem {
        val selectedId = MmkvManager.getInt(KEY_SELECTED_WALLPAPER_ID, wallpapers.first().id)
        return wallpapers.firstOrNull { it.id == selectedId } ?: wallpapers.first()
    }

    fun select(item: WallpaperItem) {
        if (wallpapers.any { it.id == item.id }) {
            MmkvManager.putInt(KEY_SELECTED_WALLPAPER_ID, item.id)
        }
    }
}
