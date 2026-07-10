package com.liling.ble.listener;

/**
 * 数据发送结果回调
 */
public interface BleWriteDataStatueListener {

    /**
     * 结果回调
     *
     * @param sn     设备sn
     * @param statue 发送结果,成功:true/失败:false
     */
    void onWriteDataStatue(String sn, boolean statue, byte [] data);
}
