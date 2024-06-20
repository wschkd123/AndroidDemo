package com.example.beyond.demo.util

import android.content.Context

/**
 * xlog 配置
 *
 * Created by fengkeke on 2023/9/5
 */
object XLogConfig {

    val XLOG_PUBKEY =
        "72946e01ac3f5398c28f37f4e6d9ca8193b06280bb9e27b19ae4e770a319490eba307427dbd2db5043f3904265a291ec9106853a9a054223e4b0bf8e857d6e17"

    val XLOG_CACHE = "/xlog/cache"

    val XLOG_FILE = "/xlog/xlog"

    @JvmStatic
    fun getXLogCacheDir(context: Context): String {
        return context.filesDir.absolutePath +  XLOG_CACHE
    }

    @JvmStatic
    fun getXLogDir(context: Context): String {
        return context.filesDir.absolutePath +  XLOG_FILE
    }
}