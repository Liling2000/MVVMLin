package com.liling.ble.data;

import java.util.List;
import java.util.UUID;

/**
 * 蓝牙设备封装对象
 * @user liling
 * @Date 2021/2/27
 */
public class BleAdvertisedData {
    private List<UUID> mUuids;
    private String mName;
    public BleAdvertisedData(List<UUID> uuids, String name){
        mUuids = uuids;
        mName = name;
    }
    public List<UUID> getUuids(){
        return mUuids;
    }
    public String getName(){
        return mName;
    }
}
