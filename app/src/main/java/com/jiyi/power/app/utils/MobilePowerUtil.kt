package com.jiyi.power.app.utils

import java.lang.StringBuilder

/**
 * @des 工具类
 * @user lilingke
 * @Date 2023/6/6
 */
object MobilePowerUtil {
    //端口的协议名称
    private val mTypeC = arrayListOf("", "PPS", "PD3.0", "QC2.0", "QC3.0", "AFC", "FCP", "SCP")

    private val mTypeC140 = arrayListOf("", "DCP", "", "QC", "", "AFC", "FCP", "SCP", "PD")

    /**
     * 获取端口协议名称
     */
    private fun getTypeCProtocolName(model: String?, index: Int): String {
        var protocol = ""
        if (model == "device name") {
            if (index >= 0 && index < mTypeC140.size) {
                protocol = mTypeC140[index]
            }
        } else {
            if (index >= 0 && index < mTypeC.size) {
                protocol = mTypeC[index]
            }
        }
        return protocol
    }

    /**
     * 获取TypeC1的协议名称
     */
    fun getTypeC1ProtocolName(model: String?, value: Int): String {
        var num = value and 0xFF
        return getTypeCProtocolName(model, num)
    }

    /**
     * 获取TypeC1的协议名称
     */
    fun getTypeC2ProtocolName(model: String?, value: Int): String {
        var num = (value shr 8) and 0xFF
        return getTypeCProtocolName(model, num)
    }

    /**
     * 获取设备异常错误码
     * 0 ：欠压保护
     * 1 ：过压保护
     * 2 ：短路
     * 3 ：过流保护
     * 4 ：过温保护
     * 5 ：重载提示
     */
    fun getDeviceErrorCode(code: Int): String {
        var errorCode = StringBuilder()
        if ((code and 0x01) > 0) {
            errorCode.append("0")
        }
        if ((code and 0x02) > 0) {
            if (errorCode.isEmpty()) errorCode.append("_")
            errorCode.append("1")
        }

        if ((code and 0x04) > 0) {
            if (errorCode.isEmpty()) errorCode.append("_")
            errorCode.append("2")
        }

        if ((code and 0x08) > 0) {
            if (errorCode.isEmpty()) errorCode.append("_")
            errorCode.append("3")
        }

        if ((code and 0x10) > 0) {
            if (errorCode.isEmpty()) errorCode.append("_")
            errorCode.append("4")
        }

        if ((code and 0x20) > 0) {
            if (errorCode.isEmpty()) errorCode.append("_")
            errorCode.append("5")
        }

        return errorCode.toString()
    }
}