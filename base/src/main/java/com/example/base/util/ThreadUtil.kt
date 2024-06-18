package com.example.base.util

import android.os.Handler
import android.os.Looper

/**
 * 切线程到主线程
 *
 * @author wangshichao
 * @date 2024/6/14
 */
object ThreadUtil {
    private val isMainThread: Boolean
        get() = Looper.getMainLooper() == Looper.myLooper()
    private val handler = Handler(Looper.getMainLooper())

    fun runOnUiThread(r: Runnable) {
        if (isMainThread) {
            r.run()
        } else {
            handler.post(r)
        }
    }

    fun runOnUiThread(r: Runnable, delay: Long) {
        handler.postDelayed(r, delay)
    }

    fun runOnUiThreadAtFront(r: Runnable) {
        if (isMainThread) {
            r.run()
        } else {
            handler.postAtFrontOfQueue(r)
        }
    }

    fun removeCallbacks(r: Runnable) {
        handler.removeCallbacks(r)
    }

}