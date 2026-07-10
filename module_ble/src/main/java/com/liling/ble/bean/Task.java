package com.liling.ble.bean;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 延迟任务
 * @user liling
 * @Date 2026/1/28
 */
public class Task implements Delayed {

    private long delayTime = 0; //延迟时间 默认200ms

    private long expireTime; //到期时间

    private AtomicLong atomic = new AtomicLong(0); //产生序列号

    private long serializableNum; //序列号

    private RequestParam requestParam;

    /**
     * 构造器
     * @param delayTime 任务延迟时间
     * @param requestParam 任务包裹数据
     */
    public Task(long delayTime, RequestParam requestParam) {
        this.requestParam = requestParam;
        this.delayTime = delayTime;
        this.expireTime = System.currentTimeMillis()
                + TimeUnit.MILLISECONDS.convert(delayTime, TimeUnit.MILLISECONDS);
        this.serializableNum = atomic.getAndIncrement();
    }

    /**
     * 构造器
     * @param requestParam 任务包裹数据
     */
    public Task(RequestParam requestParam) {
        this.requestParam = requestParam;
        this.expireTime = System.currentTimeMillis()
                + TimeUnit.MILLISECONDS.convert(delayTime, TimeUnit.MILLISECONDS);
        this.serializableNum = atomic.getAndIncrement();
    }

    /**
     * 返回任务剩余延迟时间
     */
    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(this.expireTime - System.currentTimeMillis()
                , TimeUnit.MILLISECONDS);
    }

    /**
     * 元素排序
     */
    @Override
    public int compareTo(Delayed compareDelay) {
        if (compareDelay == this) {
            return 0;
        }
        if (compareDelay instanceof Task) {
            Task compareTask = (Task) compareDelay;
            long diffTime = expireTime - compareTask.expireTime;
            if (diffTime < 0)
                return -1;
            else if (diffTime > 0)
                return 1;
            else  if (serializableNum < compareTask.serializableNum)
                return -1;
            else
                return 1;

        }
        long d = getDelay(TimeUnit.MILLISECONDS) - compareDelay.getDelay(TimeUnit.MILLISECONDS);
        return (d == 0) ? 0 : ((d < 0) ? -1 : 1);
    }

    public long getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(long expireTime) {
        this.expireTime = expireTime;
    }

    public long getDelayTime() {
        return delayTime;
    }

    public void setDelayTime(long delayTime) {
        this.delayTime = delayTime;
    }

    public RequestParam getRequestParam() {
        return requestParam;
    }

    public void setRequestParam(RequestParam requestParam) {
        this.requestParam = requestParam;
    }
}
