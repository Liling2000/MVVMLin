package com.jiyi.power.app.viewmodel

import com.jiyi.power.R
import com.jiyi.power.app.bean.ScreenSettingUiData
import com.jiyi.power.app.bean.ScreenTextColor
import com.jiyi.power.app.bean.WallpaperItem
import com.jiyi.power.app.utils.CmdConstant
import com.jiyi.power.app.utils.MobilePowerProtocolManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ScreenSettingViewModel : DeviceCommandViewModel() {
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
    fun submit(): Boolean {
        val state = _uiState.value
        // 0x3E：bit2~3 为文字颜色，bit1 为成就互动，bit0 为时间显示。
        val colorBits = when (state.textColor) { ScreenTextColor.WHITE -> 0; ScreenTextColor.DARK -> 1 }
        val lcdValue = (colorBits shl 2) or (if (state.achievementInteraction) 0x02 else 0) or (if (state.showTime) 0x01 else 0)
        val lcdSent = sendDeviceCommand(
            CmdConstant.FunctionCode.CODE_3E,
            MobilePowerProtocolManager.buildWriteByteCommand(CmdConstant.FunctionCode.CODE_3E, lcdValue),
        )
        // 0xC2 是固定 32 字节字符串；不足的字节由 copyOf 补 0x00。
        val text = state.customText.toByteArray(Charsets.UTF_8).copyOf(32)
        val textSent = sendDeviceCommand(
            CmdConstant.FunctionCode.CODE_C2,
            MobilePowerProtocolManager.buildEventCommand(CmdConstant.FunctionCode.CODE_C2, text),
        )
        return lcdSent && textSent
    }
    private fun update(block: ScreenSettingUiData.() -> ScreenSettingUiData) { _uiState.update(block) }
}
