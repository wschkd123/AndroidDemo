package com.example.beyond.demo.base

import android.app.Activity
import android.app.Application
import android.os.Bundle

/**
 * Activity 生命周期回调
 * 可选择性重写其中方法
 *
 * Created by fengkeke on 2022/4/26
 */

open class SimpleActivityLifecycleCallback : Application.ActivityLifecycleCallbacks {
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
    }

    override fun onActivityStarted(activity: Activity) {
    }

    override fun onActivityResumed(activity: Activity) {
    }

    override fun onActivityPaused(activity: Activity) {
    }

    override fun onActivityStopped(activity: Activity) {
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    override fun onActivityDestroyed(activity: Activity) {
    }

}