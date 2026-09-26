package com.jiyi.power.app.viewmodel

import com.jiyi.power.app.ScreenWallpaperRepository
import com.jiyi.power.app.bean.ScreenSettingUiData
import com.jiyi.power.app.bean.ScreenTextColor
import com.jiyi.power.app.bean.WallpaperItem
import com.jiyi.power.app.utils.CmdConstant
import com.jiyi.power.app.utils.MobilePowerProtocolManager
import com.jiyi.power.app.utils.ScreenTextProtocol
import com.jiyi.power.app.utils.TimerReminderProtocol
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ScreenSettingViewModel : DeviceCommandViewModel() {
    private val reminderProtocol = TimerReminderProtocol()
    private val textProtocol = ScreenTextProtocol()
    val wallpapers = ScreenWallpaperRepository.wallpapers
    private val _uiState = MutableStateFlow(
        ScreenSettingUiData(wallpaper = ScreenWallpaperRepository.selected())
    )
    val uiState = _uiState.asStateFlow()

    fun setShowTime(value: Boolean) = update { copy(showTime = value) }
    fun setAchievement(value: Boolean) = update { copy(achievementInteraction = value) }
    fun setTextColor(value: ScreenTextColor) = update { copy(textColor = value) }
    fun setCustomText(value: String) = update { copy(customText = value) }
    fun setWallpaper(value: WallpaperItem) = update { copy(wallpaper = value) }
    fun setCustomWallpaper(uri: String) = update { copy(wallpaper = WallpaperItem(100, 0, 0, uri)) }
    fun refreshReminder() {
        reminderProtocol.reset()
        sendDeviceCommand(
            CmdConstant.FunctionCode.CODE_C1,
            reminderProtocol.readCommand(),
        )
    }

    fun refreshCustomText() {
        textProtocol.reset()
        sendDeviceCommand(
            CmdConstant.FunctionCode.CODE_C2,
            textProtocol.readCommand(),
        )
    }

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

    override fun onBleDataReceive(data: String?) {
        super.onBleDataReceive(data)
        reminderProtocol.accept(data).forEach { minutes ->
            update { copy(reminderMinutes = minutes) }
        }
        textProtocol.accept(data).forEach { text ->
            update { copy(customText = text) }
        }
    }

    override fun onDeviceReconnected() {
        refreshReminder()
        refreshCustomText()
    }

    override fun onDeviceDisconnected() {
        reminderProtocol.reset()
        textProtocol.reset()
        update { copy(reminderMinutes = null) }
    }

    private fun update(block: ScreenSettingUiData.() -> ScreenSettingUiData) { _uiState.update(block) }
}
