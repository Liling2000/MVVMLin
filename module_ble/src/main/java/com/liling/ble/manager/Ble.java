package com.liling.ble.manager;

import com.liling.ble.api.BleApi;

/**
 * 蓝牙对外入口
 * @user liling
 * @Date 2021/3/2
 */
public class Ble {
    private static BleApi bleApi;

    public static BleApi getBleApi() {
        if (bleApi == null) {
            bleApi = new BleManager();
        }
        return bleApi;
    }
}
