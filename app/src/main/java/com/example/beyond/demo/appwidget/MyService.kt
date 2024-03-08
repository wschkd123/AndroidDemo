package com.example.beyond.demo.appwidget

import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import com.example.beyond.demo.net.NetResult
import com.example.beyond.demo.net.RetrofitFactory
import com.example.beyond.demo.net.WanAndroidService
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * 高版本服务有问题
 */
class MyService : Service() {
    private var appWidgetIds: IntArray? = null
    private var appWidgetManager: AppWidgetManager? = null
    private var handler: Handler? = null
    companion object {
        private const val TAG = "MyService"
    }
    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        appWidgetIds = intent.getIntArrayExtra("appWidgetIds")
        appWidgetManager = AppWidgetManager.getInstance(this@MyService)
        Log.i(TAG, "MyService onStartCommand appWidgetIds:$appWidgetIds appWidgetManager:$appWidgetManager")
        Toast.makeText(this@MyService, "正在加载最新数据，请稍等... ...", Toast.LENGTH_SHORT).show()
        fetchData().observeForever {
            Log.i(TAG, "fetchData $it")
//            appWidgetManager?.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_grid)

        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
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

}
