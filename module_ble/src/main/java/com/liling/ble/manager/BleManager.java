package com.liling.ble.manager;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.content.Context;

import com.liling.ble.api.BleApi;
import com.liling.ble.callback.BleScanDeviceCallBack;
import com.liling.ble.listener.BleDataListener;
import com.liling.ble.listener.BleWriteDataStatueListener;
import com.liling.ble.queue.ConnectQueue;
import com.liling.ble.utils.LogUtils;

import java.util.List;
import java.util.Map;

/**
 * 蓝牙操作类
 * @user liling
 * @Date 2021/3/2
 */
public class BleManager implements BleApi {

    @Override
    public void init(Context context) {
        BleToolManager.getInstance().init(context);
    }

    @Override
    public void setOnBleDataListener(BleDataListener bleDataListener) {
        BleToolManager.getInstance().setOnBleDataListener(bleDataListener);
    }

    @Override
    public void setOnBleScanResultCallBack(BleScanDeviceCallBack scanResultCallBack) {
        BleToolManager.getInstance().setOnBleScanResultCallBack(scanResultCallBack);
    }

    @Override
    public boolean isBleOpen() {
        return BleToolManager.getInstance().isBleOpen();
    }

    @Override
    public void startBle() {
        BleToolManager.getInstance().startBle();
    }

    @Override
    public void scanDevice() {
        BleToolManager.getInstance().scanDevice();
    }

    @Override
    public void scanDevice(String deviceName) {
        BleToolManager.getInstance().scanDevice(deviceName, true);
    }

    @Override
    public void scanDevice(boolean isSetFilter) {
        BleToolManager.getInstance().scanDevice(isSetFilter);
    }

    @Override
    public void stopScanDelay(boolean isDelay) {
        BleToolManager.getInstance().stopScanDelay(isDelay);
    }

    @Deprecated
    @Override
    public void connectBleDevice(BluetoothDevice mBluetoothDevice, String model) {
        BleToolManager.getInstance().connectBleDevice(mBluetoothDevice, model);
    }

    @Deprecated
    @Override
    public void connectBleDeviceBySn(String sn, String model) {
        BleToolManager.getInstance().connectBleDeviceBySn(sn, model);
    }

    @Override
    public void connectNoQueue(BluetoothDevice mBluetoothDevice, String model) {
        BleToolManager.getInstance().connectNoQueue(mBluetoothDevice, model);
    }

    @Override
    public void connectWithQueue(BluetoothDevice mBluetoothDevice, String model) {
        BleToolManager.getInstance().connectWithQueue(mBluetoothDevice, model);
    }

    @Deprecated
    @Override
    public void disconnectBle(BluetoothDevice mBluetoothDevice, String model) {
        BleToolManager.getInstance().disconnectBle(mBluetoothDevice, model);
    }

    @Override
    public void disconnectBle(String sn, String model) {
        BleToolManager.getInstance().disconnectBle(sn, model);
    }

    @Deprecated
    @Override
    public void writeBleData(byte[] data, String macAddress) {
        BleToolManager.getInstance().writeBleData(data, macAddress);
    }

    @Override
    public void writeDataNoQueue(byte[] data, String macAddress) {
        BleToolManager.getInstance().writeDataNoQueue(data, macAddress);
    }

    @Override
    public void writeDataWithQueue(byte[] data, String macAddress) {
        BleToolManager.getInstance().writeDataWithQueue(data, macAddress);
    }

    @Override
    public void writeDataWithQueue(byte[] data, String macAddress, int priority) {
        BleToolManager.getInstance().writeDataWithQueue(data, macAddress, priority);
    }

    @Override
    public void writeBleDataWithNoResponse(byte[] data, String macAddress) {
        BleToolManager.getInstance().writeBleDataWithNoResponse(data, macAddress);
    }

    @Override
    public List<ScanResult> getBluetoothDeviceList() {
        return BleToolManager.getInstance().getBluetoothDeviceList();
    }

    @Override
    public BluetoothAdapter getBluetoothAdapter() {
        return BleToolManager.getInstance().getBluetoothAdapter();
    }

    @Override
    public void requestMtu(String sn, int mtu) {
        BleToolManager.getInstance().requestMtu(sn, mtu);
    }

    @Override
    public void requestMtu(String sn, int mtu, int priority) {
        BleToolManager.getInstance().requstMtu(sn, mtu, priority);
    }

    @Override
    public void clearAllConnectDevice() {
        BleToolManager.getInstance().clearAllConnectDevice();
    }

    @Override
    public void cleanBleRelateData() {
        BleToolManager.getInstance().cleanBleRelateData();
    }

    @Override
    public boolean isBleConnected(String sn) {
        return BleToolManager.getInstance().getConnectedState(sn);
    }

    @Override
    public void readRemoteRssi(String sn) {
        BleToolManager.getInstance().readRemoteRssi(sn);
    }

    @Override
    public void setLogSwitch(boolean isLogShow) {
        LogUtils.setIsShowLog(isLogShow);
    }

    @Override
    public void clearConnectQueueData() {
        ConnectQueue.getInstance().clear();
    }

    @Override
    public void setCommandTimeOut(String sn, int commandTimeOut) {
        BleToolManager.getInstance().setCommandTimeOut(sn, commandTimeOut);
    }

    @Override
    public void setCloseScanLog(boolean isCloseScanLog) {
        BleToolManager.getInstance().setCloseScanLog(isCloseScanLog);
    }

    @Override
    public void release() {
        BleToolManager.getInstance().release();
    }

    @Override
    public void clearCachaData() {
        BleToolManager.getInstance().clearCacheData();
    }

    @Override
    public void executeWriteCharacteristicByUuid(byte[] data, String macAddress, String serviceUuid, String writeUuid) {
        BleToolManager.getInstance().executeWriteCharacteristicByUuid(data, macAddress, serviceUuid, writeUuid);
    }

    @Override
    public void executeReadCharacteristicByUuid(String macAddress, String serviceUuid, String readUuid) {
        BleToolManager.getInstance().executeReadCharacteristicByUuid(macAddress, serviceUuid, readUuid);
    }

    @Override
    public void setBleNotifyCharacteristicByUuid(String macAddress, String serviceUuid, String notifyUuid) {
        BleToolManager.getInstance().setBleNotifyCharacteristicByUuid(macAddress,serviceUuid,notifyUuid);
    }

    @Override
    public void setOnBleWriteDataListener(BleWriteDataStatueListener bleWriteDataListener) {
        BleToolManager.getInstance().setOnBleWriteDataListener(bleWriteDataListener);
    }

    @Override
    public void setIsOnOta(String sn, boolean isOnOta) {
        BleToolManager.getInstance().setIsInOta(sn, isOnOta);
    }

    @Override
    public void setAllMatchDevices(List<String> filterDeviceList) {
        BleToolManager.getInstance().setAllMatchDevices(filterDeviceList);
    }

    @Override
    public List<String> getAllMatchDevices() {
        return BleToolManager.getInstance().getAllMatchDevices();
    }

    @Override
    public void setDeviceAllUuid(Map<String, Map<String, String>> deviceAllUuidMap) {
        BleToolManager.getInstance().setDeviceUuidMap(deviceAllUuidMap);
    }

    @Override
    public void setAllSupportDevices(List<String> allSupportDevices) {
        BleToolManager.getInstance().setAllSupportDevices(allSupportDevices);
    }

    @Override
    public List<String> getAllSupportDevices() {
        return BleToolManager.getInstance().getAllSupportDevices();
    }

    @Override
    public void setCompatibleUuidDeviceList(List<String> compatibleUuidDeviceList) {
        BleToolManager.getInstance().setCompatibleUuidDeviceList(compatibleUuidDeviceList);
    }

    @Override
    public void deleteWriteQueueByDeviceSn(String sn) {
        PriorityQueueManager.getInstance().deleteQueue(sn);
    }
}
