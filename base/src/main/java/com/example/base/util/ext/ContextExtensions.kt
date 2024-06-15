package com.yuewen.baseutil.ext

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager

/**
 * @author beyond
 * @date 2021/12/27
 *
 * [Context]、[Activity] 等扩展函数
 */
fun Context.getActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) {
            return context
        }
        context = context.baseContext
    }
    return null
}

fun Context.getFragmentActivity(): FragmentActivity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is FragmentActivity) {
            return context
        }
        context = context.baseContext
    }
    return null
}

fun Context.getFragmentManager() : FragmentManager? {
    return this.getFragmentActivity()?.supportFragmentManager
}

fun Context.isAlive(): Boolean {
    return this.getActivity()?.isAlive() ?: false
}

fun Activity.isAlive(): Boolean {
    return !this.isFinishing && !this.isDestroyed
}