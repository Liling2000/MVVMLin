package com.liling.ble;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 蓝牙写任务优先级常量设置
 * @user liling
 * @Date 2022/12/5
 */
@IntDef({TaskPriority.DEFAULT_PRIORITY, TaskPriority.LOW_PRIORITY, TaskPriority.HIGH_PRIORITY})
@Retention(RetentionPolicy.RUNTIME)
public @interface TaskPriority {
    int HIGH_PRIORITY = 0; //高优先级任务

    int LOW_PRIORITY = 1; //低优先级任务

    int DEFAULT_PRIORITY = 2; //默认
}