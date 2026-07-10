package com.liling.ble.utils;

import android.bluetooth.BluetoothAdapter;
import android.content.IntentFilter;

import com.liling.ble.listener.BleStateCallBack;
import com.liling.ble.manager.BleToolManager;
import com.liling.ble.receiver.BlueToothStateReceiver;

/**
 * @user liling
 * @Date 2021/2/25
 */
public class BlueToothUtils {

    private static BlueToothUtils INSTANCE;

    public static synchronized BlueToothUtils getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new BlueToothUtils();
        }
        return INSTANCE;
    }

    private BlueToothStateReceiver blueToothStateReceiver;
    private BleStateCallBack mBleStateCallBack;

    //注册广播接收器，用于监听蓝牙状态变化
    public void registerBlueToothStateReceiver(BleStateCallBack bleStateCallBack) {
        //注册广播，蓝牙状态监听
        blueToothStateReceiver = new BlueToothStateReceiver();
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        BleToolManager.getInstance().getContext().registerReceiver(blueToothStateReceiver, filter);
        mBleStateCallBack = bleStateCallBack;
        blueToothStateReceiver.setOnBlueToothStateListener(new BlueToothStateReceiver.OnBlueToothStateListener() {
            @Override
            public void onStateOff() {
                //do something
                if (mBleStateCallBack != null) {
                    bleStateCallBack.stateOff();
                }
            }

            @Override
            public void onStateOn() {
                //do something
                if (mBleStateCallBack != null) {
                    bleStateCallBack.stateOn();
                }
            }

            @Override
            public void onStateTurningOn() {
                //do something
            }

            @Override
            public void onStateTurningOff() {
                //do something
            }
        });
    }

    /**
     * 资源释放
     */
    public void release() {
        if (blueToothStateReceiver != null) {
            blueToothStateReceiver.release();
            BleToolManager.getInstance().getContext().unregisterReceiver(blueToothStateReceiver);
            blueToothStateReceiver = null;
        }
        mBleStateCallBack = null;
    }
}
