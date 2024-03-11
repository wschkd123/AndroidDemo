package com.example.beyond.demo.appwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.net.Uri
import android.transition.Transition
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.WorkerThread
import androidx.lifecycle.MutableLiveData
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.SimpleTarget
import com.example.beyond.demo.R
import com.example.beyond.demo.appwidget.CharacterWidgetReceiver.Companion.ACTION_APPWIDGET_CHARACTER_REFRESH
import com.example.beyond.demo.net.NetResult
import com.example.beyond.demo.net.RetrofitFactory
import com.example.beyond.demo.net.WanAndroidService
import com.example.beyond.demo.util.kt.dpToPx
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class CharacterWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    companion object {
        private const val TAG = "CharacterWorker"

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
        val remoteViews = RemoteViews(context.packageName, R.layout.widget_character).apply {
            setTextViewText(R.id.tv_name, "梦屋名称名称名称名称梦屋名称名称名称名称")
            setOnClickPendingIntent(R.id.tv_name, pendingIntent)
            drawCanvas(this)
        }

        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(
                context,
                CharacterWidgetReceiver::class.java
            )
        )
        Log.i("AppWidget", "$TAG updateWidget appWidgetId: $appWidgetIds ${appWidgetIds.toList()}")
        appWidgetManager.updateAppWidget(appWidgetIds, remoteViews)
    }

    private fun updateListView(context: Context) {
        val remoteViews = RemoteViews(context.packageName, R.layout.widget_character)
        val intent = Intent(context, ListDemoService::class.java).apply {
//            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
        }
        remoteViews.setRemoteAdapter(com.example.beyond.demo.R.id.lv_demo, intent)
        // 为集合设置待定 intent
//        val itemClickIntent = Intent(context, ListDemoWidget::class.java).apply {
//            action = ACTION_NOTIFY_ITEM_DONE_CHANGED
//            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
//            data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
//        }
//        val pendingIntentTemplate = PendingIntent.getBroadcast(context, 0, itemClickIntent, PendingIntent.FLAG_UPDATE_CURRENT)
//        remoteViews.setPendingIntentTemplate(R.id.lv_demo, pendingIntentTemplate)
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(
                context,
                CharacterWidgetReceiver::class.java
            )
        )
        Log.i("AppWidget", "$TAG updateWidget appWidgetId: $appWidgetIds ${appWidgetIds.toList()}")
        appWidgetManager.updateAppWidget(appWidgetIds, remoteViews)

    }

    private fun drawCanvas(remoteViews: RemoteViews) {
        val width = (155 - 28).dpToPx()
        val height = 36.dpToPx()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            this.color = Color.RED
            this.strokeWidth = 2f
            this.style = Paint.Style.STROKE
        }


        canvas.drawLine(0f, height / 2f, width.toFloat(), height / 2f, paint)
        drawRoundedRect(applicationContext, remoteViews)
    }

    private fun drawRoundedRect(context: Context, remoteViews: RemoteViews) {
        val urlList = mutableListOf(
            "https://www.wanandroid.com/blogimgs/62c1bd68-b5f3-4a3c-a649-7ca8c7dfabe6.png",
            "https://www.wanandroid.com/blogimgs/50c115c2-cf6c-4802-aa7b-a4334de444cd.png",
            "https://www.wanandroid.com/blogimgs/42da12d8-de56-4439-b40c-eab66c227a4b.png"
        )
        val imageSize = 36 // 圆形图片的宽度
        val borderWidth = 4 // 边框宽度
        val overlapOffset = 8 // 第 n-1 张图片压在第 n 张图片上方的偏移量
        val N = urlList.size // 图片数量

        val bitmap = Bitmap.createBitmap(imageSize * N, imageSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        paint.isAntiAlias = true

        val requestOptions = RequestOptions()
            .override(imageSize, imageSize)
            .centerCrop()

        for (i in 0 until N) {
            val index = i
            Glide.with(context)
                .asBitmap()
                .load(urlList[i])
                .apply(requestOptions)
                .into(object : SimpleTarget<Bitmap?>(imageSize, imageSize) {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: com.bumptech.glide.request.transition.Transition<in Bitmap?>?
                    ) {
                        Log.i("AppWidget", "$TAG onResourceReady index: $index url:${urlList[index]}")
                        // 计算当前图片的绘制位置，考虑重叠偏移量
                        val left = imageSize * index + borderWidth * index
                        val top = borderWidth * index - overlapOffset * index

                        // 创建一个矩形用于绘制当前图片
                        val rectF = RectF(
                            left.toFloat(),
                            top.toFloat(),
                            (left + imageSize).toFloat(),
                            (top + imageSize).toFloat()
                        )

                        // 绘制圆形图片
                        canvas.drawOval(rectF, paint)
                        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
                        canvas.drawBitmap(resource, null, rectF, paint)

                        // 清除 Xfermode，以便绘制边框
                        paint.xfermode = null

                        // 绘制边框
                        paint.style = Paint.Style.STROKE
                        paint.strokeWidth = borderWidth.toFloat()
                        paint.color = Color.WHITE
                        canvas.drawOval(rectF, paint)

                        // 绘制完成后，检查是否是最后一张图片，进行显示
                        if (index == N - 1) {
//                            imageView.setImageBitmap(bitmap)
                            remoteViews.setImageViewBitmap(
                                R.id.iv_group_member, bitmap
                            )
                        }

                    }

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        super.onLoadFailed(errorDrawable)
                        Log.i("AppWidget", "$TAG onLoadFailed index: $index url:${urlList[index]}")

                    }


                })
        }
    }

}