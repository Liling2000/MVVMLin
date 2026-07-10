package com.liling.ble.callback;

import android.bluetooth.le.ScanResult;

import java.util.List;

/**
 * 蓝牙扫描设备回调
 * @user liling
 * @Date 2021/2/3
 */
public interface BleScanDeviceCallBack {

    /**
     * 扫描结束
     * @param scanResults 扫描结果
     */
    void scanFinished(List<ScanResult> scanResults);

    /**
     * 扫描到的第一个设备
     */
    void scanFirstDevice(ScanResult scanResult);

    /**
     * 扫描即时回调
     */
    void scanDevice(ScanResult scanResult);
}
