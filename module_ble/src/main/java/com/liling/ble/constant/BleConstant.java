package com.liling.ble.constant;

/**
 * 蓝牙相关常量配置类
 *
 * @user liling
 * @Date 2021/1/26
 */
public class BleConstant {


    public interface publicParams {
        //对外广播action 蓝牙BluetoothConnect权限缺失通知
        String BLUETOOTH_CONNECT_PERMISSION_LOST = "bluetooth_connect_permission_lost";
    }

    /**
     * 蓝牙连接状态
     */
    public interface BleConnectState {
        //断连
        int stateDisconnected = 0;
        //正在连接
        int stateConnecting = 1;
        //已连接
        int stateConnected = 2;
        //正在断连
        int stateDisconnecting = 3;
    }

    /**
     * 蓝牙命令延时处理时间
     */
    public interface BleRelateTime {
        //发送命令延时
        int delayTime = 200;
        int DELAY_TIME_500 = 500;
        //扫描超时
        int scanTimeOut = 10 * 1000;
        //连接超时
        int CONNECT_TIMEOUT = 6000;
        //写数据超时
        int WRITE_TIMEOUT = 500;
    }
}
