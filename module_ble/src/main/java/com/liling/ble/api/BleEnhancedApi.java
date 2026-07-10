package com.liling.ble.api;

import java.util.List;
import java.util.Map;

/**
 * 库优化新增相关接口
 * @user liling
 * @Date 2022/3/29
 */
public interface BleEnhancedApi {

    /**
     * 设置过滤设备集合
     * @param allMatchDevices 匹配设备列表
     */
    void setAllMatchDevices(List<String> allMatchDevices);

    /**
     * 获取设备过滤列表
     * @return 过滤列表
     */
    List<String> getAllMatchDevices();

    /**
     * 设置设备所需的service、write、notify的UUID,按设备分类存放在map中
     * @param deviceAllUuidMap uuid的map对象
     */
    void setDeviceAllUuid(Map<String, Map<String, String>> deviceAllUuidMap);

    /**
     * 设置所有支持的设备
     * @param allSupportDevices 所有支持的设备
     */
    void setAllSupportDevices(List<String> allSupportDevices);

    /**
     * 获取所有支持的设备列表
     * @return 所有支持的设备列表
     */
    List<String> getAllSupportDevices();

    /**
     * 设置需匹配UUID的设备列表
     * @param compatibleUuidDeviceList 需匹配UUID的设备列表
     */
    void setCompatibleUuidDeviceList(List<String> compatibleUuidDeviceList);

    /**
     * 删除设备所对应的写队列
     * @param sn 设备mac地址
     */
    void deleteWriteQueueByDeviceSn(String sn);
}
