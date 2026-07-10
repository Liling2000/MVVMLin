package com.liling.ble.queue;

import com.liling.ble.bean.RequestParam;
import com.liling.ble.bean.WriteTask;
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
public class WritePriorityQueue extends PriorityQueue {
    private long commandTimeOut = WRITE_TIMEOUT; //命令发送超时时间

    /**
     * @param sn 设备sn
     * @param model 设备蓝牙名称
     */
    public WritePriorityQueue(String model, String sn) {
        super(model, sn);
    }

    private Disposable disposable;

    private RequestParam currentRequestParam = null; //当前执行任务对象

    @Override
    public void execute(RequestParam requestParam) {
        //执行ble写任务
        if (requestParam.getTaskFlag() == MTU_WRITE) {
            //mtu申请
            BleToolManager.getInstance().excuteMtuTask(requestParam.getAddress()
                    , requestParam.getMtu());
            LogUtils.e(TAG + "---执行任务[mtu申请]" + "---队列ID:" + model + "-" + sn);
        } else {
            BleToolManager.getInstance().executeWriteCharacteristic(requestParam.getData()
                    , requestParam.getAddress());
            LogUtils.e(TAG + "---执行任务[ble命令]" + "---队列ID:" + model + "-" + sn);
        }
        LogUtils.e("队列:" + model + "&" + sn + "---任务超时设置时间:" + commandTimeOut);
        disposable  = Observable.timer(commandTimeOut, TimeUnit.MILLISECONDS)
                .subscribe(aLong -> {
                    //数据发送500ms未响应，标识更新，取栈中下一条数据
                    super.setBusy(false);
                    LogUtils.e(TAG + "---任务超时,执行下一项,当前队列ID:" + model + "-" + sn);
                });
    }

    /**
     * 设置超时时间
     * @param commandTimeOut 超时时长
     */
    public void setCommandTimeOut(long commandTimeOut) {
        this.commandTimeOut = commandTimeOut;
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
                    if (!isInOta && !isBusy && blockingPriorityQueue != null) {
                        WriteTask task = blockingPriorityQueue.take();
                        if (task != null) {
                            RequestParam requestParam = task.getRequestParam();
                            LogUtils.e(TAG + "---当前任务优先级:" + task.getPriorityLevel() + "---队列ID:" + model + "-" + sn);
                            //队列取出新任务后重置当前任务对象为null
                            currentRequestParam = null;
                            if (requestParam != null) {
                                isBusy = true;
                                currentRequestParam = requestParam;
                                execute(requestParam);
                            }
                        }
                    }
                } catch (Exception e) {
                    LogUtils.e(TAG + "---队列运行异常" + "---队列ID:" + model + "-" + sn + "-->" + e.getMessage());
                }
            }
        };
    }
}
