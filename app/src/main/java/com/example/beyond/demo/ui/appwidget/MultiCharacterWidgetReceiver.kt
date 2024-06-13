package com.example.beyond.demo.ui.appwidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.example.base.Init

/**
 * 多个人物
 */
class MultiCharacterWidgetReceiver : AppWidgetProvider() {

    companion object {
        private const val TAG = "MultiCharacterWidgetReceiver"
        private const val WORK_TIME_MULTI_CHARACTER = "work_time_multi_character"
        const val ACTION_APPWIDGET_MULTI_CHARACTER_REFRESH = "yuewen.appwidget.action.MULTI_CHARACTER_REFRESH"
    }

    init {
        AppWidgetUtils.fixWorkManagerRefresh(TAG, MultiCharacterWorker::class.java)
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            // 系统刷新广播、自定义刷新广播
            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                val appWidgetIds = intent.extras?.getIntArray(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                Log.i("AppWidget", "$TAG onReceive: ${intent.action} appWidgetIds: ${appWidgetIds?.toList()}")
                if (appWidgetIds?.isNotEmpty() == true) {
                    onUpdate(context, AppWidgetManager.getInstance(context), appWidgetIds)
                    startWorker(context, appWidgetIds)
                }
            }
            /**
             * 刷新所有组件
             */
            ACTION_APPWIDGET_MULTI_CHARACTER_REFRESH -> {
                val appWidgetIds = AppWidgetManager.getInstance(Init.applicationContext).getAppWidgetIds(
                    ComponentName(Init.applicationContext, MultiCharacterWidgetReceiver::class.java)
                )
                Log.i("AppWidget", "$TAG onReceive: ${intent.action} appWidgetIds: ${appWidgetIds?.toList()}")
                if (appWidgetIds?.isNotEmpty() == true) {
                    startWorker(context, appWidgetIds)
                }
            }
            else -> {
                Log.i("AppWidget", "$TAG onReceive: ${intent.action}")
                super.onReceive(context, intent)
            }
        }

    }

    private fun startWorker(context: Context, appWidgetIds: IntArray) {
        Log.i("AppWidget", "$TAG startWorker")
        val data = Data.Builder().putIntArray("appWidgetIds", appWidgetIds).build()
        val workRequest = OneTimeWorkRequest.Builder(MultiCharacterWorker::class.java)
            .setInputData(data)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_TIME_MULTI_CHARACTER, ExistingWorkPolicy.REPLACE, workRequest)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        Log.i("AppWidget", "$TAG onDisabled")
        WorkManager.getInstance(context).cancelUniqueWork(WORK_TIME_MULTI_CHARACTER)
    }

}
