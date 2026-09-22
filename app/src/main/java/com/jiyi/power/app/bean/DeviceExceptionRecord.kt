package com.jiyi.power.app.bean

/** D0H 的一条 12 字节日志。索引是本次读取顺序，不推断时间先后。 */
data class DeviceExceptionRecord(
    val index: Int,
    val typeCode: Int,
    val batteryNumber: Int?,
    val parameterHex: String,
    val timestampHex: String,
)

sealed interface DeviceExceptionReply {
    data class Storage(val disabled: Boolean, val count: Int) : DeviceExceptionReply
    data class Page(val records: List<DeviceExceptionRecord>) : DeviceExceptionReply
    data object Invalid : DeviceExceptionReply
}