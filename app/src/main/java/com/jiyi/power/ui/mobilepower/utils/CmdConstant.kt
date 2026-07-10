package com.jiyi.power.ui.mobilepower.utils

object CmdConstant {
    const val FRAME_HEAD = 0xAA
    const val FRAME_TAIL = 0x55
    const val MIN_FRAME_LENGTH = 6

    const val INDEX_FRAME_HEAD = 0
    const val INDEX_COMMAND_CODE = 1
    const val INDEX_FUNCTION_CODE = 2
    const val INDEX_DATA_LENGTH = 3
    const val INDEX_DATA_START = 4
    const val BYTE_MASK = 0xFF
    const val DATA_LENGTH_EXTEND_MASK = 0x03
    const val MAX_DATA_LENGTH = 0x3FF

    object CommandType {
        const val READ = 0x00
        const val WRITE = 0x01
        const val RESPONSE = 0x02
        const val EVENT = 0x03
    }

    object CommandBit {
        const val TYPE_MASK = 0x0F
        const val CONTINUOUS = 0x10
        const val BLOCK = 0x20
        const val DATA_LENGTH_EXTEND = 0xC0
    }

    object FunctionCode {
        /** C1电流低字节 */
        const val CODE_00 = "00"
        /** C1电流高字节 */
        const val CODE_01 = "01"
        /** C1电压低字节 */
        const val CODE_02 = "02"
        /** C1电压高字节 */
        const val CODE_03 = "03"
        /** C1功率低字节 */
        const val CODE_04 = "04"
        /** C1功率高字节 */
        const val CODE_05 = "05"
        /** C2电流低字节 */
        const val CODE_06 = "06"
        /** C2电流高字节 */
        const val CODE_07 = "07"
        /** C2电压低字节 */
        const val CODE_08 = "08"
        /** C2电压高字节 */
        const val CODE_09 = "09"
        /** C2功率低字节 */
        const val CODE_0A = "0A"
        /** C2功率高字节 */
        const val CODE_0B = "0B"
        /** C1口协议 */
        const val CODE_0C = "0C"
        /** C2口协议 */
        const val CODE_0D = "0D"
        /** USBA电流低字节 */
        const val CODE_0E = "0E"
        /** USBA电流高字节 */
        const val CODE_0F = "0F"
        /** USBA电压低字节 */
        const val CODE_10 = "10"
        /** USBA电压高字节 */
        const val CODE_11 = "11"
        /** USBA功率 */
        const val CODE_12 = "12"
        /** USBA协议 */
        const val CODE_13 = "13"
        /** 预留 */
        const val CODE_14 = "14"
        /** 设备状态 */
        const val CODE_15 = "15"
        /** 电池电量 */
        const val CODE_16 = "16"
        /** 电池温度 */
        const val CODE_17 = "17"
        /** 电池电压低字节 */
        const val CODE_18 = "18"
        /** 电池电压高字节 */
        const val CODE_19 = "19"
        /** 电池电流低字节 */
        const val CODE_1A = "1A"
        /** 电池电流高字节 */
        const val CODE_1B = "1B"
        /** 电池循环次数低字节 */
        const val CODE_1C = "1C"
        /** 电池循环次数高字节 */
        const val CODE_1D = "1D"
        /** 电池健康度 */
        const val CODE_1E = "1E"
        /** 充电剩余时间低字节 */
        const val CODE_1F = "1F"
        /** 充电剩余时间高字节 */
        const val CODE_20 = "20"
        /** 放电剩余时间低字节 */
        const val CODE_21 = "21"
        /** 放电剩余时间高字节 */
        const val CODE_22 = "22"
        /** Cell1电压低字节 */
        const val CODE_23 = "23"
        /** Cell1电压高字节 */
        const val CODE_24 = "24"
        /** Cell2电压低字节 */
        const val CODE_25 = "25"
        /** Cell2电压高字节 */
        const val CODE_26 = "26"
        /** Cell3电压低字节 */
        const val CODE_27 = "27"
        /** Cell3电压高字节 */
        const val CODE_28 = "28"
        /** Cell4电压低字节 */
        const val CODE_29 = "29"
        /** Cell4电压高字节 */
        const val CODE_2A = "2A"
        /** Cell5电压低字节 */
        const val CODE_2B = "2B"
        /** Cell5电压高字节 */
        const val CODE_2C = "2C"
        /** Cell6电压低字节 */
        const val CODE_2D = "2D"
        /** Cell6电压高字节 */
        const val CODE_2E = "2E"
        /** Cell7电压低字节 */
        const val CODE_30 = "30"
        /** Cell7电压高字节 */
        const val CODE_31 = "31"
        /** C1输出功率设置 */
        const val CODE_32 = "32"
        /** C2输出功率设置 */
        const val CODE_33 = "33"
        /** 小电流模式设置 */
        const val CODE_34 = "34"
        /** 小电流时间限制低字节 */
        const val CODE_35 = "35"
        /** 小电流时间限制高字节 */
        const val CODE_36 = "36"
        /** 高温保护阈值 */
        const val CODE_37 = "37"
        /** 低温保护阈值 */
        const val CODE_38 = "38"
        /** 电池状态1 */
        const val CODE_39 = "39"
        /** 电池状态2 */
        const val CODE_3A = "3A"
        /** 模式设置 */
        const val CODE_60 = "60"
        /** 模式状态 */
        const val CODE_61 = "61"
        /** 升级命令 */
        const val CODE_65 = "65"
        /** 升级状态 */
        const val CODE_66 = "66"
        /** 固件CRC值低字节 */
        const val CODE_67 = "67"
        /** 固件CRC值高字节 */
        const val CODE_68 = "68"
        /** 24位固件长度字节0 */
        const val CODE_69 = "69"
        /** 24位固件长度字节1 */
        const val CODE_6A = "6A"
        /** 24位固件长度字节2 */
        const val CODE_6B = "6B"
        /** 升级数据 */
        const val CODE_80 = "80"
        /** 图片上传开始 */
        const val CODE_90 = "90"
        /** 图片数据包 */
        const val CODE_91 = "91"
        /** 图片数据传输结束 */
        const val CODE_92 = "92"
        /** 图片数据删除 */
        const val CODE_93 = "93"
        /** 图片设置 */
        const val CODE_94 = "94"
        /** 图片数据查询 */
        const val CODE_95 = "95"
        /** 从机事件ACK */
        const val CODE_96 = "96"
        /** 时间同步 */
        const val CODE_99 = "99"
        /** 天气同步 */
        const val CODE_B0 = "B0"
        /** 歌词总数和开始传输 */
        const val CODE_C0 = "C0"
        /** 歌曲名称 */
        const val CODE_C1 = "C1"
        /** 歌曲作者 */
        const val CODE_C2 = "C2"
        /** 歌词传输 */
        const val CODE_C3 = "C3"
        /** 歌词传输结束 */
        const val CODE_C4 = "C4"
        /** 演唱的进度 */
        const val CODE_C5 = "C5"
        /** 歌词删除 */
        const val CODE_C6 = "C6"
        /** 微信新消息 */
        const val CODE_D0 = "D0"
        /** 微信消息内容 */
        const val CODE_D1 = "D1"
        /** 微信消息设置 */
        const val CODE_D2 = "D2"
        /** 心情模式表情传输 */
        const val CODE_E0 = "E0"
        /** 表情数据包 */
        const val CODE_E1 = "E1"
        /** 表情数据传输结束 */
        const val CODE_E2 = "E2"
        /** 表情数据删除 */
        const val CODE_E3 = "E3"
        /** 表情设置 */
        const val CODE_E4 = "E4"
        /** 表情数据查询 */
        const val CODE_E5 = "E5"
        /** 表情总数 */
        const val CODE_E6 = "E6"
        /** 设备型号 */
        const val CODE_F0 = "F0"
        /** 序列号 */
        const val CODE_F1 = "F1"
        /** 生产批次 */
        const val CODE_F2 = "F2"
        /** 生产日期 */
        const val CODE_F3 = "F3"
        /** 额定容量 */
        const val CODE_F4 = "F4"
        /** 标称电压 */
        const val CODE_F5 = "F5"
        /** 电池信息 */
        const val CODE_F6 = "F6"
        /** 版本号 */
        const val CODE_F7 = "F7"
    }
}
