package com.liling.ble.queue;

import com.liling.ble.bean.RequestParam;
import com.liling.ble.bean.Task;
import com.liling.ble.manager.BleToolManager;
import com.liling.ble.utils.LogUtils;

/**
 * @user liling
 * @Date 2026/1/28
 */
public class ConnectQueue extends Queue{

    private static volatile ConnectQueue instance;

    private Task curTask; //当前任务对象

    ConnectQueue() {
        super();
    }

    public static ConnectQueue getInstance() {
        if (instance != null) {
            return instance;
        }
        synchronized (ConnectQueue.class) {
            if (instance == null) {
                instance = new ConnectQueue();
            }
        }
        return instance;
    }

    @Override
    public void execute(RequestParam requestParam) {
        //执行ble连接
        BleToolManager.getInstance().connect(requestParam.getDevice(), requestParam.getModel());
    }

    @Override
    public Runnable getRunnable() {
        return () -> {
            while (true) {
                try {
                    Thread.sleep(1);
                    if (!isBusy && delayQueue != null) {
                        LogUtils.e("connect_queue_size:" + delayQueue.size());
                        curTask = delayQueue.take();
                        if (curTask != null) {
                            RequestParam requestParam = curTask.getRequestParam();
                            if (requestParam != null) {
                                isBusy = true;
                                execute(requestParam);
                            }
                        }
                    }
                } catch (Exception e) {
                    LogUtils.e("connect queue exception-->" + e.getMessage());
                }
            }
        };
    }

    public void clear() {
        super.clear();
        curTask = null;
        isBusy = false;
    }

    @Override
    public void clearData(boolean isNeedClear) {

    }
}
