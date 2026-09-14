package com.jiyi.power.app.viewmodel

import com.jiyi.power.app.utils.CmdConstant
import com.jiyi.power.app.utils.MobilePowerProtocolManager

class DeviceSettingViewModel : DeviceCommandViewModel() {
    fun restoreFactorySettings(): Boolean = sendDeviceCommand(
        CmdConstant.FunctionCode.CODE_3D,
        MobilePowerProtocolManager.buildWriteByteCommand(CmdConstant.FunctionCode.CODE_3D, 0xFF),
    )
}
