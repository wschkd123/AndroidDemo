package com.example.base

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlin.system.exitProcess

/**
 *
 * Created by fengkeke on 2023/9/3
 */
object AppContext {
    const val TAG = "AppContext"
    val isDebug: Boolean = true
    lateinit var application: Application

    private val activityStack: MutableList<Activity> = mutableListOf()
    val resumedActivity: Activity? get() = activityStack.lastOrNull()

    /**
     * 是否在前台
     */
    var isForeground: Boolean = true
        get() {
            return field
        }
        private set(value) {
            field = value
        }

    fun init(application: Application) {
        AppContext.application = application
        application.registerActivityLifecycleCallbacks(object : SimpleActivityLifecycleCallback() {

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                super.onActivityCreated(activity, savedInstanceState)
                Log.i(
                    TAG,
                    "[ActivityLifecycle] onActivityCreated: ${activity.javaClass.simpleName}"
                )
                activityStack.add(activity)
            }

            override fun onActivityStarted(activity: Activity) {
                super.onActivityStarted(activity)
                Log.i(
                    TAG,
                    "[ActivityLifecycle] onActivityStarted: ${activity.javaClass.simpleName}"
                )
            }

            override fun onActivityResumed(activity: Activity) {
                super.onActivityResumed(activity)
                Log.i(
                    TAG,
                    "[ActivityLifecycle] onActivityResumed: ${activity.javaClass.simpleName}"
                )
            }

            override fun onActivityPaused(activity: Activity) {
                super.onActivityPaused(activity)
                Log.i(
                    TAG,
                    "[ActivityLifecycle] onActivityPaused: ${activity.javaClass.simpleName}"
                )
            }

            override fun onActivityStopped(activity: Activity) {
                super.onActivityStopped(activity)
                Log.i(
                    TAG,
                    "[ActivityLifecycle] onActivityStopped: ${activity.javaClass.simpleName}"
                )
            }

            override fun onActivityDestroyed(activity: Activity) {
                super.onActivityDestroyed(activity)
                Log.i(
                    TAG,
                    "[ActivityLifecycle] onActivityDestroyed: ${activity.javaClass.simpleName}"
                )
                activityStack.remove(activity)
            }
        })

        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                super.onStart(owner)
                Log.i(TAG, "App 回到前台。")
                isForeground = true
            }

            override fun onStop(owner: LifecycleOwner) {
                super.onStop(owner)
                Log.i(TAG, "App 进入后台。")
                isForeground = false
            }
        })
    }

    /**
     * 退出 app
     */
    fun exit() {
        activityStack.forEach {
            it.finish()
        }
        exitProcess(0)
    }
}