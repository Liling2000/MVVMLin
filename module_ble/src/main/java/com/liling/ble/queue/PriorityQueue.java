package com.liling.ble.queue;

import com.liling.ble.bean.RequestParam;
import com.liling.ble.bean.Task;
import com.liling.ble.bean.WriteTask;
import com.liling.ble.utils.LogUtils;
import com.google.gson.Gson;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * 优先级队列
 * @user liling
 */
abstract class PriorityQueue {
    protected final String TAG = "Write_Queue_Log:";

    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    protected boolean isBusy; //标识当前任务执行状态

    protected boolean isInOta = false;

    protected String model; //设备名称
    protected String sn; //设备sn

    protected PriorityBlockingQueue<WriteTask> blockingPriorityQueue = new PriorityBlockingQueue<>();

    protected PriorityQueue(String model, String sn) {
        this.model = model;
        this.sn = sn;
        executorService.execute(getRunnable());
    }

    public abstract void execute(RequestParam requestParam);

    public abstract Runnable getRunnable();

    /**
     * 数据存放队列
     * @param task 任务对象
     */
    public void put(WriteTask task) {
        LogUtils.e(TAG + "---新增任务:" + new Gson().toJson(task) + "---队列ID:" + model + "-" + sn);
        if (blockingPriorityQueue != null && task != null) {
            blockingPriorityQueue.put(task);
        }
        LogUtils.e(TAG + "---队列内容:" + new Gson().toJson(blockingPriorityQueue) + "---队列ID:" + model + "-" + sn);
    }

    /**
     * 队列移除数据
     * @param task 任务
     */
    public void remove(Task task) {
        LogUtils.e(TAG + "---移除任务" + new Gson().toJson(task) + "---队列ID:" + model + "-" + sn);
        if (task != null) blockingPriorityQueue.remove(task);
    }

    /**
     * 清空队列数据
     */
    public void clear() {
        if (blockingPriorityQueue != null) {
            LogUtils.e(TAG + "---清空队列" + "---队列ID:" + model + "-" + sn);
            blockingPriorityQueue.clear();
            blockingPriorityQueue = null;
        }
    }

    /**
     * 停止从队列中读取数据
     */
    public void shutDown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
            executorService = null;
        }
    }

    /**
     * 设置当前任务状态
     * @param busy 连接是否忙碌
     */
    public void setBusy(boolean busy) {
        LogUtils.e(TAG + "---队列阻塞状态:" + busy + "---队列ID:" + model + "-" + sn);
        isBusy = busy;
        clearData(!isBusy);
    }

    public void setIsInOta(boolean isInOta) {
        LogUtils.e(TAG + "---队列阻塞状态 by ota:" + isInOta + "---队列ID:" + model + "-" + sn);
        this.isInOta = isInOta;
    }

    /**
     * 清除数据
     * @param isNeedClear 是否清除标识
     */
    public abstract void clearData(boolean isNeedClear);
}
