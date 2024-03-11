package com.example.beyond.demo.appwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.WorkerThread
import androidx.lifecycle.MutableLiveData
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.example.beyond.demo.R
import com.example.beyond.demo.appwidget.CharacterWidgetReceiver.Companion.ACTION_APPWIDGET_CHARACTER_REFRESH
import com.example.beyond.demo.net.NetResult
import com.example.beyond.demo.net.RetrofitFactory
import com.example.beyond.demo.net.WanAndroidService
import com.example.beyond.demo.util.kt.dpToPx
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

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

    init {
        Log.i("AppWidget", "$TAG init context:$context workerParams:$workerParams")
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
//        updateListView(applicationContext)
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
        val intent = Intent()
        intent.setClass(context, CharacterWidgetReceiver::class.java)
        intent.setAction(ACTION_APPWIDGET_CHARACTER_REFRESH)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val remoteViews = RemoteViews(context.packageName, R.layout.widget_multi_character).apply {
            setTextViewText(R.id.tv_title, "跟梦中人聊聊")
            setOnClickPendingIntent(R.id.tv_title, pendingIntent)
            val urlList = mutableListOf(
                "https://www.wanandroid.com/blogimgs/62c1bd68-b5f3-4a3c-a649-7ca8c7dfabe6.png",
                "https://www.wanandroid.com/blogimgs/50c115c2-cf6c-4802-aa7b-a4334de444cd.png",
                "https://www.wanandroid.com/blogimgs/42da12d8-de56-4439-b40c-eab66c227a4b.png",
                "https://www.wanandroid.com/blogimgs/62c1bd68-b5f3-4a3c-a649-7ca8c7dfabe6.png"
            )
            val imageViewIds = listOf(R.id.iv_character_first, R.id.iv_character_second, R.id.iv_character_third, R.id.iv_character_fourth)
            val textViewIds = listOf(R.id.tv_character_first, R.id.tv_character_second, R.id.tv_character_third, R.id.tv_character_fourth)
            urlList.forEachIndexed { index, url ->
                loadBitmap(url) { bitmap ->
                    setTextViewText(textViewIds[index], "梦屋名称$index")
                    setImageViewBitmap(imageViewIds[index], bitmap)
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, MultiCharacterWidgetReceiver::class.java))
                    Log.i("AppWidget", "$TAG updateWidget appWidgetId: $appWidgetIds ${appWidgetIds.toList()}")
                    appWidgetManager.updateAppWidget(appWidgetIds, this)
                }
            }

        }

    }

    private fun loadBitmap(url: String, invoke: (Bitmap) -> Unit) {
        val requestOptions = RequestOptions()
            .transform(RoundedCorners(10.dpToPx()))
            .override(480, 645)
            .centerCrop()

        Glide.with(applicationContext)
            .asBitmap()
            .load(url)
            .apply(requestOptions)
            .into(object : SimpleTarget<Bitmap>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap>?
                ) {
                    invoke.invoke(resource)
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    super.onLoadFailed(errorDrawable)
                    Log.e("AppWidget", "$TAG onLoadFailed url:$url")
                }
            })

    }

}