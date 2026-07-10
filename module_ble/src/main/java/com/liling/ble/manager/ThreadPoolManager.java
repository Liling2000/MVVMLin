package com.liling.ble.manager;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 线程池
 * @user liling
 * @Date 2021/1/31
 */
public class ThreadPoolManager {

    private static ThreadPoolManager intance = new ThreadPoolManager();
    private int corePoolSize; //核心线程数量
    private ExecutorService executorFixed;
    private ExecutorService executorSingle;
    private Executor mainThreadExecutor;

    public static ThreadPoolManager getInstance() {
        return intance;
    }


    private ThreadPoolManager() {
        //corePoolSize为当前设备可用处理器核心数*2+1，cpu效率能最大程度执行
        corePoolSize = Runtime.getRuntime().availableProcessors() * 2 + 1;
        executorFixed = Executors.newFixedThreadPool(corePoolSize);
        mainThreadExecutor = new MainThreadExecutor();
        executorSingle = Executors.newSingleThreadExecutor();
    }

    /**
     * 子线程执行任务
     */
    public void execute(Runnable runnable) {
        if (runnable == null) {
            return;
        }
        executorFixed.execute(runnable);
    }

    /**
     * 单任务线程池
     */
    public void executeSingleThread(Runnable runnable) {
        if (runnable == null) {
            return;
        }
        executorSingle.execute(runnable);
    }

    /**
     * 主线程执行任务
     */
    public void executeOnMainThread(Runnable runnable) {
        mainThreadExecutor.execute(runnable);
    }

    /**
     * 主线程执行器
     */
    private static class MainThreadExecutor implements Executor {
        private Handler mainThreadHandler = new Handler(Looper.getMainLooper());
        @Override
        public void execute(Runnable command) {
            mainThreadHandler.post(command);
        }
    }
}

