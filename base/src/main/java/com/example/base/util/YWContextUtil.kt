package com.example.base.util

import android.content.Context
import android.content.ContextWrapper
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager

/**
 *
 * @author wangshichao
 * @date 2023/9/15
 */
object YWContextUtil {

    @JvmStatic
    fun getFragmentManager(originContext: Context): FragmentManager? {
        return getFragmentActivity(originContext)?.supportFragmentManager
    }

    @JvmStatic
    fun getFragmentActivity(originContext: Context): FragmentActivity? {
        var context = originContext
        while (context is ContextWrapper) {
            if (context is FragmentActivity) {
                return context
            }
            context = context.baseContext
        }
        return null
    }

}