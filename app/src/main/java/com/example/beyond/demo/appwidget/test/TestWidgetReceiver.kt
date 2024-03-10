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

        /**
         * 自定义的刷新广播
         */
        const val REFRESH_ACTION = "android.appwidget.action.REFRESH"


        private const val ONE_TIME_WORK_NAME = "one_time"
        /**
         * 定期任务名称
         */
        private const val PERIODIC_WORK_NAME = "periodic"

    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.i("AppWidget", "$TAG onReceive: ${intent.action}")
        super.onReceive(context, intent)
        when (intent.action) {
            // 系统刷新广播
            ACTION_APPWIDGET_UPDATE,
                // 接收刷新广播
            REFRESH_ACTION -> {
//                val extras = intent.extras
//                if (extras != null) {
//                    val appWidgetIds = extras.getIntArray(AppWidgetManager.EXTRA_APPWIDGET_IDS)
//                    if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
//                        onUpdate(context, AppWidgetManager.getInstance(context), appWidgetIds)
//                    }
//                }
                // 执行一次任务
                Log.i("AppWidget", "$TAG onReceive, start oneTime workManager")
                WorkManager.getInstance(context)
                    .enqueueUniqueWork(ONE_TIME_WORK_NAME, ExistingWorkPolicy.KEEP, OneTimeWorkRequest.from(TestWorker::class.java))
            }
        }

    }

    /**
     * 调用时机：
     * 1. 向桌面添加AppWidget时
     * 2. 到达指定的更新时间
     * 3. 更新widget时
     */
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.i("AppWidget", "$TAG onUpdate appWidgetIds: $appWidgetIds ${appWidgetIds.toList()}")
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }

    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        Log.i("AppWidget", "$TAG updateAppWidget appWidgetId: $appWidgetId")
        val remoteViews = RemoteViews(context.packageName, R.layout.widget_test).apply {

        }
        appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        Log.i("AppWidget", "$TAG onDeleted appWidgetIds: $appWidgetIds ${appWidgetIds.toList()}}")
    }

    /**
     * AppWidget的实例第一次被创建时调用
     */
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
//        Log.i("AppWidget", "$TAG onEnabled，start periodic workManager")
        // 开始定时工作,间隔15分钟刷新一次
//        val workRequest = PeriodicWorkRequest.Builder(
//            TestWorker::class.java,
//            PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS, TimeUnit.MILLISECONDS
//        ).setConstraints(Constraints.Builder().build()).build()
//        WorkManager.getInstance(context)
//            .enqueueUniquePeriodicWork(PERIODIC_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, workRequest)
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