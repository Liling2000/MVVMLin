package com.liling.ble.utils;

import android.bluetooth.BluetoothAdapter;
import android.content.IntentFilter;

import androidx.core.content.ContextCompat;

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
        release();
        //注册广播，蓝牙状态监听
        blueToothStateReceiver = new BlueToothStateReceiver();
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        // Bluetooth can run as a highly privileged system app, so its protected broadcasts
        // require an exported dynamic receiver on modern Android versions.
        ContextCompat.registerReceiver(BleToolManager.getInstance().getContext(), blueToothStateReceiver, filter, ContextCompat.RECEIVER_EXPORTED);
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
            try {
                BleToolManager.getInstance().getContext().unregisterReceiver(blueToothStateReceiver);
            } catch (IllegalArgumentException ignored) {
                // The framework may already have removed this process-scoped receiver.
            }
            blueToothStateReceiver = null;
        }
        mBleStateCallBack = null;
    }
}
