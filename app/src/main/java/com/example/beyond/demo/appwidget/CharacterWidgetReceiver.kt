package com.example.beyond.demo.appwidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.beyond.demo.common.Init.applicationContext
import java.util.concurrent.TimeUnit

/**
 * 单个人物
 */
class CharacterWidgetReceiver : AppWidgetProvider() {

    companion object {
        private const val TAG = "CharacterWidgetReceiver"
        private const val ONE_TIME_WORK_NAME = "one_time"
        const val ACTION_APPWIDGET_CHARACTER_REFRESH = "yuewen.appwidget.action.CHARACTER_REFRESH"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            // 系统刷新广播、自定义刷新广播
            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                val appWidgetIds = intent.extras?.getIntArray(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                Log.i("AppWidget", "$TAG onReceive: ACTION_APPWIDGET_UPDATE appWidgetIds: ${appWidgetIds?.toList()}")
                if (appWidgetIds?.isNotEmpty() == true) {
                    onUpdate(context, AppWidgetManager.getInstance(context), appWidgetIds)
                    startWorker(context, appWidgetIds)
                }
            }
            /**
             * 刷新所有组件
             */
            ACTION_APPWIDGET_CHARACTER_REFRESH -> {
                val appWidgetIds = AppWidgetManager.getInstance(applicationContext).getAppWidgetIds(ComponentName(applicationContext, CharacterWidgetReceiver::class.java))
                Log.i("AppWidget", "$TAG onReceive: ACTION_APPWIDGET_CHARACTER_REFRESH appWidgetIds: ${appWidgetIds?.toList()}")
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
        val workRequest = OneTimeWorkRequest.Builder(CharacterWorker::class.java)
            .setInputData(data)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(ONE_TIME_WORK_NAME, ExistingWorkPolicy.KEEP, workRequest)
//        WorkManager.getInstance(context).getWorkInfosForUniqueWorkLiveData(ONE_TIME_WORK_NAME).observeForever { info ->
//            if (info.isNotEmpty()) {
//                val workInfo = info[0]
//                Log.i("AppWidget", "$TAG workState ${workInfo.state.name} id:${workInfo.id}")
//            }
//        }

    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        Log.i("AppWidget", "$TAG onDisabled")
        WorkManager.getInstance(context).cancelUniqueWork(ONE_TIME_WORK_NAME)
    }

}
