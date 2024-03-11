package com.example.beyond.demo.appwidget.test

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.WorkerThread
import androidx.lifecycle.MutableLiveData
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.beyond.demo.R
import com.example.beyond.demo.net.NetResult
import com.example.beyond.demo.net.RetrofitFactory
import com.example.beyond.demo.net.WanAndroidService
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TestWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    companion object {
        private const val TAG = "TestWorker"
    }

    init {
        Log.i("AppWidget", "$TAG init")
    }

    @WorkerThread
    override fun doWork(): Result {
//        val result = fetchExposedAppRec()
        Log.i("AppWidget", "$TAG doWork")

        //刷新widget
        val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(
                applicationContext,
                TestWidgetReceiver::class.java
            )
        )
        updateAppWidget(applicationContext, appWidgetManager, appWidgetIds)

        return Result.success()
    }

    @WorkerThread
    private fun fetchExposedAppRec() : Any? {
//        if (!LoginManager.isLogin()) {
//            Log.w("AppWidget", "$TAG fetchData not login")
//            return null
//        }
        try {
            val call = RetrofitFactory.getRetrofit().create(WanAndroidService::class.java)
                .getBannerInfo()
            val response = call.execute()
            val result = response.body()?.data
            Log.i("AppWidget", "$TAG fetchExposedAppRec result: $result")
            // 处理网络请求结果
            if (response.isSuccessful && result != null) {
                return result
            }
        } catch (e: Exception) {
            Log.w("AppWidget", "$TAG fetchExposedAppRec error: ${e.message}")
            e.printStackTrace()
        }
        return null
    }

    /**
     * 更新widget
     */
    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val data = long2String(System.currentTimeMillis())

        val intent = Intent()
        intent.setClass(context, TestWidgetReceiver::class.java)
        intent.setAction(TestWidgetReceiver.REFRESH_ACTION)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val remoteViews = RemoteViews(context.packageName, R.layout.widget_test).apply {
            setTextViewText(R.id.tv_text, data)
            setOnClickPendingIntent(R.id.tv_refresh, pendingIntent)
        }

        Log.i("AppWidget", "$TAG updateWidget appWidgetId: $appWidgetIds ${appWidgetIds.toList()}")
        appWidgetManager.updateAppWidget(appWidgetIds, remoteViews)
    }

    private fun long2String(time: Long): String {
        return try {
            val simpleDateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            simpleDateFormat.format(Date(time))
        } catch (e: Exception) {
            ""
        }
    }
}