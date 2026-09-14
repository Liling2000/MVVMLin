package com.jiyi.power.app.viewmodel

import com.jiyi.power.app.utils.CmdConstant
import com.jiyi.power.app.utils.MobilePowerProtocolManager

class ChargingModeViewModel : DeviceCommandViewModel() {
    /** V2.0 has independent C1/C2 mode registers; apply the selected mode to both ports. */
    fun applyMode(mode: Int): Boolean {
        // UI 是设备级模式，而协议拆分为 C1/C2 两个寄存器，因此需成对写入。
        val c1 = sendDeviceCommand(
            CmdConstant.FunctionCode.CODE_3B,
            MobilePowerProtocolManager.buildWriteByteCommand(CmdConstant.FunctionCode.CODE_3B, mode)
        )
        val c2 = sendDeviceCommand(
            CmdConstant.FunctionCode.CODE_3C,
            MobilePowerProtocolManager.buildWriteByteCommand(CmdConstant.FunctionCode.CODE_3C, mode)
        )
        return c1 && c2
    }
}
