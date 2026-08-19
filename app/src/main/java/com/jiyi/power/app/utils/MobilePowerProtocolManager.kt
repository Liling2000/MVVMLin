package com.jiyi.power.app.utils

import com.jiyi.power.app.bean.BatteryInfo
import com.jiyi.power.app.bean.CellVoltage
import com.jiyi.power.app.bean.DeviceSettings
import com.jiyi.power.app.bean.DeviceStatus
import com.jiyi.power.app.bean.DisplayMode
import com.jiyi.power.app.bean.FastChargeProtocol
import com.jiyi.power.app.bean.FirmwareInfo
import com.jiyi.power.app.bean.FunctionSpec
import com.jiyi.power.app.bean.MobilePowerSnapshot
import com.jiyi.power.app.bean.OperationType
import com.jiyi.power.app.bean.ParsedFrame
import com.jiyi.power.app.bean.Payload
import com.jiyi.power.app.bean.PortMetrics
import com.jiyi.power.app.bean.RegisterValue
import java.nio.charset.Charset
import java.util.Locale

/**
 * 移动电源业务协议解析器。
 *
 * [ProtocolUtil] 只负责把十六进制帧拆成通用字段，本类负责进一步解释功能码：
 * - 0x00..0x6B 是设备寄存器区，可以连续读取并组合成 [MobilePowerSnapshot]。
 * - 0x80 以后多为升级、图片、歌词、微信、心情模式等事件或块传输 payload。
 * - 未识别或长度不足的数据不会抛异常，而是保留为 [Payload.Unknown]，方便上层排查原始数据。
 */
object MobilePowerProtocolManager {

    private val specs: Map<String, FunctionSpec> = buildSpecs()

    fun parseFrame(frameHex: String): ParsedFrame? {
        // 先使用通用协议工具完成帧级校验；只有帧头、帧尾、长度和校验和都合法才进入业务解析。
        val raw = ProtocolUtil.parseResponse(frameHex) ?: return null
        val dataBytes = raw.dataHex.hexToByteArrayOrNull() ?: return null
        val functionSpec = specs[raw.functionCode]

        // 设备寄存器支持从任意起始功能码连续读取。这里按“起始功能码 + 数据下标”
        // 还原每个字节对应的寄存器编号，后续 buildSnapshot 再把低/高字节合并成业务字段。
        val registers = parseSequentialRegisters(raw.functionCode, dataBytes)
        val payload = parsePayload(raw.functionCode, dataBytes, registers)

        return ParsedFrame(
            raw = raw, functionSpec = functionSpec, payload = payload, registers = registers
        )
    }

    fun getFunctionSpec(code: String): FunctionSpec? {
        return specs[code.cleanHex().padStart(2, '0')]
    }

    fun getAllFunctionSpecs(): List<FunctionSpec> {
        return specs.values.sortedBy { it.code.toInt(16) }
    }

    fun buildReadCommand(functionCode: String): String? {
        return ProtocolUtil.buildReadCommand(functionCode)
    }

    fun buildReadCommand(functionCode: String, readLength: Int): String? {
        return ProtocolUtil.buildReadCommand(functionCode, readLength)
    }

    fun buildWriteByteCommand(functionCode: String, value: Int): String? {
        return ProtocolUtil.buildWriteCommand(
            functionCode, byteArrayOf(value.toByte()).toHexString()
        )
    }

    fun buildWriteUInt16LECommand(functionCode: String, value: Int): String? {
        return ProtocolUtil.buildWriteCommand(functionCode, uint16LE(value).toHexString())
    }

    fun buildEventCommand(functionCode: String, data: ByteArray = byteArrayOf()): String? {
        return ProtocolUtil.buildEventCommand(functionCode, data.toHexString())
    }

    fun buildBlockWriteCommand(functionCode: String, data: ByteArray): String? {
        return ProtocolUtil.buildBlockWriteCommand(functionCode, data.toHexString())
    }

    private fun parseSequentialRegisters(
        startCode: String, bytes: ByteArray
    ): Map<String, RegisterValue> {
        val start = startCode.toIntOrNull(16) ?: return emptyMap()

        // 0x00..0x6B 是当前定义的寄存器区。大于 0x6B 的功能码属于事件/块数据，
        // 不能按连续寄存器解释，否则图片、歌词、固件数据会被错误映射成寄存器。
        if (start > 0x6B) return emptyMap()

        return bytes.mapIndexed { index, byte ->
            val code = (start + index).toHexByteString()
            val spec = specs[code]
            code to RegisterValue(
                code = code,
                name = spec?.name ?: "未知功能码",
                value = byte.u8(),
                valueHex = byte.u8().toHexByteString(),
                description = spec?.description.orEmpty()
            )
        }.toMap()
    }

    private fun parsePayload(
        functionCode: String, bytes: ByteArray, registers: Map<String, RegisterValue>
    ): Payload {
        if (bytes.isEmpty()) return Payload.Empty

        // 先处理有固定 payload 结构的功能码；剩余的寄存器区统一构建快照。
        // 这样可以避免把 0x90/0xC0 这类事件数据误当成连续寄存器。
        return when (functionCode) {
            CmdConstant.FunctionCode.CODE_90, CmdConstant.FunctionCode.CODE_E0 -> parseUploadStart(bytes)
            CmdConstant.FunctionCode.CODE_91, CmdConstant.FunctionCode.CODE_80, CmdConstant.FunctionCode.CODE_E1 -> Payload.DataPacket(
                bytes, bytes.toHexString()
            )

            CmdConstant.FunctionCode.CODE_92, CmdConstant.FunctionCode.CODE_E2 -> parseTransferEnd(bytes)
            CmdConstant.FunctionCode.CODE_93, CmdConstant.FunctionCode.CODE_94, CmdConstant.FunctionCode.CODE_95, CmdConstant.FunctionCode.CODE_E3, CmdConstant.FunctionCode.CODE_E4, CmdConstant.FunctionCode.CODE_E5 -> parseIndexCommand(
                bytes
            )

            CmdConstant.FunctionCode.CODE_96 -> Payload.Ack(
                success = bytes[0].u8() == 0, status = bytes[0].u8()
            )

            CmdConstant.FunctionCode.CODE_99 -> parseTimeSync(bytes)
            CmdConstant.FunctionCode.CODE_B0 -> parseWeatherSync(bytes)
            CmdConstant.FunctionCode.CODE_C0 -> parseLyricsStart(bytes)
            CmdConstant.FunctionCode.CODE_C1, CmdConstant.FunctionCode.CODE_C2, CmdConstant.FunctionCode.CODE_D0, CmdConstant.FunctionCode.CODE_D1, CmdConstant.FunctionCode.CODE_F0, CmdConstant.FunctionCode.CODE_F1, CmdConstant.FunctionCode.CODE_F2, CmdConstant.FunctionCode.CODE_F3, CmdConstant.FunctionCode.CODE_F6, CmdConstant.FunctionCode.CODE_F7 -> Payload.Text(
                bytes.toCleanString()
            )

            CmdConstant.FunctionCode.CODE_C3 -> parseLyricLine(bytes)
            CmdConstant.FunctionCode.CODE_C4, CmdConstant.FunctionCode.CODE_C5, CmdConstant.FunctionCode.CODE_C6, CmdConstant.FunctionCode.CODE_D2 -> Payload.IndexCommand(
                bytes[0].u8()
            )

            CmdConstant.FunctionCode.CODE_F4, CmdConstant.FunctionCode.CODE_F5 -> Payload.UInt16Value(
                bytes.u16LE(0) ?: bytes[0].u8()
            )

            CmdConstant.FunctionCode.CODE_65 -> Payload.FirmwareUpgradeCommand(bytes[0].u8() and 0x07)
            CmdConstant.FunctionCode.CODE_66 -> parseFirmwareStatus(bytes[0].u8())
            else -> {
                val start = functionCode.toIntOrNull(16) ?: -1
                // 普通读取或连续读取寄存器时，返回完整快照；未覆盖到的字段保持 null。
                if (start in 0x00..0x6B) Payload.RegisterBlock(buildSnapshot(registers))
                else Payload.Unknown(bytes, bytes.toHexString())
            }
        }
    }

    private fun buildSnapshot(registers: Map<String, RegisterValue>): MobilePowerSnapshot {
        fun value(code: String): Int? = registers[code]?.value

        // 协议里的多字节数值采用 little-endian：低字节功能码在前，高字节功能码在后。
        // 例如 C1 电流由 0x00 + 0x01 组合为 mA。
        fun u16(lowCode: String, highCode: String): Int? {
            val low = value(lowCode) ?: return null
            val high = value(highCode) ?: return null
            return low or (high shl 8)
        }

        // 协议枚举值可能出现未在当前 App 版本中定义的新值，统一映射为 UNKNOWN，保留兼容性。
        fun protocol(code: String): FastChargeProtocol? {
            val protocolCode = value(code) ?: return null
            return FastChargeProtocol.values().firstOrNull { it.code == protocolCode }
                ?: FastChargeProtocol.UNKNOWN
        }

        fun mode(code: String): DisplayMode? {
            val modeCode = value(code) ?: return null
            return DisplayMode.values().firstOrNull { it.code == modeCode } ?: DisplayMode.UNKNOWN
        }

        // 设备状态和升级状态都是 bit field，需要单独拆位；其他寄存器大多是数值或枚举。
        val status = value(CmdConstant.FunctionCode.CODE_15)?.let { parseDeviceStatus(it) }
        val firmwareStatus =
            value(CmdConstant.FunctionCode.CODE_66)?.let { parseFirmwareStatus(it) as Payload.FirmwareStatus }

        // 电芯电压是成对的低/高字节寄存器，缺任意一个字节就不生成该电芯数据。
        val cells = listOfNotNull(
            u16(CmdConstant.FunctionCode.CODE_23, CmdConstant.FunctionCode.CODE_24)?.let {
                CellVoltage(
                    1, it
                )
            },
            u16(CmdConstant.FunctionCode.CODE_25, CmdConstant.FunctionCode.CODE_26)?.let {
                CellVoltage(
                    2,
                    it
                )
            },
            u16(CmdConstant.FunctionCode.CODE_27, CmdConstant.FunctionCode.CODE_28)?.let {
                CellVoltage(
                    3,
                    it
                )
            },
            u16(CmdConstant.FunctionCode.CODE_29, CmdConstant.FunctionCode.CODE_2A)?.let {
                CellVoltage(
                    4,
                    it
                )
            },
            u16(CmdConstant.FunctionCode.CODE_2B, CmdConstant.FunctionCode.CODE_2C)?.let {
                CellVoltage(
                    5,
                    it
                )
            },
            u16(CmdConstant.FunctionCode.CODE_2D, CmdConstant.FunctionCode.CODE_2E)?.let {
                CellVoltage(
                    6,
                    it
                )
            },
            u16(CmdConstant.FunctionCode.CODE_30, CmdConstant.FunctionCode.CODE_31)?.let {
                CellVoltage(
                    7,
                    it
                )
            })

        return MobilePowerSnapshot(
            c1 = PortMetrics(
                currentMa = u16(CmdConstant.FunctionCode.CODE_00, CmdConstant.FunctionCode.CODE_01),
                voltageMv = u16(CmdConstant.FunctionCode.CODE_02, CmdConstant.FunctionCode.CODE_03),
                powerW = u16(CmdConstant.FunctionCode.CODE_04, CmdConstant.FunctionCode.CODE_05),
                protocol = protocol(CmdConstant.FunctionCode.CODE_0C)
            ), c2 = PortMetrics(
                currentMa = u16(CmdConstant.FunctionCode.CODE_06, CmdConstant.FunctionCode.CODE_07),
                voltageMv = u16(CmdConstant.FunctionCode.CODE_08, CmdConstant.FunctionCode.CODE_09),
                powerW = u16(CmdConstant.FunctionCode.CODE_0A, CmdConstant.FunctionCode.CODE_0B),
                protocol = protocol(CmdConstant.FunctionCode.CODE_0D)
            ), usbA = PortMetrics(
                currentMa = u16(CmdConstant.FunctionCode.CODE_0E, CmdConstant.FunctionCode.CODE_0F),
                voltageMv = u16(CmdConstant.FunctionCode.CODE_10, CmdConstant.FunctionCode.CODE_11),
                powerW = value(CmdConstant.FunctionCode.CODE_12),
                protocol = protocol(CmdConstant.FunctionCode.CODE_13)
            ), deviceStatus = status, battery = BatteryInfo(
                percent = value(CmdConstant.FunctionCode.CODE_16),
                temperatureC = value(CmdConstant.FunctionCode.CODE_17)?.toSignedByteInt(),
                voltageMv = u16(CmdConstant.FunctionCode.CODE_18, CmdConstant.FunctionCode.CODE_19),
                currentMa = u16(CmdConstant.FunctionCode.CODE_1A, CmdConstant.FunctionCode.CODE_1B),
                cycleCount = u16(
                    CmdConstant.FunctionCode.CODE_1C,
                    CmdConstant.FunctionCode.CODE_1D
                ),
                healthPercent = value(CmdConstant.FunctionCode.CODE_1E),
                chargeRemainMinutes = u16(
                    CmdConstant.FunctionCode.CODE_1F,
                    CmdConstant.FunctionCode.CODE_20
                ),
                dischargeRemainMinutes = u16(
                    CmdConstant.FunctionCode.CODE_21,
                    CmdConstant.FunctionCode.CODE_22
                ),
                status1 = value(CmdConstant.FunctionCode.CODE_39),
                status2 = value(CmdConstant.FunctionCode.CODE_3A)
            ), cells = cells, settings = DeviceSettings(
                c1OutputPowerW = value(CmdConstant.FunctionCode.CODE_32),
                c2OutputPowerW = value(CmdConstant.FunctionCode.CODE_33),
                lowCurrentMode = value(CmdConstant.FunctionCode.CODE_34)?.let { it != 0 },
                lowCurrentLimitMinutes = u16(
                    CmdConstant.FunctionCode.CODE_35,
                    CmdConstant.FunctionCode.CODE_36
                ),
                highTemperatureThresholdC = value(CmdConstant.FunctionCode.CODE_37)?.toSignedByteInt(),
                lowTemperatureThresholdC = value(CmdConstant.FunctionCode.CODE_38)?.toSignedByteInt(),
                modeSetting = mode(CmdConstant.FunctionCode.CODE_60),
                modeStatus = mode(CmdConstant.FunctionCode.CODE_61)
            ), firmware = FirmwareInfo(
                upgradeCommand = value(CmdConstant.FunctionCode.CODE_65)?.let { it and 0x07 },
                upgradeStatus = firmwareStatus,
                crc16 = u16(CmdConstant.FunctionCode.CODE_67, CmdConstant.FunctionCode.CODE_68),
                // 固件长度是 24 位 little-endian，按 byte0 | byte1 << 8 | byte2 << 16 组合。
                length = value(CmdConstant.FunctionCode.CODE_69)?.let { b0 ->
                    val b1 = value(CmdConstant.FunctionCode.CODE_6A) ?: return@let null
                    val b2 = value(CmdConstant.FunctionCode.CODE_6B) ?: return@let null
                    b0 or (b1 shl 8) or (b2 shl 16)
                })
        )
    }

    private fun parseDeviceStatus(value: Int): DeviceStatus {
        // 0x15 设备状态按 bit 表示多个开关/连接状态，bit 为 1 表示对应状态有效。
        return DeviceStatus(
            lowCurrentMode = value and 0x80 != 0,
            usbAConnected = value and 0x40 != 0,
            c2Exception = value and 0x20 != 0,
            c1Exception = value and 0x10 != 0,
            c2Charging = value and 0x08 != 0,
            c1Charging = value and 0x04 != 0,
            c2Connected = value and 0x02 != 0,
            c1Connected = value and 0x01 != 0,
            raw = value
        )
    }

    private fun parseFirmwareStatus(value: Int): Payload.FirmwareStatus {
        // 0x66 升级状态：高 5 位是状态标志，低 3 位是当前升级阶段。
        return Payload.FirmwareStatus(
            finished = value and 0x80 != 0,
            transferring = value and 0x40 != 0,
            error = value and 0x20 != 0,
            ready = value and 0x10 != 0,
            appMode = value and 0x08 != 0,
            currentState = value and 0x07
        )
    }

    private fun parseUploadStart(bytes: ByteArray): Payload {
        // 图片/表情上传开始命令固定为：index(1 byte) + blockCount(2 bytes little-endian)。
        if (bytes.size < 3) return Payload.Unknown(bytes, bytes.toHexString())
        return Payload.ImageUploadStart(
            index = bytes[0].u8(), blockCount = bytes.u16LE(1) ?: 0
        )
    }

    private fun parseTransferEnd(bytes: ByteArray): Payload {
        // 传输结束命令固定为：index(1 byte) + status(1 byte)，status=0x01 表示完成。
        if (bytes.size < 2) return Payload.Unknown(bytes, bytes.toHexString())
        val status = bytes[1].u8()
        return Payload.TransferEnd(
            index = bytes[0].u8(), status = status, finished = status == 0x01
        )
    }

    private fun parseIndexCommand(bytes: ByteArray): Payload {
        return Payload.IndexCommand(bytes[0].u8())
    }

    private fun parseTimeSync(bytes: ByteArray): Payload {
        // 时间同步按 year/month/day/hour/minute/second/week 顺序传输，年份为 2000 年偏移量。
        if (bytes.size < 7) return Payload.Unknown(bytes, bytes.toHexString())
        return Payload.TimeSync(
            year = 2000 + bytes[0].u8(),
            month = bytes[1].u8(),
            day = bytes[2].u8(),
            hour = bytes[3].u8(),
            minute = bytes[4].u8(),
            second = bytes[5].u8(),
            week = bytes[6].u8()
        )
    }

    private fun parseWeatherSync(bytes: ByteArray): Payload {
        // 天气同步前 4 字节是天气和温度，后续最多 16 字节是城市名。
        if (bytes.size < 4) return Payload.Unknown(bytes, bytes.toHexString())
        return Payload.WeatherSync(
            weatherCode = bytes[0].u8(),
            currentTempC = bytes[1].toInt(),
            maxTempC = bytes[2].toInt(),
            minTempC = bytes[3].toInt(),
            city = bytes.drop(4).take(16).toByteArray().toCleanString()
        )
    }

    private fun parseLyricsStart(bytes: ByteArray): Payload {
        // 歌词开始命令：第 0 字节是歌词数量，随后 64 字节作者，再 64 字节歌曲名。
        // 长度不足时 take 会截断，字符串清理会去掉协议填充的 0x00。
        val count = bytes[0].u8()
        val author = bytes.drop(1).take(64).toByteArray().toCleanString()
        val songName = bytes.drop(65).take(64).toByteArray().toCleanString()
        return Payload.LyricsStart(count = count, author = author, songName = songName)
    }

    private fun parseLyricLine(bytes: ByteArray): Payload {
        if (bytes.isEmpty()) return Payload.Empty
        return Payload.LyricLine(
            index = bytes[0].u8(), content = bytes.drop(1).toByteArray().toCleanString()
        )
    }

    private fun buildSpecs(): Map<String, FunctionSpec> {
        // linkedMapOf 保持登记顺序，getAllFunctionSpecs 再按功能码排序，便于 UI 或调试页展示。
        val result = linkedMapOf<String, FunctionSpec>()
        fun f(code: String, name: String, op: OperationType, desc: String, block: Boolean = false) {
            result[code] = FunctionSpec(code, name, op, desc, block)
        }

        f(
            CmdConstant.FunctionCode.CODE_00,
            "C1电流低字节",
            OperationType.READ_ONLY,
            "C1当前IBUS电流低字节，单位毫安"
        )
        f(
            CmdConstant.FunctionCode.CODE_01,
            "C1电流高字节",
            OperationType.READ_ONLY,
            "C1当前IBUS电流高字节，单位毫安"
        )
        f(
            CmdConstant.FunctionCode.CODE_02,
            "C1电压低字节",
            OperationType.READ_ONLY,
            "C1当前VBUS电压低字节，单位毫伏"
        )
        f(
            CmdConstant.FunctionCode.CODE_03,
            "C1电压高字节",
            OperationType.READ_ONLY,
            "C1当前VBUS电压高字节，单位毫伏"
        )
        f(CmdConstant.FunctionCode.CODE_04, "C1功率低字节", OperationType.READ_ONLY, "C1功率低字节，单位W")
        f(CmdConstant.FunctionCode.CODE_05, "C1功率高字节", OperationType.READ_ONLY, "C1功率高字节，单位W")
        f(
            CmdConstant.FunctionCode.CODE_06,
            "C2电流低字节",
            OperationType.READ_ONLY,
            "C2当前IBUS电流低字节，单位毫安"
        )
        f(
            CmdConstant.FunctionCode.CODE_07,
            "C2电流高字节",
            OperationType.READ_ONLY,
            "C2当前IBUS电流高字节，单位毫安"
        )
        f(
            CmdConstant.FunctionCode.CODE_08,
            "C2电压低字节",
            OperationType.READ_ONLY,
            "C2当前VBUS电压低字节，单位毫伏"
        )
        f(
            CmdConstant.FunctionCode.CODE_09,
            "C2电压高字节",
            OperationType.READ_ONLY,
            "C2当前VBUS电压高字节，单位毫伏"
        )
        f(CmdConstant.FunctionCode.CODE_0A, "C2功率低字节", OperationType.READ_ONLY, "C2功率低字节，单位W")
        f(CmdConstant.FunctionCode.CODE_0B, "C2功率高字节", OperationType.READ_ONLY, "C2功率高字节，单位W")
        f(
            CmdConstant.FunctionCode.CODE_0C,
            "C1口协议",
            OperationType.READ_ONLY,
            "IDLE=0x00，PD=0x01，QC=0x02，SCP=0x03"
        )
        f(
            CmdConstant.FunctionCode.CODE_0D,
            "C2口协议",
            OperationType.READ_ONLY,
            "IDLE=0x00，PD=0x01，QC=0x02，SCP=0x03"
        )
        f(
            CmdConstant.FunctionCode.CODE_0E,
            "USBA电流低字节",
            OperationType.READ_ONLY,
            "USBA当前IBUS电流低字节，单位毫安"
        )
        f(
            CmdConstant.FunctionCode.CODE_0F,
            "USBA电流高字节",
            OperationType.READ_ONLY,
            "USBA当前IBUS电流高字节，单位毫安"
        )
        f(
            CmdConstant.FunctionCode.CODE_10,
            "USBA电压低字节",
            OperationType.READ_ONLY,
            "USBA当前VBUS电压低字节，单位毫伏"
        )
        f(
            CmdConstant.FunctionCode.CODE_11,
            "USBA电压高字节",
            OperationType.READ_ONLY,
            "USBA当前VBUS电压高字节，单位毫伏"
        )
        f(CmdConstant.FunctionCode.CODE_12, "USBA功率", OperationType.READ_ONLY, "USBA功率，单位W")
        f(
            CmdConstant.FunctionCode.CODE_13,
            "USBA协议",
            OperationType.READ_ONLY,
            "BC=0x00，PD=0x01，QC=0x02，SCP=0x03等"
        )
        f(CmdConstant.FunctionCode.CODE_14, "预留", OperationType.RESERVED, "预留")
        f(
            CmdConstant.FunctionCode.CODE_15,
            "设备状态",
            OperationType.READ_ONLY,
            "Bit7小电流模式，Bit6 USBA连接，Bit5 C2异常，Bit4 C1异常，Bit3 C2充电，Bit2 C1充电，Bit1 C2连接，Bit0 C1连接"
        )
        f(CmdConstant.FunctionCode.CODE_16, "电池电量", OperationType.READ_ONLY, "0-100%")
        f(CmdConstant.FunctionCode.CODE_17, "电池温度", OperationType.READ_ONLY, "单位摄氏度")
        f(
            CmdConstant.FunctionCode.CODE_18,
            "电池电压低字节",
            OperationType.READ_ONLY,
            "电池电压低字节，单位毫伏"
        )
        f(
            CmdConstant.FunctionCode.CODE_19,
            "电池电压高字节",
            OperationType.READ_ONLY,
            "电池电压高字节，单位毫伏"
        )
        f(
            CmdConstant.FunctionCode.CODE_1A,
            "电池电流低字节",
            OperationType.READ_ONLY,
            "电池电流低字节，单位毫安"
        )
        f(
            CmdConstant.FunctionCode.CODE_1B,
            "电池电流高字节",
            OperationType.READ_ONLY,
            "电池电流高字节，单位毫安"
        )
        f(
            CmdConstant.FunctionCode.CODE_1C,
            "电池循环次数低字节",
            OperationType.READ_ONLY,
            "电池循环次数低字节，单位次"
        )
        f(
            CmdConstant.FunctionCode.CODE_1D,
            "电池循环次数高字节",
            OperationType.READ_ONLY,
            "电池循环次数高字节，单位次"
        )
        f(CmdConstant.FunctionCode.CODE_1E, "电池健康度", OperationType.READ_ONLY, "0-100%")
        f(
            CmdConstant.FunctionCode.CODE_1F,
            "充电剩余时间低字节",
            OperationType.READ_ONLY,
            "充电剩余时间低字节，单位分钟"
        )
        f(
            CmdConstant.FunctionCode.CODE_20,
            "充电剩余时间高字节",
            OperationType.READ_ONLY,
            "充电剩余时间高字节，单位分钟"
        )
        f(
            CmdConstant.FunctionCode.CODE_21,
            "放电剩余时间低字节",
            OperationType.READ_ONLY,
            "放电剩余时间低字节，单位分钟"
        )
        f(
            CmdConstant.FunctionCode.CODE_22,
            "放电剩余时间高字节",
            OperationType.READ_ONLY,
            "放电剩余时间高字节，单位分钟"
        )
        f(
            CmdConstant.FunctionCode.CODE_23,
            "Cell1电压低字节",
            OperationType.READ_ONLY,
            "Cell1电压低字节，单位毫伏"
        )
        f(
            CmdConstant.FunctionCode.CODE_24,
            "Cell1电压高字节",
            OperationType.READ_ONLY,
            "Cell1电压高字节，单位毫伏"
        )
        f(
            CmdConstant.FunctionCode.CODE_25,
            "Cell2电压低字节",
            OperationType.READ_ONLY,
            "Cell2电压低字节，单位毫伏"
        )
        f(
            CmdConstant.FunctionCode.CODE_26,
            "Cell2电压高字节",
            OperationType.READ_ONLY,
            "Cell2电压高字节，单位毫伏"
        )
        f(
            CmdConstant.FunctionCode.CODE_27,
            "Cell3电压低字节",
            OperationType.READ_ONLY,
            "Cell3电压低字节，单位毫伏"
        )
        f(
            CmdConstant.FunctionCode.CODE_28,
            "Cell3电压高字节",
            OperationType.READ_ONLY,
            "Cell3电压高字节，单位毫伏"
        )
        f(
            CmdConstant.FunctionCode.CODE_29,
            "Cell4电压低字节",
            OperationType.READ_ONLY,
            "Cell4电压低字节，单位毫伏"
        )
        f(
            CmdConstant.FunctionCode.CODE_2A,
            "Cell4电压高字节",
            OperationType.READ_ONLY,
            "Cell4电压高字节，单位毫伏"
        )
        f(
            CmdConstant.FunctionCode.CODE_2B,
            "Cell5电压低字节",
            OperationType.READ_ONLY,
            "Cell5电压低字节，单位毫伏"
        )
        f(
            CmdConstant.FunctionCode.CODE_2C,
            "Cell5电压高字节",
            OperationType.READ_ONLY,
            "Cell5电压高字节，单位毫伏"
        )
        f(
            CmdConstant.FunctionCode.CODE_2D,
            "Cell6电压低字节",
            OperationType.READ_ONLY,
            "Cell6电压低字节，单位毫伏"
        )
        f(
            CmdConstant.FunctionCode.CODE_2E,
            "Cell6电压高字节",
            OperationType.READ_ONLY,
            "Cell6电压高字节，单位毫伏"
        )
        f(
            CmdConstant.FunctionCode.CODE_30,
            "Cell7电压低字节",
            OperationType.READ_ONLY,
            "Cell7电压低字节，单位毫伏"
        )
        f(
            CmdConstant.FunctionCode.CODE_31,
            "Cell7电压高字节",
            OperationType.READ_ONLY,
            "Cell7电压高字节，单位毫伏"
        )
        f(CmdConstant.FunctionCode.CODE_32, "C1输出功率设置", OperationType.READ_WRITE, "C1输出功率设置，单位W")
        f(CmdConstant.FunctionCode.CODE_33, "C2输出功率设置", OperationType.READ_WRITE, "C2输出功率设置，单位W")
        f(CmdConstant.FunctionCode.CODE_34, "小电流模式设置", OperationType.READ_WRITE, "小电流模式0/1")
        f(
            CmdConstant.FunctionCode.CODE_35,
            "小电流时间限制低字节",
            OperationType.READ_WRITE,
            "小电流时间限制低字节，单位分钟"
        )
        f(
            CmdConstant.FunctionCode.CODE_36,
            "小电流时间限制高字节",
            OperationType.READ_WRITE,
            "小电流时间限制高字节，单位分钟"
        )
        f(CmdConstant.FunctionCode.CODE_37, "高温保护阈值", OperationType.READ_WRITE, "高温保护阈值，单位摄氏度")
        f(CmdConstant.FunctionCode.CODE_38, "低温保护阈值", OperationType.READ_WRITE, "低温保护阈值，单位摄氏度")
        f(CmdConstant.FunctionCode.CODE_39, "电池状态1", OperationType.READ_ONLY, "根据AFE分类")
        f(CmdConstant.FunctionCode.CODE_3A, "电池状态2", OperationType.READ_ONLY, "根据AFE分类")
        f(
            CmdConstant.FunctionCode.CODE_60,
            "模式设置",
            OperationType.READ_WRITE,
            "0标准，1时间，2天气，3歌词，4微信，5图片投影，6心情"
        )
        f(CmdConstant.FunctionCode.CODE_61, "模式状态", OperationType.READ_ONLY, "当前模式状态，定义参考60H")
        f(
            CmdConstant.FunctionCode.CODE_65,
            "升级命令",
            OperationType.WRITE,
            "Bit2-0：0空闲，1升级开始，2升级开始文件信息，3升级停止或中断"
        )
        f(
            CmdConstant.FunctionCode.CODE_66,
            "升级状态",
            OperationType.READ,
            "Bit7完成，Bit6传输中，Bit5错误，Bit4 Ready，Bit3 APP模式，Bit2-0当前状态"
        )
        f(CmdConstant.FunctionCode.CODE_67, "固件CRC值低字节", OperationType.READ_WRITE, "CRC16-8005低字节")
        f(CmdConstant.FunctionCode.CODE_68, "固件CRC值高字节", OperationType.READ_WRITE, "CRC16-8005高字节")
        f(CmdConstant.FunctionCode.CODE_69, "24位固件长度字节0", OperationType.READ_WRITE, "固件长度低字节")
        f(CmdConstant.FunctionCode.CODE_6A, "24位固件长度字节1", OperationType.READ_WRITE, "固件长度中字节")
        f(CmdConstant.FunctionCode.CODE_6B, "24位固件长度字节2", OperationType.READ_WRITE, "固件长度高字节")
        f(CmdConstant.FunctionCode.CODE_80, "升级数据", OperationType.WRITE_ONLY, "升级数据0-256bytes", true)
        f(
            CmdConstant.FunctionCode.CODE_90,
            "图片上传开始",
            OperationType.EVENT,
            "图片index和图片数据块总数",
            true
        )
        f(CmdConstant.FunctionCode.CODE_91, "图片数据包", OperationType.WRITE_ONLY, "图片数据0-256bytes", true)
        f(
            CmdConstant.FunctionCode.CODE_92,
            "图片数据传输结束",
            OperationType.EVENT,
            "图片index和传输状态",
            true
        )
        f(CmdConstant.FunctionCode.CODE_93, "图片数据删除", OperationType.EVENT, "删除的图片index", true)
        f(CmdConstant.FunctionCode.CODE_94, "图片设置", OperationType.EVENT, "设置为主题图片的index", true)
        f(CmdConstant.FunctionCode.CODE_95, "图片数据查询", OperationType.EVENT, "要查询的图片index", true)
        f(CmdConstant.FunctionCode.CODE_96, "从机事件ACK", OperationType.RESPONSE, "0x00 OK，0x01 ERR", true)
        f(CmdConstant.FunctionCode.CODE_99, "时间同步", OperationType.EVENT, "年、月、日、时、分、秒、星期")
        f(
            CmdConstant.FunctionCode.CODE_B0,
            "天气同步",
            OperationType.EVENT,
            "天气、当前温度、最高温度、最低温度、城市"
        )
        f(CmdConstant.FunctionCode.CODE_C0, "歌词总数和开始传输", OperationType.EVENT, "歌词总数、作者、歌曲名")
        f(CmdConstant.FunctionCode.CODE_C1, "歌曲名称", OperationType.EVENT, "128bytes字符串，协议标注删除")
        f(CmdConstant.FunctionCode.CODE_C2, "歌曲作者", OperationType.EVENT, "128bytes字符串，协议标注删除")
        f(CmdConstant.FunctionCode.CODE_C3, "歌词传输", OperationType.WRITE_ONLY, "歌词index和歌词数据")
        f(CmdConstant.FunctionCode.CODE_C4, "歌词传输结束", OperationType.EVENT, "0x01完成，0x02中断")
        f(CmdConstant.FunctionCode.CODE_C5, "演唱的进度", OperationType.EVENT, "当前歌词index")
        f(CmdConstant.FunctionCode.CODE_C6, "歌词删除", OperationType.EVENT, "0x01删除保存的歌词")
        f(CmdConstant.FunctionCode.CODE_D0, "微信新消息", OperationType.EVENT, "来电人员名字，64bytes")
        f(CmdConstant.FunctionCode.CODE_D1, "微信消息内容", OperationType.EVENT, "消息内容，最多256bytes")
        f(CmdConstant.FunctionCode.CODE_D2, "微信消息设置", OperationType.EVENT, "0x01删除，0x02已读")
        f(CmdConstant.FunctionCode.CODE_E0, "心情模式表情传输", OperationType.EVENT, "index和长度", true)
        f(CmdConstant.FunctionCode.CODE_E1, "表情数据包", OperationType.EVENT, "表情数据0-256bytes", true)
        f(CmdConstant.FunctionCode.CODE_E2, "表情数据传输结束", OperationType.EVENT, "index和状态", true)
        f(CmdConstant.FunctionCode.CODE_E3, "表情数据删除", OperationType.EVENT, "表情index", true)
        f(CmdConstant.FunctionCode.CODE_E4, "表情设置", OperationType.EVENT, "表情index", true)
        f(CmdConstant.FunctionCode.CODE_E5, "表情数据查询", OperationType.EVENT, "表情index", true)
        f(CmdConstant.FunctionCode.CODE_E6, "表情总数", OperationType.READ_ONLY, "回复表情总数，预留", true)
        f(CmdConstant.FunctionCode.CODE_F0, "设备型号", OperationType.EVENT, "32bytes字符串")
        f(CmdConstant.FunctionCode.CODE_F1, "序列号", OperationType.EVENT, "32bytes字符串")
        f(CmdConstant.FunctionCode.CODE_F2, "生产批次", OperationType.EVENT, "32bytes字符串")
        f(CmdConstant.FunctionCode.CODE_F3, "生产日期", OperationType.EVENT, "16bytes字符串")
        f(CmdConstant.FunctionCode.CODE_F4, "额定容量", OperationType.EVENT, "2bytes，低8位在前，单位mAh")
        f(CmdConstant.FunctionCode.CODE_F5, "标称电压", OperationType.EVENT, "2bytes，低8位在前，单位mV")
        f(CmdConstant.FunctionCode.CODE_F6, "电池信息", OperationType.EVENT, "16bytes字符串")
        f(CmdConstant.FunctionCode.CODE_F7, "版本号", OperationType.EVENT, "16bytes字符串")

        return result
    }

    private fun String.hexToByteArrayOrNull(): ByteArray? {
        val clean = cleanHex()
        if (clean.isEmpty()) return byteArrayOf()
        if (clean.length % 2 != 0) return null
        return try {
            ByteArray(clean.length / 2) { index ->
                clean.substring(index * 2, index * 2 + 2).toInt(16).toByte()
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun String.cleanHex(): String {
        return replace("0x", "", ignoreCase = true).replace(" ", "").replace("\n", "")
            .replace("\r", "").replace("\t", "").uppercase(Locale.US)
    }

    private fun Byte.u8(): Int = toInt() and 0xFF

    private fun Int.toSignedByteInt(): Int = toByte().toInt()

    private fun Int.toHexByteString(): String = "%02X".format(Locale.US, this and 0xFF)

    private fun ByteArray.u16LE(offset: Int): Int? {
        if (offset < 0 || offset + 1 >= size) return null
        return this[offset].u8() or (this[offset + 1].u8() shl 8)
    }

    private fun ByteArray.toHexString(): String {
        return joinToString(separator = "") { it.u8().toHexByteString() }
    }

    private fun ByteArray.toCleanString(charset: Charset = Charsets.UTF_8): String {
        return String(this, charset).trim('\u0000', ' ', '\r', '\n', '\t')
    }

    private fun uint16LE(value: Int): ByteArray {
        return byteArrayOf((value and 0xFF).toByte(), ((value shr 8) and 0xFF).toByte())
    }
}