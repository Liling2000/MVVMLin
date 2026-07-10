package com.liling.ble.utils;

import android.app.ActivityManager;
import android.content.Context;

import java.util.List;

/**
 * @user liling
 * @Date 2026/1/29
 */
public class Utils {

    /**
     * 判断app是否在后台
     * @param context 上下文引用
     * @return true:后台 false:前端
     */
    public static boolean isBackground(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcessInfos = manager.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcessInfos) {
            if (appProcess.processName.equals(context.getPackageName())) {
                if (appProcess.importance != ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    return true;
                } else {
                    return false;
                }
            }
        }
        return false;
    }
}
