package com.liling.ble.presenter;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.text.TextUtils;

import com.liling.ble.callback.OnBletoothCallBack;
import com.liling.ble.manager.BleToolManager;
import com.liling.ble.utils.LogUtils;
import com.liling.ble.utils.Utils;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

/**
 * 蓝牙功能设置
 * @user liling
 * @Date 2021/2/1
 */
public class BletoothImpl implements OnBletoothCallBack {

    private final String TAG = "BletoothImpl";

    @Override
    public void startBle(BluetoothAdapter adapter) {
        if (adapter != null) {
            if (!adapter.isEnabled()) {
                //强制性打开蓝牙
                adapter.enable();
            }
            LogUtils.e(TAG + "-->蓝牙已打开");
        } else {
            LogUtils.e(TAG + "-->设备不支持蓝牙");
        }
    }

    @Override
    public List<ScanFilter> getScanFilters(String deviceName, boolean isSetFilter) {
        List<ScanFilter> filters = new ArrayList<>();
        if (isSetFilter) {
            if (TextUtils.isEmpty(deviceName)) {
                //为空则自行全量扫描
                List<String> deviceNameList = BleToolManager.getInstance().getAllMatchDevices();
                if (deviceNameList != null && !deviceNameList.isEmpty()) {
                    LogUtils.e(TAG + "-->" + new Gson().toJson(deviceNameList));
                    for (String mDeviceName : deviceNameList) {
                        filters.add(new ScanFilter.Builder().setDeviceName(mDeviceName).build());
                    }
                }
//            filters.add(new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(UUID.fromString("53527AA4-29F7-AE11-4E74-997334782568").toString())).build());
//            filters.add(new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(UUID.fromString("EDFEC62E-9910-0BAC-5241-D8BDA6932A2E").toString())).build());
            } else {
                //扫描指定设备
                filters.add(new ScanFilter.Builder().setDeviceName(deviceName).build());
            }
        }
        return filters;
    }

    @Override
    public ScanSettings getScanSettings(Context context, BluetoothAdapter adapter) {
        //设置低功耗模式
        ScanSettings.Builder builder;
        if (Utils.isBackground(context)) {
            //app在后台 扫描模式设置低功耗
            builder = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_POWER);
        } else {
            //app在前端 扫描模式设置低延迟(高效)
            builder = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
        }
        //android 6.0添加设置回调类型、匹配模式等
        if(android.os.Build.VERSION.SDK_INT >= 23) {
            //定义回调类型
            builder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES);
            //设置蓝牙LE扫描滤波器硬件匹配的匹配模式
            builder.setMatchMode(ScanSettings.MATCH_MODE_STICKY);
        }
        //芯片组支持批处理芯片上的扫描
        if (adapter.isOffloadedScanBatchingSupported()) {
            //设置蓝牙LE扫描的报告延迟的时间（以毫秒为单位）
            //设置为0以立即通知结果
            builder.setReportDelay(0L);
        }
        return builder.build();
    }

    @Override
    public boolean isTheEquipmentValid(String deviceName) {
        List<String> deviceNameList = BleToolManager.getInstance().getAllSupportDevices();
        if (deviceNameList != null && !deviceNameList.isEmpty()
                && deviceNameList.contains(deviceName)) {
            return true;
        }
        return false;
    }
}
