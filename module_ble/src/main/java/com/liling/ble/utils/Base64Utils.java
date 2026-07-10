package com.liling.ble.utils;

import java.io.UnsupportedEncodingException;
import java.util.Base64;

/**
 * base64转换器
 * @author liling
 * @date 2020/7/31
 */
public class Base64Utils {

    /**
     * 转为Base64字符串
     * @param input
     * @return
     */
    public static String toBase64(byte[] input) {
        return android.util.Base64.encodeToString(input, android.util.Base64.NO_WRAP);

    }

    public static String baseConvertStr(String str) {
        if(null != str){
            Base64.Decoder decoder = Base64.getDecoder();
            try {
                return new String(decoder.decode(str.getBytes()), "GBK");
            } catch (UnsupportedEncodingException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 转为Base64字符串
     * @param input
     * @return
     */
    public static String toBase64(byte[] input, int flags) {
        return android.util.Base64.encodeToString(input, flags);
    }
}
