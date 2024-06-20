package com.example.beyond.demo

import android.app.Application
import android.content.Context
import com.example.base.AppContext
import com.example.base.Init

/**
 *
 * @author wangshichao
 * @date 2024/3/11
 */
class DemoApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        Init.setApplication(this)
        AppContext.init(this)
        initLogger(this)
    }

    private fun initLogger(context: Context) {
//        val logInitCallback: Logger.InitCallback = object : Logger.InitCallback {
//            override fun onSuccess() {}
//            override fun onFailed(throwable: Throwable) {
//            }
//        }
//
//        // init xlog 主进程 使用xlog，debug包设置同时输出到控制台，release包设置不输出到控制台
//        val logConfiguration = LogConfiguration.Builder()
//            .setXLogLevel(if (BuildConfig.DEBUG) Logger.LEVEL_VERBOSE else Logger.LEVEL_WARNING)
//            .setXlogPubKey(XLogConfig.XLOG_PUBKEY).setCachePath(XLogConfig.getXLogCacheDir(context))
//            .setLogPath(XLogConfig.getXLogDir(context)).setLogPrefix("Demo").setConsoleLogLevel(
//                if (BuildConfig.DEBUG) Logger.LEVEL_VERBOSE else Logger.LEVEL_NONE
//            ).setPrintStackInfo(BuildConfig.DEBUG).build()
//
//        Logger.init(logConfiguration, logInitCallback)
    }
}