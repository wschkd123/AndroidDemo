package com.example.beyond.demo.appwidget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
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

class CharacterWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    companion object {
        private const val TAG = "CharacterWorker"
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
        //只能通过远程对象来设置appwidget中的控件状态
        val remoteViews = RemoteViews(context.packageName, R.layout.widget_test)
        //通过远程对象修改textview
        remoteViews.setTextViewText(R.id.tv_text, data)

        //获得appwidget管理实例，用于管理appwidget以便进行更新操作
        val appWidgetManager = AppWidgetManager.getInstance(context)
        //获得所有本程序创建的appwidget
        val componentName = ComponentName(context, CharacterWidgetProviderTest::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
        //更新appwidget
        Log.i("AppWidget", "$TAG updateWidget appWidgetId: $appWidgetIds ${appWidgetIds.toList()}")
        appWidgetManager.updateAppWidget(componentName, remoteViews)
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