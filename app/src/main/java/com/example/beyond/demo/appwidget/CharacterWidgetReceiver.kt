package com.example.beyond.demo.appwidget

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.example.beyond.demo.R
import com.example.beyond.demo.ui.MainActivity

/**
 * 单个人物
 */
class CharacterWidgetReceiver : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.i("AppWidget", "$TAG onReceive: ${intent.action}")
        super.onReceive(context, intent)
        when (intent.action) {
            // 系统刷新广播
            AppWidgetManager.ACTION_APPWIDGET_UPDATE,
                // 接收刷新广播
            ACTION_APPWIDGET_CHARACTER_REFRESH -> {
                // 执行一次任务
                Log.i("AppWidget", "$TAG onReceive, start oneTime workManager")
                WorkManager.getInstance(context)
                    .enqueueUniqueWork(
                        ONE_TIME_WORK_NAME, ExistingWorkPolicy.KEEP, OneTimeWorkRequest.from(
                            CharacterWorker::class.java))
            }
        }

    }
    
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.i("AppWidget", "$TAG onUpdate appWidgetIds: $appWidgetIds ${appWidgetIds.toList()}")
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        Log.i("AppWidget", "$TAG onDisabled")
        WorkManager.getInstance(context).cancelUniqueWork(ONE_TIME_WORK_NAME)
    }

    companion object {
        private const val TAG = "CharacterWidgetReceiver"
        private const val ONE_TIME_WORK_NAME = "one_time"
        const val ACTION_APPWIDGET_CHARACTER_REFRESH = "yuewen.appwidget.action.CHARACTER_REFRESH"

        @SuppressLint("RemoteViewLayout")
        internal fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val activityIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val appOpenIntent = PendingIntent.getActivity(
                context,
                0,
                activityIntent,
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val remoteViews = RemoteViews(context.packageName, R.layout.widget_character).apply {
                setOnClickPendingIntent(R.id.rl_root, appOpenIntent)
            }
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
        }
    }

}
