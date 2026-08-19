package com.jiyi.power.app.viewmodel

import androidx.lifecycle.ViewModel
import com.jiyi.power.R
import com.jiyi.power.app.bean.ScreenSettingUiData
import com.jiyi.power.app.bean.ScreenTextColor
import com.jiyi.power.app.bean.WallpaperItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ScreenSettingViewModel : ViewModel() {
    val wallpapers = listOf(
        WallpaperItem(1, R.mipmap.ic_power_banner_space, R.mipmap.ic_power_banner_space),
        WallpaperItem(2, R.mipmap.ic_power_banner_panther, R.mipmap.ic_power_banner_panther),
        WallpaperItem(3, R.mipmap.ic_power_banner_deer, R.mipmap.ic_power_banner_deer),
        WallpaperItem(4, R.mipmap.ic_power_banner_flower, R.mipmap.ic_power_banner_flower)
    )
    private val _uiState = MutableStateFlow(ScreenSettingUiData(wallpaper = wallpapers.first()))
    val uiState = _uiState.asStateFlow()

    fun setShowTime(value: Boolean) = update { copy(showTime = value) }
    fun setAchievement(value: Boolean) = update { copy(achievementInteraction = value) }
    fun setTextColor(value: ScreenTextColor) = update { copy(textColor = value) }
    fun setCustomText(value: String) = update { copy(customText = value) }
    fun setWallpaper(value: WallpaperItem) = update { copy(wallpaper = value) }
    fun setCustomWallpaper(uri: String) = update { copy(wallpaper = WallpaperItem(100, 0, 0, uri)) }
    private fun update(block: ScreenSettingUiData.() -> ScreenSettingUiData) { _uiState.update(block) }
}
