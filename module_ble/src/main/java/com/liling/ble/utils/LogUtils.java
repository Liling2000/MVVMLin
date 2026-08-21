package com.liling.ble.utils;

import android.util.Log;

import com.orhanobut.logger.Logger;

/**
 * log工具
 * @user liling
 * @Date 2021/11/18
 */
public class LogUtils {

    private static boolean isShowLog = true; //日志打印开关 默认关闭

    /**
     * 设置日志打印开关
     * @param isShowLog1 是否显示日志 true:显示 fasle:关闭
     */
    public static void setIsShowLog(boolean isShowLog1) {
        isShowLog = isShowLog1;
    }

    public static void e(String msg) {
        if (isShowLog) {
//            Logger.e(msg);
            Log.e("LogUtils", msg);
        }
    }

    public static void v(String msg) {
        if (isShowLog) {
            Logger.v(msg);
        }
    }

    public static void d(String msg) {
        if (isShowLog) {
            Logger.d(msg);
        }
    }

    public static void i(String msg) {
        if (isShowLog) {
            Logger.i(msg);
        }
    }

    public static void w(String msg) {
        if (isShowLog) {
            Logger.w(msg);
        }
    }
}
