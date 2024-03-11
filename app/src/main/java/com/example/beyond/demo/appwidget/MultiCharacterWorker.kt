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
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.example.beyond.demo.R
import com.example.beyond.demo.net.NetResult
import com.example.beyond.demo.ui.MainActivity
import com.example.beyond.demo.util.kt.dpToPx
import com.google.gson.reflect.TypeToken
import com.example.beyond.demo.appwidget.bean.AppRecResult

/**
 * 多个人物 WorkerManager
 *
 * @author wangshichao
 * @date 2024/3/11
 */
class MultiCharacterWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    private val appOpenIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        },
        PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    companion object {
        private const val TAG = "MultiCharacterWorker"

    }

    init {
        Log.i("AppWidget", "$TAG init context:$context workerParams:$workerParams")
    }

    @WorkerThread
    override fun doWork(): Result {
        // 网络请求
        val type = object : TypeToken<NetResult<AppRecResult>>() {}.type
//        val recList = Gson().fromJson<NetResult<AppRecResult>>(TestWorker.MOCK_DATA, type).data?.recList
        val recList = null

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
            RemoteViews(context.packageName, R.layout.widget_multi_character).apply {
                setOnClickPendingIntent(R.id.root_view, appOpenIntent)
                setTextViewText(R.id.tv_title, "跟梦中人聊聊")
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
                        Log.i("AppWidget", "$TAG updateWidget appWidgetId: $appWidgetIds ${appWidgetIds.toList()}")
                        appWidgetManager.updateAppWidget(appWidgetIds, this)
                    }
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