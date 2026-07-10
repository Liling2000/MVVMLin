package com.jiyi.power.ui.mobilepower.bean

/**
 * 功能码支持的操作类型。
 *
 * @property text 面向 UI 或调试输出展示的中文说明。
 */
enum class OperationType(val text: String) {
    READ_ONLY("只读"),
    WRITE_ONLY("只写"),
    READ_WRITE("读/写"),
    WRITE("写"),
    READ("读"),
    EVENT("事件"),
    RESPONSE("响应"),
    RESERVED("预留")
}

/**
 * 功能码定义。
 *
 * @property code 功能码，固定为两位大写十六进制字符串，例如 "00"、"6B"。
 * @property name 功能码名称，用于列表展示或调试日志。
 * @property operationType 功能码读写/事件属性。
 * @property description 协议文档中的字段含义、单位或 bit 位说明。
 * @property isBlock 是否为块数据命令，常见于图片、表情、固件数据传输。
 */
data class FunctionSpec(
    val code: String,
    val name: String,
    val operationType: OperationType,
    val description: String,
    val isBlock: Boolean = false
)

/**
 * 完整帧解析结果。
 *
 * @property raw [ProtocolUtil] 解析出的通用帧字段，包含命令码、功能码、数据区等原始信息。
 * @property functionSpec 当前功能码对应的协议定义；如果功能码未登记则为 null。
 * @property payload 按功能码进一步解析后的业务数据。
 * @property registers 连续寄存器读取结果。key 为寄存器功能码，value 为该寄存器的单字节值。
 */
data class ParsedFrame(
    val raw: ProtocolParsedData,
    val functionSpec: FunctionSpec?,
    val payload: Payload,
    val registers: Map<String, RegisterValue>
)

/**
 * 单个寄存器值。
 *
 * 连续读取时，数据区每个字节会按“起始功能码 + 数据下标”映射成一个寄存器值。
 *
 * @property code 寄存器功能码，固定为两位大写十六进制字符串。
 * @property name 寄存器名称；未知功能码显示为“未知功能码”。
 * @property value 寄存器原始单字节无符号值，范围 0..255。
 * @property valueHex [value] 的两位十六进制形式。
 * @property description 该寄存器在协议文档中的说明。
 */
data class RegisterValue(
    val code: String,
    val name: String,
    val value: Int,
    val valueHex: String,
    val description: String
)

/**
 * 业务 payload 的结构化结果。
 *
 * 寄存器区会解析为 [RegisterBlock]，块传输类命令会保留原始数据或拆出 index/status，
 * 字符串类命令会去掉尾部的 0x00 和空白字符。
 */
sealed class Payload {
    /** 无数据区或空 payload。 */
    object Empty : Payload()

    /**
     * 寄存器区快照。
     *
     * @property snapshot 由 0x00..0x6B 寄存器组合出的设备状态快照，未读取到的字段为 null。
     */
    data class RegisterBlock(val snapshot: MobilePowerSnapshot) : Payload()

    /**
     * 从机事件 ACK。
     *
     * @property success true 表示状态字节为 0x00，即 OK。
     * @property status 原始状态字节，0x00 表示 OK，0x01 表示 ERR，其它值保留。
     */
    data class Ack(val success: Boolean, val status: Int) : Payload()

    /**
     * 块数据包。
     *
     * @property bytes 数据区原始字节，例如图片数据、表情数据或固件升级数据。
     * @property dataHex [bytes] 的大写十六进制字符串。
     */
    data class DataPacket(val bytes: ByteArray, val dataHex: String) : Payload()

    /**
     * 图片或表情上传开始。
     *
     * @property index 图片/表情索引。
     * @property blockCount 本次传输包含的数据块总数，2 字节 little-endian。
     */
    data class ImageUploadStart(val index: Int, val blockCount: Int) : Payload()

    /**
     * 图片或表情传输结束。
     *
     * @property index 图片/表情索引。
     * @property status 原始传输状态字节。
     * @property finished true 表示 [status] 为 0x01，即传输完成。
     */
    data class TransferEnd(val index: Int, val status: Int, val finished: Boolean) : Payload()

    /**
     * 只有索引参数的命令。
     *
     * @property index 图片、表情、歌词或微信消息等业务对象索引。
     */
    data class IndexCommand(val index: Int) : Payload()

    /**
     * 时间同步数据。
     *
     * @property year 完整年份，协议中的年份字节会加 2000。
     * @property month 月，通常为 1..12。
     * @property day 日，通常为 1..31。
     * @property hour 时，24 小时制，通常为 0..23。
     * @property minute 分，通常为 0..59。
     * @property second 秒，通常为 0..59。
     * @property week 星期值，按设备协议定义传输，未在这里重新映射。
     */
    data class TimeSync(
        val year: Int,
        val month: Int,
        val day: Int,
        val hour: Int,
        val minute: Int,
        val second: Int,
        val week: Int
    ) : Payload()

    /**
     * 天气同步数据。
     *
     * @property weatherCode 天气编码，具体含义由设备协议或天气服务映射表决定。
     * @property currentTempC 当前温度，单位摄氏度。
     * @property maxTempC 当日最高温度，单位摄氏度。
     * @property minTempC 当日最低温度，单位摄氏度。
     * @property city 城市名称，协议最多读取 16 字节并去掉末尾填充字符。
     */
    data class WeatherSync(
        val weatherCode: Int,
        val currentTempC: Int,
        val maxTempC: Int,
        val minTempC: Int,
        val city: String
    ) : Payload()

    /**
     * 歌词传输开始信息。
     *
     * @property count 歌词行数。
     * @property author 歌手/作者名称，协议最多 64 字节。
     * @property songName 歌曲名称，协议最多 64 字节。
     */
    data class LyricsStart(val count: Int, val author: String, val songName: String) : Payload()

    /**
     * 单行歌词。
     *
     * @property index 歌词行索引。
     * @property content 歌词内容。
     */
    data class LyricLine(val index: Int, val content: String) : Payload()

    /**
     * 普通文本 payload。
     *
     * @property value UTF-8 字符串，已清理协议填充的 0x00 和空白字符。
     */
    data class Text(val value: String) : Payload()

    /**
     * 16 位无符号整型 payload。
     *
     * @property value 2 字节 little-endian 数值；长度不足时会退化为首字节值。
     */
    data class UInt16Value(val value: Int) : Payload()

    /**
     * 固件升级命令。
     *
     * @property command 低 3 位升级命令：0 空闲，1 升级开始，2 文件信息，3 停止或中断。
     */
    data class FirmwareUpgradeCommand(val command: Int) : Payload()

    /**
     * 固件升级状态。
     *
     * @property finished bit7，升级完成。
     * @property transferring bit6，正在传输。
     * @property error bit5，升级错误。
     * @property ready bit4，设备已准备好。
     * @property appMode bit3，设备处于 APP 模式。
     * @property currentState bit0-bit2，当前升级阶段原始值。
     */
    data class FirmwareStatus(
        val finished: Boolean,
        val transferring: Boolean,
        val error: Boolean,
        val ready: Boolean,
        val appMode: Boolean,
        val currentState: Int
    ) : Payload()

    /**
     * 未识别或长度不满足预期的 payload。
     *
     * @property bytes 数据区原始字节。
     * @property dataHex [bytes] 的大写十六进制字符串。
     */
    data class Unknown(val bytes: ByteArray, val dataHex: String) : Payload()
}

/**
 * 设备寄存器快照。
 *
 * @property c1 Type-C1 端口实时数据。
 * @property c2 Type-C2 端口实时数据。
 * @property usbA USB-A 端口实时数据。
 * @property deviceStatus 0x15 设备状态 bit 位解析结果；未读取到 0x15 时为 null。
 * @property battery 电池整体信息。
 * @property cells 单体电芯电压列表，只包含低/高字节都已读取到的电芯。
 * @property settings 设备设置相关寄存器。
 * @property firmware 固件升级相关寄存器。
 */
data class MobilePowerSnapshot(
    val c1: PortMetrics,
    val c2: PortMetrics,
    val usbA: PortMetrics,
    val deviceStatus: DeviceStatus?,
    val battery: BatteryInfo,
    val cells: List<CellVoltage>,
    val settings: DeviceSettings,
    val firmware: FirmwareInfo
)

/**
 * 单个输出端口指标。
 *
 * @property currentMa 端口电流，单位 mA，未读取到对应寄存器时为 null。
 * @property voltageMv 端口电压，单位 mV，未读取到对应寄存器时为 null。
 * @property powerW 端口功率，单位 W，未读取到对应寄存器时为 null。
 * @property protocol 当前快充协议，未读取到协议寄存器时为 null。
 */
data class PortMetrics(
    val currentMa: Int? = null,
    val voltageMv: Int? = null,
    val powerW: Int? = null,
    val protocol: FastChargeProtocol? = null
)

/**
 * 快充协议枚举。
 *
 * @property code 设备协议中的原始枚举值。
 * @property text 展示文案。
 */
enum class FastChargeProtocol(val code: Int, val text: String) {
    IDLE(0x00, "IDLE"),
    PD(0x01, "PD"),
    QC(0x02, "QC"),
    SCP(0x03, "SCP"),
    UNKNOWN(-1, "UNKNOWN")
}

/**
 * 设备状态 bit 位解析结果，对应 0x15 寄存器。
 *
 * @property lowCurrentMode bit7，小电流模式是否开启。
 * @property usbAConnected bit6，USB-A 是否连接。
 * @property c2Exception bit5，Type-C2 是否异常。
 * @property c1Exception bit4，Type-C1 是否异常。
 * @property c2Charging bit3，Type-C2 是否正在充电。
 * @property c1Charging bit2，Type-C1 是否正在充电。
 * @property c2Connected bit1，Type-C2 是否连接。
 * @property c1Connected bit0，Type-C1 是否连接。
 * @property raw 0x15 寄存器原始单字节值。
 */
data class DeviceStatus(
    val lowCurrentMode: Boolean,
    val usbAConnected: Boolean,
    val c2Exception: Boolean,
    val c1Exception: Boolean,
    val c2Charging: Boolean,
    val c1Charging: Boolean,
    val c2Connected: Boolean,
    val c1Connected: Boolean,
    val raw: Int
)

/**
 * 电池整体信息。
 *
 * @property percent 电池电量百分比，范围通常为 0..100。
 * @property temperatureC 电池温度，单位摄氏度，按有符号单字节解析。
 * @property voltageMv 电池电压，单位 mV。
 * @property currentMa 电池电流，单位 mA。
 * @property cycleCount 电池循环次数，单位次。
 * @property healthPercent 电池健康度百分比，范围通常为 0..100。
 * @property chargeRemainMinutes 预计充满剩余时间，单位分钟。
 * @property dischargeRemainMinutes 预计放电剩余时间，单位分钟。
 * @property status1 电池状态 1 原始值，具体含义由 AFE 分类定义。
 * @property status2 电池状态 2 原始值，具体含义由 AFE 分类定义。
 */
data class BatteryInfo(
    val percent: Int? = null,
    val temperatureC: Int? = null,
    val voltageMv: Int? = null,
    val currentMa: Int? = null,
    val cycleCount: Int? = null,
    val healthPercent: Int? = null,
    val chargeRemainMinutes: Int? = null,
    val dischargeRemainMinutes: Int? = null,
    val status1: Int? = null,
    val status2: Int? = null
)

/**
 * 单体电芯电压。
 *
 * @property cell 电芯序号，从 1 开始。
 * @property voltageMv 电芯电压，单位 mV。
 */
data class CellVoltage(val cell: Int, val voltageMv: Int)

/**
 * 设备设置相关寄存器。
 *
 * @property c1OutputPowerW Type-C1 输出功率设置，单位 W。
 * @property c2OutputPowerW Type-C2 输出功率设置，单位 W。
 * @property lowCurrentMode 小电流模式设置，true 表示开启，false 表示关闭。
 * @property lowCurrentLimitMinutes 小电流模式时间限制，单位分钟。
 * @property highTemperatureThresholdC 高温保护阈值，单位摄氏度，按有符号单字节解析。
 * @property lowTemperatureThresholdC 低温保护阈值，单位摄氏度，按有符号单字节解析。
 * @property modeSetting 用户设置的显示模式。
 * @property modeStatus 设备当前实际显示模式。
 */
data class DeviceSettings(
    val c1OutputPowerW: Int? = null,
    val c2OutputPowerW: Int? = null,
    val lowCurrentMode: Boolean? = null,
    val lowCurrentLimitMinutes: Int? = null,
    val highTemperatureThresholdC: Int? = null,
    val lowTemperatureThresholdC: Int? = null,
    val modeSetting: DisplayMode? = null,
    val modeStatus: DisplayMode? = null
)

/**
 * 设备显示模式。
 *
 * @property code 设备协议中的原始枚举值。
 * @property text 展示文案。
 */
enum class DisplayMode(val code: Int, val text: String) {
    STANDARD(0x00, "标准模式"),
    TIME(0x01, "时间模式"),
    WEATHER(0x02, "天气模式"),
    LYRICS(0x03, "歌词模式"),
    WECHAT(0x04, "微信模式"),
    IMAGE(0x05, "图片投影模式"),
    MOOD(0x06, "心情模式"),
    UNKNOWN(-1, "未知模式")
}

/**
 * 固件升级相关信息。
 *
 * @property upgradeCommand 升级命令低 3 位，0 空闲，1 升级开始，2 文件信息，3 停止或中断。
 * @property upgradeStatus 0x66 升级状态 bit 位解析结果。
 * @property crc16 固件 CRC16-8005 值，2 字节 little-endian。
 * @property length 固件长度，24 位 little-endian，单位 byte。
 */
data class FirmwareInfo(
    val upgradeCommand: Int? = null,
    val upgradeStatus: Payload.FirmwareStatus? = null,
    val crc16: Int? = null,
    val length: Int? = null
)
