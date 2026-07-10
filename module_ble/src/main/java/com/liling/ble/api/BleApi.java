package com.liling.ble.api;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.content.Context;

import com.liling.ble.TaskPriority;
import com.liling.ble.callback.BleScanDeviceCallBack;
import com.liling.ble.listener.BleDataListener;
import com.liling.ble.listener.BleWriteDataStatueListener;

import java.util.List;

/**
 * 蓝牙模块对外提供api
 * @user liling
 * @Date 2021/3/2
 */
public interface BleApi extends BleEnhancedApi{

    /**
     * 初始化蓝牙
     * @param context 上下文
     */
    void init(Context context);

    /**
     * 设置蓝牙数据监听回调
     * @param bleDataListener 监听器
     */
    void setOnBleDataListener(BleDataListener bleDataListener);

    /**
     * 扫描结果回调
     * @param scanResultCallBack 扫描结果监听
     */
    void setOnBleScanResultCallBack(BleScanDeviceCallBack scanResultCallBack);

    /**
     * 判断蓝牙是否打开
     * @return 蓝牙是否打开
     */
    boolean isBleOpen();

    /**
     * 开启蓝牙
     */
    void startBle();

    /**
     * 全局扫描设备
     */
    void scanDevice();

    /**
     * 扫描指定设备
     */
    void scanDevice(String deviceName);

    /**
     * 扫描指定设备
     * @param isSetFilter 是否设置过滤器
     */
    void scanDevice(boolean isSetFilter);

    /**
     * 延时停止扫描
     */
    void stopScanDelay(boolean isDelay);

    /**
     * 通过设备对象连接蓝牙(请求存放队列)
     * @param mBluetoothDevice 蓝牙设备对象
     * @param model 蓝牙名称
     */
    @Deprecated
    void connectBleDevice(BluetoothDevice mBluetoothDevice, String model);

    /**
     * 通过sn号连接蓝牙(请求存放队列)
     * @param sn mac地址
     * @param model 蓝牙名称
     */
    @Deprecated
    void connectBleDeviceBySn(String sn, String model);

    /**
     * 连接请求不存放队列
     * @param model 设备名称
     * @param mBluetoothDevice 设备对象
     */
    void connectNoQueue(BluetoothDevice mBluetoothDevice, String model);

    /**
     * 连接请求存放队列
     * @param model 设备名称
     * @param mBluetoothDevice 设备对象
     */
    void connectWithQueue(BluetoothDevice mBluetoothDevice, String model);

    /**
     * 断连蓝牙
     * @param mBluetoothDevice 蓝牙设备对象
     * @param model 蓝牙名称
     */
    void disconnectBle(BluetoothDevice mBluetoothDevice, String model);

    /**
     * 断连蓝牙
     * @param sn mac地址
     * @param model 蓝牙名称
     */
    void disconnectBle(String sn, String model);

    /**
     * 向蓝牙写数据
     * @param data 写入数据
     * @param macAddress 蓝牙设备mac地址
     */
    @Deprecated
    void writeBleData(byte[] data, String macAddress);

    /**
     * 写数据 不存放队列
     * @param data 写入数据
     * @param macAddress 蓝牙设备mac地址
     */
    void writeDataNoQueue(byte[] data, String macAddress);

    /**
     * 写数据 存放队列
     * @param data 写入数据
     * @param macAddress 蓝牙设备mac地址
     */
    void writeDataWithQueue(byte[] data, String macAddress);

    /**
     * 写数据 存放队列
     * @param data 写入数据
     * @param macAddress 蓝牙设备mac地址
     */
    void writeDataWithQueue(byte[] data, String macAddress, @TaskPriority int priority);

    /**
     * 向蓝牙写数据,没有通知响应
     * @param data 写入数据
     * @param macAddress 蓝牙设备mac地址
     */
    void writeBleDataWithNoResponse(byte[] data, String macAddress);

    /**
     * 获取扫描结果
     * @return 扫描结果集合
     */
    List<ScanResult> getBluetoothDeviceList();

    /**
     * 获取蓝牙适配器
     * @return 蓝牙适配器
     */
    BluetoothAdapter getBluetoothAdapter();

    /**
     * 修改mtu
     * @param sn mac地址
     */
    void requestMtu(String sn, int mtu);

    /**
     * 修改mtu
     * @param sn mac地址
     */
    void requestMtu(String sn, int mtu, @TaskPriority int priority);

    /**
     * 清空所有蓝牙连接
     */
    void clearAllConnectDevice();

    /**
     * 关闭蓝牙后的清除缓存处理
     */
    void cleanBleRelateData();

    /**
     * @param sn mac地址
     * @return 蓝牙是否连接成功
     */
    boolean isBleConnected(String sn);

    /**
     * @param sn 蓝牙mac地址
     */
    void readRemoteRssi(String sn);

    /**
     * 设置日志开关
     * @param isLogShow 是否打开日志开关
     */
    void setLogSwitch(boolean isLogShow);

    /**
     * 清除连接队列数据
     */
    void clearConnectQueueData();

    /**
     * 设置设备发送命令超时值
     * @param sn 设备sn号
     * @param commandTimeOut 超时值
     */
    void setCommandTimeOut(String sn, int commandTimeOut);

    /**
     * 设置扫描日志开关状态
     * @param isCloseScanLog 是否关闭扫描日志 true:关闭 false:开启 默认开启
     */
    void setCloseScanLog(boolean isCloseScanLog);

    /**
     * 释放资源对象
     */
    void release();

    /**
     * 清空列表数据
     */
    void clearCachaData();

    /**
     * 根据给定的serviceUuid 和 writeUuid写入数据
     *
     * @param data        待写入数据
     * @param macAddress  设备sn
     * @param serviceUuid 服务uuid
     * @param writeUuid   写特征值uuid
     */
    void executeWriteCharacteristicByUuid(byte[] data, String macAddress, String serviceUuid, String writeUuid);

    /**
     * 读指定服务的读特征值
     *
     * @param macAddress  设备sn
     * @param serviceUuid 服务uuid
     * @param readUuid    读特征值Uuid
     */
    void executeReadCharacteristicByUuid(String macAddress, String serviceUuid, String readUuid);

    /**
     * 开启指定uuid的notify通道
     * @param macAddress
     * @param serviceUuid
     * @param notifyUuid
     */
    void setBleNotifyCharacteristicByUuid(String macAddress, String serviceUuid, String notifyUuid);

    /**
     * 发送数据的结果回调
     * @param bleWriteDataListener
     */
    void setOnBleWriteDataListener(BleWriteDataStatueListener bleWriteDataListener);

    /**
     * 设置是否处于ota过程
     * @param sn
     * @param isOnOta
     */
    void setIsOnOta(String sn, boolean isOnOta);
}
