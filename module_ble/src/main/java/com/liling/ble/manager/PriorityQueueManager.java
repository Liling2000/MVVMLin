package com.liling.ble.manager;

import android.util.ArrayMap;

import com.liling.ble.bean.RequestParam;
import com.liling.ble.bean.WriteTask;
import com.liling.ble.queue.WritePriorityQueue;
import com.liling.ble.utils.LogUtils;
import com.orhanobut.logger.Logger;

import java.util.Map;

/**
 * @user liling
 * @Date 2022/12/2
 */
public class PriorityQueueManager {

    private final String TAG = PriorityQueueManager.class.getSimpleName();

    private Map<String, WritePriorityQueue> queueMap = new ArrayMap<>();

    private static PriorityQueueManager instance = new PriorityQueueManager();

    public static PriorityQueueManager getInstance() {
        return instance;
    }

    /**
     * 创建队列
     * @param model 设备名称
     * @param sn 设备sn号
     * @param commandTimeOut 命令发送延时时间
     * @return map对象
     */
    private void createQueue(String model, String sn, long commandTimeOut) {
        synchronized (queueMap) {
            if (queueMap != null && !queueMap.containsKey(sn)) {
                WritePriorityQueue writePriorityQueue = new WritePriorityQueue(model, sn);
                queueMap.put(sn, writePriorityQueue);
                LogUtils.e("create_write_queue");
            }
        }
    }

    /**
     * 删除队列
     * @param sn 设备sn号
     */
    public void deleteQueue(String sn) {
        synchronized (queueMap) {
            if (queueMap != null && queueMap.containsKey(sn)) {
                queueMap.get(sn).clear();
                queueMap.remove(sn);
                LogUtils.e("delete_write_queue");
            }
        }
    }

    /**
     * 操作队列
     * @param sn 设备sn号
     * @param data 操作数据
     * @param commandOutTime 命令发送延时
     */
    public void addDataToQueue(String sn, WriteTask data, long commandOutTime) {
        if (Ble.getBleApi().isBleConnected(sn)) {
            createQueue(data.getRequestParam().getModel(), sn, commandOutTime);
            WritePriorityQueue writePriorityQueue = queueMap.get(sn);
            if (writePriorityQueue != null) {
                writePriorityQueue.setCommandTimeOut(commandOutTime);
                writePriorityQueue.put(data);
            }
        } else {
            Logger.e(TAG + "[method:addDataToQueue]:该设备未连接，数据无法发送");
        }
    }

    /**
     * 获取当前设备对应队列所在的当前任务的参数
     * @param sn 设备sn号
     * @return 任务参数
     */
    public RequestParam getRequestParamBySn(String sn) {
        WritePriorityQueue writePriorityQueue = getWriteQueue(sn);
        if (writePriorityQueue != null) {
            return writePriorityQueue.getCurrentRequestParam();
        }
        return new RequestParam();
    }

    /**
     * 根据sn获取对应任务队列
     * @param sn 设备sn号
     * @return 任务队列
     */
    private WritePriorityQueue getWriteQueue(String sn) {
        if (queueMap != null && !queueMap.isEmpty() && queueMap.containsKey(sn)) {
            return queueMap.get(sn);
        }
        return null;
    }

    /**
     * 设置队列运行状态
     * @param sn 设备sn号
     * @param isBusy 是否正在处理任务
     */
    public void setQueueRunningState(String sn, boolean isBusy) {
        WritePriorityQueue writePriorityQueue = getWriteQueue(sn);
        if (writePriorityQueue != null) {
            writePriorityQueue.cancelDisposable();
            writePriorityQueue.setBusy(isBusy);
        }

    }

    public void setOtaState(String sn, boolean isInOta) {
        WritePriorityQueue writePriorityQueue = getWriteQueue(sn);
        if (writePriorityQueue != null) {
            writePriorityQueue.setIsInOta(isInOta);
        }
    }

    /**
     * 清空所有队列
     */
    public void clear() {
        if (queueMap != null && !queueMap.isEmpty()) {
            queueMap.clear();
        }
    }

}
