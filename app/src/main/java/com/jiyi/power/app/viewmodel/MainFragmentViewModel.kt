package com.jiyi.power.app.viewmodel

import com.aleyn.mvvm.base.BaseViewModel
import android.text.TextUtils
import com.jiyi.power.app.bean.Payload
import com.jiyi.power.app.bean.MobilePowerSnapshot
import com.jiyi.power.app.bean.MobilePowerHomeInfoBean
import com.jiyi.power.app.ble.BleConnectionCoordinator
import com.jiyi.power.app.ble.DeviceConnectionState
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

    private var receiveBuffer = ""

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
    val homeInfo = MutableStateFlow<MobilePowerHomeInfoBean?>(null)
    private var currentDeviceSn: String? = null


    override fun onBleDataReceive(data: String?) {
        super.onBleDataReceive(data)
        appendAndExtractFrames(data).forEach { frame ->
            parsingReadResult(frame)
            parsingWriteResult(frame)
        }
    }

    /** 设备首页连续读取响应较长，这里按协议长度处理 BLE 分包和粘包。 */
    private fun appendAndExtractFrames(data: String?): List<String> {
        val chunk = data?.filterNot(Char::isWhitespace)?.uppercase() ?: return emptyList()
        if (chunk.isEmpty() || chunk.length % 2 != 0 || chunk.any { it !in "0123456789ABCDEF" }) {
            return emptyList()
        }
        receiveBuffer = (receiveBuffer + chunk).takeLast(8192)
        val frames = mutableListOf<String>()
        while (receiveBuffer.isNotEmpty()) {
            var head = -1
            var index = 0
            while (index + 2 <= receiveBuffer.length) {
                if (receiveBuffer.regionMatches(index, "AA", 0, 2)) {
                    head = index
                    break
                }
                index += 2
            }
            if (head < 0) {
                receiveBuffer = ""
                break
            }
            if (head > 0) receiveBuffer = receiveBuffer.substring(head)
            if (receiveBuffer.length < 8) break

            val command = receiveBuffer.substring(2, 4).toInt(16)
            val lowLength = receiveBuffer.substring(6, 8).toInt(16)
            val dataLength = ((command shr 6) shl 8) + lowLength
            val frameHexLength = (CmdConstant.MIN_FRAME_LENGTH + dataLength) * 2
            if (frameHexLength > 4096 * 2) {
                receiveBuffer = receiveBuffer.drop(2)
                continue
            }
            if (receiveBuffer.length < frameHexLength) break
            frames += receiveBuffer.substring(0, frameHexLength)
            receiveBuffer = receiveBuffer.substring(frameHexLength)
        }
        return frames
    }

    private fun parsingReadResult(data: String?) {
        val parsedFrame = data?.let { MobilePowerProtocolManager.parseFrame(it) } ?: return
        if (!parsedFrame.raw.functionCode.equals(CmdConstant.FunctionCode.CODE_00, ignoreCase = true)) {
            return
        }
        val registerBlock = parsedFrame.payload as? Payload.RegisterBlock ?: return
        val snapshot = registerBlock.snapshot
        dashboardSnapshot.value = snapshot
        currentDeviceSn?.let { sn ->
            homeInfo.value = MobilePowerProtocolManager.toHomeInfoBean(sn, snapshot)
        }
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
        if (sn.isNullOrBlank()) return false
        val command = MobilePowerProtocolManager.buildHomeInfoReadCommand() ?: return false
        currentDeviceSn = sn
        sendCmdData(sn, command)
        return true
    }

    fun setLowCurrentMode(sn: String?, isOpen: Boolean): Boolean {
        val command = MobilePowerProtocolManager.buildWriteByteCommand(
            CmdConstant.FunctionCode.CODE_34,
            if (isOpen) 1 else 0,
        ) ?: return false
        if (sn.isNullOrBlank()) return false
        val connected = BleConnectionCoordinator.connectionStates.value.any { (deviceSn, state) ->
            deviceSn.equals(sn, ignoreCase = true) && state == DeviceConnectionState.CONNECTED
        }
        if (!connected) return false
        sendCmdData(sn, command)
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
