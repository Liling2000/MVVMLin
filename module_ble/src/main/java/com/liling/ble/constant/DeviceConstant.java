package com.liling.ble.constant;

/**
 * @user liling
 * @Date 2022/1/21
 * 设备相关常量类
 */
public class DeviceConstant {
    /**
     * UUID存储对应的key
     */
    public interface BleUuidKey {

        String SERVICE_UUID_KEY = "service_uuid"; //服务存储key

        String WRITE_CHARACTERRISTIC_UUID_KEY = "write_characterristic_uuid"; //写特征存储key

        String NOTIFY_CHARACTERRISTIC_UUID_KEY = "notify_characterristic_uuid"; //通知特征存储key
    }

    /**
     * 写数据任务类型
     */
    public interface TaskFlag{

        int NORMAL_WRITE = 0; //正常数据读写

        int MTU_WRITE = 1; //mtu申请写

        int RSSI_WRITE = 2; //rssi信号值写
    }
}
