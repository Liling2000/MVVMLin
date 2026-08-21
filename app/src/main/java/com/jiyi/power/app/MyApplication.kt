package com.jiyi.power.app

import android.app.Activity
import android.os.Bundle
import com.alibaba.android.arouter.launcher.ARouter
import com.aleyn.mvvm.app.MVVMLin
import com.aleyn.mvvm.base.BaseApplication
import com.aleyn.mvvm.utils.MmkvManager
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.jiyi.power.BuildConfig
import com.jiyi.power.app.ble.BleConnectionCoordinator
import com.scwang.smart.refresh.header.ClassicsHeader
import com.scwang.smart.refresh.layout.SmartRefreshLayout
import kotlinx.coroutines.CoroutineExceptionHandler

/**
 *   @author : Aleyn
 *   time   : 2019/11/04
 */
class MyApplication : BaseApplication() {

    companion object {
        init {
            SmartRefreshLayout.setDefaultRefreshHeaderCreator { context, _ ->
                ClassicsHeader(context)
            }
        }
    }


    override fun onCreate() {
        super.onCreate()
        MmkvManager.init(this)
        BleConnectionCoordinator.initialize(this)
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            private var startedActivities = 0

            override fun onActivityStarted(activity: Activity) {
                if (startedActivities++ == 0) BleConnectionCoordinator.onAppForeground()
            }

            override fun onActivityStopped(activity: Activity) {
                startedActivities = (startedActivities - 1).coerceAtLeast(0)
                if (startedActivities == 0) BleConnectionCoordinator.onAppBackground()
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })

        if (BuildConfig.DEBUG) {
            ARouter.openLog()
            ARouter.openDebug()
        }
        ARouter.init(this)

        //可选
        MVVMLin.setNetException(CoroutineExceptionHandler { context, e ->
             ToastUtils.showShort(e.message)
        })

        LogUtils.getConfig().run {
            isLogSwitch = BuildConfig.DEBUG
            setSingleTagSwitch(true)
        }
    }
}
