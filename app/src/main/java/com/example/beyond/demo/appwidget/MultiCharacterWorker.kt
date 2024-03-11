package com.example.beyond.demo.appwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.WorkerThread
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.beyond.demo.R
import com.example.beyond.demo.appwidget.bean.AppRecResult
import com.example.beyond.demo.net.NetResult
import com.example.beyond.demo.ui.MainActivity
import com.example.beyond.demo.util.kt.dpToPx
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 多个人物 WorkerManager
 *
 * @author wangshichao
 * @date 2024/3/11
 */
class MultiCharacterWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    companion object {
        private const val TAG = "MultiCharacterWorker"
    }
    private val appOpenIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        },
        PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    @WorkerThread
    override fun doWork(): Result {
        // 网络请求
        val type = object : TypeToken<NetResult<AppRecResult>>() {}.type
        val recList = Gson().fromJson<NetResult<AppRecResult>>(AppRecResult.MOCK_DATA, type).data?.recList
        try {
            Thread.sleep(6000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        Log.i("AppWidget", "$TAG doWork")

        //刷新widget
        val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(applicationContext, MultiCharacterWidgetReceiver::class.java))
        updateAppWidget(applicationContext, appWidgetManager, appWidgetIds, recList)
        return Result.success()
    }

    /**
     * 刷新widget
     */
    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
        recList: List<AppRecResult.Rec>? = null
    ) {
        if (recList.isNullOrEmpty()) {
            RemoteViews(context.packageName, R.layout.widget_multi_character_empty).apply {
                setOnClickPendingIntent(R.id.root_view, appOpenIntent)
                appWidgetManager.updateAppWidget(appWidgetIds, this)
            }
            Log.i("AppWidget", "$TAG updateWidget empty appWidgetId: $appWidgetIds ${appWidgetIds.toList()}")
        } else {
            val remoteViews = RemoteViews(context.packageName, R.layout.widget_multi_character).apply {
                setOnClickPendingIntent(R.id.root_view, appOpenIntent)
            }

            val imageViewIds = listOf(R.id.iv_character_first, R.id.iv_character_second, R.id.iv_character_third, R.id.iv_character_fourth)
            val textViewIds = listOf(R.id.tv_character_first, R.id.tv_character_second, R.id.tv_character_third, R.id.tv_character_fourth)
            recList.subList(0, 4).forEachIndexed { index, rec ->
                // 人物昵称
                remoteViews.setTextViewText(textViewIds[index], rec.getCharacterName())

                // 人物形象
                AppWidgetUtils.loadBitmap(rec.getAvatarUrl(), 480, 645, 10.dpToPx()) { bitmap ->
                    remoteViews.setImageViewBitmap(imageViewIds[index], bitmap)
                    Log.i("AppWidget", "$TAG updateWidget appWidgetId: $appWidgetIds ${appWidgetIds.toList()}")
                    appWidgetManager.updateAppWidget(appWidgetIds, remoteViews)
                }
            }
        }

    }

}