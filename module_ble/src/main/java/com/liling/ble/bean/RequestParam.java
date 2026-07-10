package com.liling.ble.bean;

import android.bluetooth.BluetoothDevice;
import android.text.TextUtils;

import com.liling.ble.utils.BleUtils;

/**
 * @user liling
 * @Date 2026/1/28
 */
public class RequestParam {

    private String address = ""; //设备mac地址

    private byte[] data; //发送数据

    public String dataStr = ""; //发送数据 16进制指令

    private BluetoothDevice device; //连接设备

    private String model = ""; //设备蓝牙名称

    private int taskFlag; //默认为0  0:正常写数据 1:mtu申请 2：rssi信号值读取

    private int mtu; //mtu值

    private String requestPrefix = ""; //app请求命令前缀

    private String responsePrefix = ""; //设备回复命令前缀

    public RequestParam () {

    }

    RequestParam(String address, byte[] data,String dataStr, BluetoothDevice device, String model, int taskFlag
            , int mtu, String requestPrefix, String responsePrefix) {
        this.address = address;
        this.data = data;
        this.dataStr = dataStr;
        this.device = device;
        this.model = model;
        this.taskFlag = taskFlag;
        this.mtu = mtu;
        this.requestPrefix = requestPrefix;
        this.responsePrefix = responsePrefix;
    }

    public static class Builder {
        private String address = "";
        private byte[] data;
        private String dataStr = "";
        private BluetoothDevice device;
        private String model = "";
        private int taskFlag;
        private int mtu;
        private String requestPrefix = "";
        private String responsePrefix = "";

        public Builder address(String address) {
            this.address = address;
            return this;
        }

        public Builder data(byte[] data) {
            this.data = data;
            if (data != null) {
                String strData = BleUtils.byteToString(data);
                dataStr = strData;
                if (!TextUtils.isEmpty(strData) && strData.length() >= 4 && "BA".equalsIgnoreCase(strData.substring(0, 2))) {
                    StringBuilder sb = new StringBuilder();
                    this.responsePrefix
                            = sb.append("AA").append(strData.substring(2, 4)).toString();
                }
            }
            return this;
        }

        public Builder device(BluetoothDevice device){
            this.device = device;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder taskFlag(int taskFlag) {
            this.taskFlag = taskFlag;
            return this;
        }

        public Builder mtu(int mtu) {
            this.mtu = mtu;
            return this;
        }

        public RequestParam build() {
            return new RequestParam(address, data, dataStr, device, model, taskFlag, mtu, requestPrefix
                    , responsePrefix);
        }
    }

    public String getAddress() {
        return address;
    }

    public byte[] getData() {
        return data;
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    public String getModel() {
        return model;
    }

    public int getTaskFlag() {
        return taskFlag;
    }

    public int getMtu() {
        return mtu;
    }

    public String getResponsePrefix() {
        return responsePrefix;
    }
}
