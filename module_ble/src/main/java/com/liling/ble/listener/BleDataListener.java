package com.liling.ble.listener;

import android.bluetooth.BluetoothDevice;

/**
 * 蓝牙数据获取监听
 * @user liling
 * @Date 2021/1/27
 */
public interface BleDataListener {

    /**
     * 蓝牙通知数据
     * @param device 蓝牙设备
     * @param data 发送数据
     */
    void onBleNotify(BluetoothDevice device, byte[] data);

    /**
     * 蓝牙连接状态获取
     * @param device 当前设备
     * @param state 连接状态
     */
    void sendConnectState(BluetoothDevice device, int state);

    /**
     * 连接成功后写数据
     */
    void sendWriteMsg(BluetoothDevice device, int state);

    /**
     * mtu扩展回调
     */
    void mtuResponse(String sn, int state, int mtu);

    /**
     * 信号值读取回调
     */
    void onRssiResponse(String sn, String model, int rssi);
}
