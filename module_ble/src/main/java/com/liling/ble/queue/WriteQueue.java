package com.liling.ble.queue;

import com.liling.ble.bean.RequestParam;
import com.liling.ble.bean.Task;
import com.liling.ble.manager.BleToolManager;
import com.liling.ble.utils.LogUtils;

import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;

import static com.liling.ble.constant.BleConstant.BleRelateTime.WRITE_TIMEOUT;
import static com.liling.ble.constant.DeviceConstant.TaskFlag.MTU_WRITE;

/**
 * 写队列
 * @user liling
 * @Date 2026/1/28
 */
public class WriteQueue extends Queue{

    private static volatile WriteQueue instance;

    private WriteQueue() {
        super();
    }

    private Disposable disposable;

    private RequestParam currentRequestParam = null; //当前执行任务对象

    public static WriteQueue getInstance() {
        if (instance != null) {
            return instance;
        }
        synchronized (WriteQueue.class) {
            if (instance == null) {
                instance = new WriteQueue();
            }
        }
        return instance;
    }

    @Override
    public void execute(RequestParam requestParam) {
        //执行ble写任务
        if (requestParam.getTaskFlag() == MTU_WRITE) {
            //mtu申请
            BleToolManager.getInstance().excuteMtuTask(requestParam.getAddress()
                    , requestParam.getMtu());
        } else {
            BleToolManager.getInstance().executeWriteCharacteristic(requestParam.getData()
                    , requestParam.getAddress());
        }
        disposable  = Observable.timer(WRITE_TIMEOUT, TimeUnit.MILLISECONDS)
                .subscribe(aLong -> {
                    //数据发送500ms未响应，标识更新，取栈中下一条数据
                    WriteQueue.getInstance().setBusy(false);
                });
    }

    /**
     * 取消超时任务
     */
    public void cancelDisposable() {
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
    }

    /**
     * 获取当前写任务对象
     * @return 当前对象
     */
    public RequestParam getCurrentRequestParam() {
        return currentRequestParam;
    }

    @Override
    public void clearData(boolean isNeedClear) {
        if (isNeedClear) {
            currentRequestParam = null;
        }
    }

    @Override
    public Runnable getRunnable() {
        return () -> {
            while (true) {
                try {
                    Thread.sleep(1);
                    if (!isBusy) {
                        LogUtils.e("write_queue_size:" + delayQueue.size());
                        Task task = delayQueue.take();
                        if (task != null) {
                            RequestParam requestParam = task.getRequestParam();
                            //队列取出新任务后重置当前任务对象为null
                            currentRequestParam = null;
                            if (requestParam != null) {
                                isBusy = true;
//                                if (!delayQueue.isEmpty()) {
//                                    //保持队列中每两个任务之间相差100ms的时间间隔
//                                    Thread.sleep(100);
//                                }
                                currentRequestParam = requestParam;
                                execute(requestParam);
                            }
                        }
                    }
                } catch (Exception e) {
                    LogUtils.e("queue take data-->" + e.getMessage());
                }
            }
        };
    }

}
