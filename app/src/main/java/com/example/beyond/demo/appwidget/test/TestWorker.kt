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
        //模拟耗时/网络请求操作
//        fetchData().observeForever {
//            updateWidget(applicationContext)
//        }
        Log.i("AppWidget", "$TAG doWork")
        //模拟耗时/网络请求操作
        try {
            Thread.sleep(1000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        //刷新widget
        updateWidget(applicationContext)

        return Result.success()
    }

    private fun fetchData(): MutableLiveData<NetResult<Any>> {
        val liveData = MutableLiveData<NetResult<Any>>()
        GlobalScope.launch {
            try {
                val result = RetrofitFactory.getRetrofit().create(WanAndroidService::class.java)
                    .getBannerInfo()
                liveData.postValue(result)
            } catch (e: Exception) {
                e.printStackTrace()
                liveData.postValue(NetResult.badResult())
            }
        }
        return liveData
    }

    /**
     * 刷新widget
     */
    private fun updateWidget(context: Context) {
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

        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, TestWidgetReceiver::class.java))
        Log.i("AppWidget", "$TAG updateWidget appWidgetId: $appWidgetIds ${appWidgetIds.toList()}")
        appWidgetManager.updateAppWidget(appWidgetIds, remoteViews)
    }

    private fun long2String(time: Long): String {
        return try {
            val simpleDateFormat = SimpleDateFormat( "HH:mm:ss", Locale.getDefault())
            simpleDateFormat.format(Date(time))
        } catch (e: Exception) {
            ""
        }
    }
}