package com.example.beyond.demo.ui.appwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.WorkerThread
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.base.util.YWNetUtil
import com.example.base.util.ext.dpToPx
import com.example.beyond.demo.R
import com.example.beyond.demo.net.NetResult
import com.example.beyond.demo.ui.MainActivity
import com.example.beyond.demo.ui.appwidget.bean.AppRecResult
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 多个人物 WorkerManager
 *
 * @author wangshichao
 * @date 2024/3/11
 */
class MultiCharacterWorker(context: Context, private val workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    companion object {
        private const val TAG = "MultiCharacterWorker"
    }

    init {
        Log.i("AppWidget", "$TAG init workerParams:${workerParams.id}")
    }

    private val appOpenIntent by lazy {
        PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            },
            PendingIntent.FLAG_IMMUTABLE
        )
    }

    @WorkerThread
    override fun doWork(): Result {
        val appWidgetIds = workerParams.inputData.getIntArray("appWidgetIds")
        Log.i("AppWidget", "$TAG doWork widgetIds:${appWidgetIds?.toList()}")
        if (appWidgetIds == null || appWidgetIds.isEmpty() || appWidgetIds[0] == 0) {
            Log.w("AppWidget", "$TAG appWidgetIds is empty, return")
            return Result.success()
        }

        if (!YWNetUtil.isNetworkAvailable(applicationContext)) {
            Log.w("AppWidget", "$TAG  doWork network not available")
            return Result.success()
        }

        //TODO 网络请求
        val type = object : TypeToken<NetResult<AppRecResult>>() {}.type
        val recList =
            Gson().fromJson<NetResult<AppRecResult>>(AppRecResult.MOCK_2, type).data?.recList
        Log.i("AppWidget", "$TAG doWork")

        updateAppWidgetFromServer(applicationContext, AppWidgetManager.getInstance(applicationContext), appWidgetIds, recList)
        Log.i("AppWidget", "$TAG doWork end")
        return Result.success()
    }

    /**
     * 刷新widget
     */
    private fun updateAppWidgetFromServer(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
        recList: List<AppRecResult.Rec>? = null
    ) {
        if (recList.isNullOrEmpty()) { // 数据异常
            RemoteViews(context.packageName, R.layout.widget_multi_character_empty).apply {
                setOnClickPendingIntent(R.id.root_view_multi_character_empty, appOpenIntent)
                appWidgetManager.updateAppWidget(appWidgetIds, this)
            }
            Log.i("AppWidget", "$TAG updateWidget empty")
        } else { // 数据正常
            val remoteViews =
                RemoteViews(context.packageName, R.layout.widget_multi_character).apply {
                    setOnClickPendingIntent(R.id.ll_top, appOpenIntent)
                }
            val imageViewIds = listOf(
                R.id.iv_character_first,
                R.id.iv_character_second,
                R.id.iv_character_third,
                R.id.iv_character_fourth
            )
            val textViewIds = listOf(
                R.id.tv_character_first,
                R.id.tv_character_second,
                R.id.tv_character_third,
                R.id.tv_character_fourth
            )
            recList.take(4).forEachIndexed { index, rec ->
                // 人物昵称
                remoteViews.setTextViewText(textViewIds[index], rec.getCharacterName())

                // 人物形象
                AppWidgetUtils.loadBitmapSync(
                    TAG,
                    rec.getAvatarUrl(),
                    64.dpToPx(),
                    86.dpToPx(),
                    radius = 10.dpToPx()
                )?.let { bitmap ->
                    remoteViews.setImageViewBitmap(imageViewIds[index], bitmap)
                        remoteViews.setOnClickPendingIntent(imageViewIds[index], appOpenIntent)
                }
            }

            Log.i("AppWidget", "$TAG updateWidget")
            appWidgetManager.updateAppWidget(appWidgetIds, remoteViews)
        }

    }

}