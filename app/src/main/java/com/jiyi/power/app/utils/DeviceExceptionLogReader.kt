package com.jiyi.power.app.utils

import com.jiyi.power.app.bean.DeviceExceptionRecord
import com.jiyi.power.app.bean.DeviceExceptionReply
import com.jiyi.power.app.bean.ParsedFrame

/** 一次 D1 + 多次 D0。超时后必须重新 begin，不能直接重发会推进偏移的 D0。 */
class DeviceExceptionLogReader {
    var expectedCode: String? = null
        private set
    var records: List<DeviceExceptionRecord> = emptyList()
        private set
    var totalCount: Int? = null
        private set
    var disabled: Boolean? = null
        private set
    var failed: Boolean = false
        private set
    val complete: Boolean get() = expectedCode == null && totalCount != null && records.size == totalCount && !failed

    fun begin() {
        records = emptyList()
        totalCount = null
        disabled = null
        failed = false
        expectedCode = CmdConstant.FunctionCode.CODE_D1
    }

    fun stop() { expectedCode = null }

    /** true 表示消费了当前等待的回包；无关回包不会推进分页或重置超时。 */
    fun accept(frame: ParsedFrame): Boolean {
        if (frame.raw.functionCode != expectedCode) return false
        val reply = MobilePowerProtocolManager.toExceptionReply(frame) ?: return false
        when (reply) {
            is DeviceExceptionReply.Storage -> {
                totalCount = reply.count
                disabled = reply.disabled
                expectedCode = if (reply.count == 0) null else CmdConstant.FunctionCode.CODE_D0
            }
            is DeviceExceptionReply.Page -> {
                val remaining = (totalCount ?: return false) - records.size
                if (reply.records.isEmpty() || reply.records.size > remaining) {
                    failed = true
                    expectedCode = null
                } else {
                    records = records + reply.records.mapIndexed { index, record ->
                        record.copy(index = records.size + index)
                    }
                    expectedCode = if (records.size == totalCount) null else CmdConstant.FunctionCode.CODE_D0
                }
            }
            DeviceExceptionReply.Invalid -> {
                failed = true
                expectedCode = null
            }
        }
        return true
    }
}
