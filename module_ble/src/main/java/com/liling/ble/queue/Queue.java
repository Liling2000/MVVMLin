package com.liling.ble.queue;

import com.liling.ble.bean.RequestParam;
import com.liling.ble.bean.Task;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 队列
 * @user liling
 * @Date 2026/1/28
 */
abstract class Queue {

    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    protected boolean isBusy; //标识当前任务执行状态

    protected DelayQueue<Task> delayQueue = new DelayQueue<>();

    protected Queue() {
        executorService.execute(getRunnable());
    }

    public abstract void execute(RequestParam requestParam);

    public abstract Runnable getRunnable();

    /**
     * 数据存放队列
     * @param task 任务对象
     */
    public void put(Task task) {
        delayQueue.put(task);
    }

    /**
     * 队列移除数据
     * @param task 任务
     */
    public void remove(Task task) {
        if (task != null) delayQueue.remove(task);
    }

    /**
     * 清空队列数据
     */
    public void clear() {
        if (delayQueue != null) {
            delayQueue.clear();
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
        isBusy = busy;
        clearData(!isBusy);
    }

    /**
     * 清除数据
     * @param isNeedClear 是否清除标识
     */
    public abstract void clearData(boolean isNeedClear);
}
