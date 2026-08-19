package com.jiyi.power.app.viewmodel

import com.aleyn.mvvm.base.BaseViewModel
import android.text.TextUtils
import com.jiyi.power.app.bean.Payload
import com.jiyi.power.app.bean.MobilePowerSnapshot
import com.jiyi.power.app.utils.MobilePowerProtocolManager
import com.jiyi.power.app.utils.CmdConstant
import kotlinx.coroutines.flow.MutableStateFlow
import java.lang.StringBuilder

data class C1PortMetricsUiState(
    val currentMa: Int? = null,
    val voltageMv: Int? = null,
    val powerW: Int? = null
)

class MainFragmentViewModel : BaseViewModel() {

    // 设备温度
    val mMobileTemperature = MutableStateFlow(0)

    // 设备电量
    val mMobileElectricitye = MutableStateFlow(0)

    // 自充剩余时间
    val mMobileRemaindere = MutableStateFlow(0)

    // 充电剩余时间
    val mMobileChargeTime = MutableStateFlow(0)

    // 端口状态
    val mPortStatus = MutableStateFlow(0)

    // 设备异常
    val mDeviceError = MutableStateFlow(0)

    // 小电流模式
    val mLowCurrentMode = MutableStateFlow(0)

    // 提示倒计时
    val mReminderCountdown = MutableStateFlow(0)

    // 关机倒计时
    val mOffCountdown = MutableStateFlow(0)

    // 当前主题
    val mMobileTheme = MutableStateFlow(0)

    // 屏保提示语
    val mMobileScreensaver = MutableStateFlow("")

    // 写入结果
    val mWriteSuccessful = MutableStateFlow("")
    val mBluetoothModuleVersion = MutableStateFlow("")
    val mDCModuleVersion = MutableStateFlow("")
    val mBmsModuleVersion = MutableStateFlow("")
    val c1PortMetrics = MutableStateFlow(C1PortMetricsUiState())
    val dashboardSnapshot = MutableStateFlow<MobilePowerSnapshot?>(null)


    override fun onBleDataReceive(data: String?) {
        super.onBleDataReceive(data)
        parsingReadResult(data)
        parsingWriteResult(data)
    }

    private fun parsingReadResult(data: String?) {
        val parsedFrame = data?.let { MobilePowerProtocolManager.parseFrame(it) } ?: return
        val registerBlock = parsedFrame.payload as? Payload.RegisterBlock ?: return
        val snapshot = registerBlock.snapshot
        dashboardSnapshot.value = snapshot
        val c1 = snapshot.c1

        c1PortMetrics.value = C1PortMetricsUiState(
            currentMa = c1.currentMa,
            voltageMv = c1.voltageMv,
            powerW = c1.powerW
        )
    }

    private fun setBluetoothModuleVersion(version: String) {
        val strV = getModuleVersion(version)

    }

    private fun setDCModuleVersion(version: String) {
        val strV = getModuleVersion(version)

    }

    private fun setBmsModuleVersion(version: String) {
        val strV = getModuleVersion(version)
        if (!TextUtils.isEmpty(strV)) {
            mBmsModuleVersion.value = strV
        }
    }

    private fun getModuleVersion(version: String): String {
        return ""
    }

    private fun parsingWriteResult(data: String?) {
        data?.let {

        }
    }

    private fun readDeviceStatus(code: String): Boolean {
        return false
    }

    private fun readDeviceStatus(code: String, priority: Int): Boolean {

        return false
    }

    fun writeDeviceStatus(code: String, value: String): Boolean {
        return false
    }

    fun writeDeviceStatus(code: String, value: String, priority: Int): Boolean {

        return false
    }

    fun redCountdownReminder(): Boolean {
        return false
    }

    fun redCountdownOff(): Boolean {
        return false
    }

    fun redMobileTheme(): Boolean {
        return false
    }

    fun redMobileScreensaver(): Boolean {
        return false
    }

    fun setLowCurrentMode(isOpen: Boolean): Boolean {
        return false
    }

    fun requestDashboard(sn: String?): Boolean {
        val command = MobilePowerProtocolManager.buildReadCommand(
            CmdConstant.FunctionCode.CODE_00,
            0x3B,
        ) ?: return false
        if (sn.isNullOrBlank()) return false
        sendCmdData(sn, command)
        return true
    }

    fun setLowCurrentMode(sn: String?, isOpen: Boolean): Boolean {
        val command = MobilePowerProtocolManager.buildWriteByteCommand(
            CmdConstant.FunctionCode.CODE_34,
            if (isOpen) 1 else 0,
        ) ?: return false
        if (sn.isNullOrBlank()) return false
        sendCmdData(sn, command)
        dashboardSnapshot.value = dashboardSnapshot.value?.let { snapshot ->
            snapshot.copy(settings = snapshot.settings.copy(lowCurrentMode = isOpen))
        }
        return true
    }

    fun setMobileTheme(value: String): Boolean {
        return false
    }

    fun setScreensaverText(value: String): Boolean {
        return false
    }

    fun setCountdownReminder(value: String): Boolean {
        return false
    }

    fun setCountdownOff(value: String): Boolean {
        return false
    }

    fun closeCountdownOff(value: String): Boolean {
        return false
    }

    fun setRestore(value: String): Boolean {
        return false
    }

    private fun getPortStatusValue(
        value: Int, rightShiftCount: Int, maxValue: Int
    ): Int {
        return (value shr rightShiftCount) and maxValue
    }

    fun getDeviceStatue(): Int {
        val portStatus = mPortStatus.value

        if (portStatus <= 0) {
            return 0
        }

        val typeC1Statue = portStatus and 3
        val typeC2Statue = (portStatus shr 2) and 3

        if (typeC1Statue == 2 || typeC2Statue == 2) {
            return 2
        }

        if (typeC1Statue == 1 || typeC2Statue == 1) {
            return 1
        }

        return 0
    }

    fun getDeviceErrorCode(error: Int): String {
        val errorCode = StringBuilder()

        if ((error and 0x0101) > 0) {
            errorCode.append("0")
        }

        if ((error and 0x0404) > 0) {
            if (errorCode.isNotEmpty()) errorCode.append("_")
            errorCode.append("2")
        }

        if ((error and 0x0808) > 0) {
            if (errorCode.isNotEmpty()) errorCode.append("_")
            errorCode.append("3")
        }

        if ((error and 0x10) > 0 || (error and 0x1000) > 0) {
            if (errorCode.isNotEmpty()) errorCode.append("_")
            errorCode.append("4")
        }

        return errorCode.toString()
    }

    fun moduleVersionAvailable(): Boolean {
        return mBluetoothModuleVersion.value.isNotEmpty() && mDCModuleVersion.value.isNotEmpty()
    }
}
