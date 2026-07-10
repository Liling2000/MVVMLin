package com.liling.ble.bean;

/**
 * @user liling
 * @Date 2022/12/2
 */
public class WriteTask implements Comparable<WriteTask> {

    private int priorityLevel; //优先级值 值越小 越排序靠前

    private RequestParam requestParam; //数据体对象

    //时间戳 用于比较优先级相同情况下 时间戳越小越排在队列前面（满足先进先出）
    private long timeStamp = System.currentTimeMillis();

    public int getPriorityLevel() {
        return priorityLevel;
    }

    public void setPriorityLevel(int priorityLevel) {
        this.priorityLevel = priorityLevel;
    }

    public RequestParam getRequestParam() {
        return requestParam;
    }

    public void setRequestParam(RequestParam requestParam) {
        this.requestParam = requestParam;
    }

    @Override
    public int compareTo(WriteTask o) {
        if (this.priorityLevel == o.priorityLevel) {
            //优先级相同，比较时间戳，时间戳越大的排在队列后面
            return (int) (this.timeStamp - o.timeStamp);
        }
        return this.priorityLevel - o.priorityLevel;
    }
}
