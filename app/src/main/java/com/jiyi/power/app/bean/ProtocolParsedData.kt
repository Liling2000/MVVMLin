package com.jiyi.power.app.bean

data class ProtocolParsedData(
    val commandCode: String,
    val commandType: String,
    val isBlock: Boolean,
    val isContinuous: Boolean,
    val dataLengthExtend: Int,
    val functionCode: String,
    val dataHex: String
)