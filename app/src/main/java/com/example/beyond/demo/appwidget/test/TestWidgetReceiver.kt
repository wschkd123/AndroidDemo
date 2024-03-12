package com.example.beyond.demo.appwidget.test

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.example.beyond.demo.R

/**
 * Author: clement
 * Create: 2022/7/22
 * Desc:
 */
class TestWidgetReceiver : AppWidgetProvider() {

    companion object {
        private const val TAG = "TestWidgetReceiver"
        const val REFRESH_ACTION = "android.appwidget.action.REFRESH"
        private const val ONE_TIME_WORK_NAME = "one_time"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.i("AppWidget", "$TAG onReceive: ${intent.action}")
        super.onReceive(context, intent)
        when (intent.action) {
            // 系统刷新广播、自定义刷新广播
            ACTION_APPWIDGET_UPDATE,
            REFRESH_ACTION -> {
                // 执行一次任务
                Log.i("AppWidget", "$TAG onReceive, start oneTime workManager")
                WorkManager.getInstance(context)
                    .enqueueUniqueWork(
                        ONE_TIME_WORK_NAME,
                        ExistingWorkPolicy.REPLACE,
                        OneTimeWorkRequest.from(TestWorker::class.java)
                    )
            }
        }

    }


    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        Log.i("AppWidget", "$TAG onDeleted appWidgetIds: $appWidgetIds ${appWidgetIds.toList()}}")
    }

    /**
     * 删除一个AppWidget时调用
     */
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        Log.i("AppWidget", "$TAG onDisabled")
        WorkManager.getInstance(context).cancelUniqueWork(ONE_TIME_WORK_NAME)
    }

}