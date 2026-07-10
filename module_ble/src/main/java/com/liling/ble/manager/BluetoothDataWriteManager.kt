package com.liling.ble.manager

import android.text.TextUtils
import com.liling.ble.utils.BleUtils
import com.orhanobut.logger.Logger

/**
 * @date 2026/6/22
 * @description 蓝牙设备写数据管理类
 */
class BluetoothDataWriteManager {
    companion object {
        /**
         * 蓝牙设备写数据
         * @param sn 设备mac地址
         * @param sourceData 源数据
         */
        fun writeData(sn: String, sourceData: String) {
            Logger.e("BluetoothDataWriteManager writeData sn = $sn, sourceData = $sourceData")
            if (TextUtils.isEmpty(sourceData) || TextUtils.isEmpty(sn)) {
                return
            }
            if (!Ble.getBleApi().isBleOpen) {
                Logger.e("BluetoothDataWriteManager Ble is close")
                return
            }

            Ble.getBleApi().writeDataWithQueue(BleUtils.stringToBytes(sourceData), sn)
        }


        /**
         * 蓝牙设备写数据
         * @param sn 设备mac地址
         * @param sourceData 源数据
         */
        fun writeDataNoQueue(sn: String, sourceData: String) {
            Logger.e("BluetoothDataWriteManager writeDataNoQueue sn = $sn, sourceData = $sourceData")
            if (TextUtils.isEmpty(sourceData) || TextUtils.isEmpty(sn)) {
                return
            }
            if (!Ble.getBleApi().isBleOpen) {
                Logger.e("BluetoothDataWriteManager Ble is close")
                return
            }
            Ble.getBleApi().writeDataNoQueue(BleUtils.stringToBytes(sourceData), sn)
        }
    }
}