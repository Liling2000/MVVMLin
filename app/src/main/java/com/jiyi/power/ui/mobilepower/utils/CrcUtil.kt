package com.jiyi.power.ui.mobilepower.utils

object CrcUtil {

    private const val CRC_16_POLY_8005 = 0x8005
    private const val CRC_16_MASK = 0xFFFF
    private const val CRC_16_HIGH_BIT = 0x8000
    private const val BYTE_MASK = 0xFF

    /**
     * CRC-16: x16 + x15 + x2 + 1
     *
     * 多项式：0x8005
     *
     * @param data 待计算数据
     * @param initValue 初始值，通常为 0x0000 或 0xFFFF
     * @param xorOut 最终输出异或值，通常为 0x0000
     */
    fun crc16_8005(
        data: ByteArray, initValue: Int = 0x0000, xorOut: Int = 0x0000
    ): Int {
        var crc = initValue and CRC_16_MASK

        for (byte in data) {
            crc = crc xor ((byte.toInt() and BYTE_MASK) shl 8)

            repeat(8) {
                crc = if (crc and CRC_16_HIGH_BIT != 0) {
                    (crc shl 1) xor CRC_16_POLY_8005
                } else {
                    crc shl 1
                }

                crc = crc and CRC_16_MASK
            }
        }

        return (crc xor xorOut) and CRC_16_MASK
    }

    /**
     * CRC-16: x16 + x15 + x2 + 1
     *
     * 多项式：0x8005
     *
     * @param data 待计算数据
     * @param initValue 初始值，通常为 0x0000 或 0xFFFF
     * @param xorOut 最终输出异或值，通常为 0x0000
     */
    fun crc16_8005Bytes(
        data: ByteArray,
        initValue: Int = 0x0000,
        xorOut: Int = 0x0000,
        highByteFirst: Boolean = true
    ): ByteArray {
        val crc = crc16_8005(data, initValue, xorOut)

        val highByte = ((crc shr 8) and BYTE_MASK).toByte()
        val lowByte = (crc and BYTE_MASK).toByte()

        return if (highByteFirst) {
            byteArrayOf(highByte, lowByte)
        } else {
            byteArrayOf(lowByte, highByte)
        }
    }

    /**
     * CRC-16: x16 + x15 + x2 + 1
     *
     * 多项式：0x8005
     *
     * @param data 待计算数据
     * @param initValue 初始值，通常为 0x0000 或 0xFFFF
     * @param xorOut 最终输出异或值，通常为 0x0000
     * @return 返回16进制字符串
     */
    fun crc16_8005Hex(
        data: ByteArray,
        initValue: Int = 0x0000,
        xorOut: Int = 0x0000,
        highByteFirst: Boolean = true
    ): String {
        return crc16_8005Bytes(
            data = data, initValue = initValue, xorOut = xorOut, highByteFirst = highByteFirst
        ).joinToString(separator = "") {
            "%02X".format(it.toInt() and BYTE_MASK)
        }
    }
}