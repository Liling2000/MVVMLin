package com.jiyi.power.app.bean

enum class ScreenTextColor { WHITE, DARK }

data class WallpaperItem(
    val id: Int,
    val previewRes: Int,
    val wallpaperRes: Int,
    val customUri: String? = null
)

data class ScreenSettingUiData(
    val showTime: Boolean = true,
    val achievementInteraction: Boolean = true,
    val textColor: ScreenTextColor = ScreenTextColor.WHITE,
    val customText: String = "",
    val wallpaper: WallpaperItem
)
