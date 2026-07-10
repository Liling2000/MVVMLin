package com.liling.ble.utils;

import android.bluetooth.BluetoothDevice;

import com.liling.ble.constant.BleConstant;
import com.liling.ble.data.BleAdvertisedData;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 蓝牙数据处理工具类
 * @user liling
 * @Date 2021/1/26
 */
public class BleUtils {

    /**
     * 16进制字符串转字节数组
     * @param string 16进制字符串
     * @return 字节数组
     */
    public static byte[] stringToBytes(String string) throws IllegalArgumentException {
        int length;
        if ((length = string.length()) % 2 != 0) {
            throw new IllegalArgumentException("Hex string has odd number of characters");
        } else {
            byte[] bytes = new byte[length / 2];

            for (int temp = 0; temp < length; temp += 2) {
                bytes[temp / 2] = (byte) Integer.parseInt(string.substring(temp, temp + 2), 16);
            }
            return bytes;
        }
    }

    /**
     * 字节数组转换为十六进制字符串
     *
     * @param b 需要转换的字节数组
     * @return String 十六进制字符串
     */
    public static String byte2hex(byte b[]) {
        if (b == null) {
            throw new IllegalArgumentException(
                    "Argument b ( byte array ) is null! ");
        }
        String hs = "";
        String stmp = "";
        for (int n = 0; n < b.length; n++) {
            stmp = Integer.toHexString(b[n] & 0xff);
            if (stmp.length() == 1) {
                hs = hs + "0" + stmp;
            } else {
                hs = hs + stmp;
            }
        }
        return hs.toUpperCase();
    }

    /**
     * 两个byte转int
     * @param b 高位
     * @param c 低位
     * @return 整数型
     */
    public static int byteToInt(byte b, byte c) {
        short s = 0;
        int ret;
        short s0 = (short) (c & 0xff);// 最低位
        short s1 = (short) (b & 0xff);
        s1 <<= 8;
        s = (short) (s0 | s1);
        ret = s;
        return ret;
    }

    /**
     * int转两个byte
     * @param res 整型
     * @return 字节数组
     */
    private byte[] int2byte(int res) {
        byte[] targets = new byte[2];
        targets[1] = (byte) (res & 0xff);// 最低位
        targets[0] = (byte) ((res >> 8) & 0xff);// 次低位
        return targets;
    }

    /**
     * 16进制字符串转byte数组
     * @param hex 16进制字符串
     * @return byte数组
     */
    public static byte[] hexStringToByte(String hex) {
        int len = (hex.length() / 2);
        byte[] result = new byte[len];
        char[] achar = hex.toCharArray();
        for (int i = 0; i < len; i++) {
            int pos = i * 2;
            result[i] = (byte) (toByte(achar[pos]) << 4 | toByte(achar[pos + 1]));
        }
        return result;
    }

    /**
     * 单个字符转byte
     * @param c 字符
     * @return 字节
     */
    private static byte toByte(char c) {
        byte b = (byte) "0123456789ABCDEF".indexOf(c);
        return b;
    }

    /**
     * 通过蓝牙扫描结果获取名称
     * @param advertisedData 扫描结果
     * @return 蓝牙设备对象
     */
    public static BleAdvertisedData parseAdertisedData(byte[] advertisedData) {
        List<UUID> uuids = new ArrayList<UUID>();
        String name = null;
        if( advertisedData == null ){
            return new BleAdvertisedData(uuids, name);
        }
        ByteBuffer buffer = ByteBuffer.wrap(advertisedData).order(ByteOrder.LITTLE_ENDIAN);
        while (buffer.remaining() > 2) {
            byte length = buffer.get();
            if (length == 0) break;

            byte type = buffer.get();
            switch (type) {
                case 0x02: // Partial list of 16-bit UUIDs
                case 0x03: // Complete list of 16-bit UUIDs
                    while (length >= 2) {
                        uuids.add(UUID.fromString(String.format(
                                "%08x-0000-1000-8000-00805f9b34fb", buffer.getShort())));
                        length -= 2;
                    }
                    break;
                case 0x06: // Partial list of 128-bit UUIDs
                case 0x07: // Complete list of 128-bit UUIDs
                    while (length >= 16) {
                        long lsb = buffer.getLong();
                        long msb = buffer.getLong();
                        uuids.add(new UUID(msb, lsb));
                        length -= 16;
                    }
                    break;
                case 0x09:
                    byte[] nameBytes = new byte[length-1];
                    buffer.get(nameBytes);
                    try {
                        name = new String(nameBytes, "utf-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    break;
                default:
                    buffer.position(buffer.position() + length - 1);
                    break;
            }
        }
        return new BleAdvertisedData(uuids, name);
    }

    public static boolean isEmpty(byte[] bytes) {
        return bytes == null || bytes.length == 0;
    }

    public static String byteToString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();

        if (!isEmpty(bytes)) {
            for (int i = 0; i < bytes.length; i++) {
                sb.append(String.format("%02X", bytes[i]));
            }
        }

        return sb.toString();
    }
}
