package com.liling.ble.callback;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.Context;

import java.util.List;

/**
 * 蓝牙操作接口
 * @user liling
 * @Date 2021/2/1
 */
public interface OnBletoothCallBack {

    /**
     * 打开蓝牙
     * @param adapter 蓝牙适配器
     */
    void startBle(BluetoothAdapter adapter);

    /**
     * 添加扫描过滤器
     * @param deviceName 设备名称
     * @param isSetFilter 是否设置过滤器
     * @return 扫描过滤器
     */
    List<ScanFilter> getScanFilters(String deviceName, boolean isSetFilter);

    /**
     * 蓝牙设置
     * @param context 上下文引用
     * @param adapter 蓝牙适配器
     * @return 扫描设置
     */
    ScanSettings getScanSettings(Context context, BluetoothAdapter adapter);

    /**
     * 判断扫描出来的设备是否属于有效设备
     * @param deviceName 设备名称
     */
    boolean isTheEquipmentValid(String deviceName);


}
