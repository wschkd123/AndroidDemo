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
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.WorkerThread
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.example.beyond.demo.R
import com.example.beyond.demo.net.NetResult
import com.example.beyond.demo.ui.MainActivity
import com.example.beyond.demo.util.kt.dpToPx
import com.google.gson.reflect.TypeToken
import com.example.beyond.demo.appwidget.bean.AppRecResult
import com.example.beyond.demo.appwidget.test.TestWorker
import com.google.gson.Gson

/**
 * 单个人物 WorkerManager
 *
 * @author wangshichao
 * @date 2024/3/8
 */
class CharacterWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    companion object {
        private const val TAG = "CharacterWorker"
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
        val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(applicationContext, CharacterWidgetReceiver::class.java))
        updateAppWidget(applicationContext, appWidgetManager, appWidgetIds, recList)
        return Result.success()
    }

    /**
     * 更新widget
     */
    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
        recList: List<AppRecResult.Rec>? = null
    ) {
        if (recList.isNullOrEmpty()) {
            RemoteViews(context.packageName, R.layout.widget_character_empty).apply {
                setOnClickPendingIntent(R.id.root_view_character_empty, appOpenIntent)
                appWidgetManager.updateAppWidget(appWidgetIds, this)
            }
            Log.i("AppWidget", "$TAG updateWidget empty appWidgetId: $appWidgetIds ${appWidgetIds.toList()}")
        } else {
            val recCharacter = recList[0]
            val remoteViews = RemoteViews(context.packageName, R.layout.widget_character).apply {
                setOnClickPendingIntent(R.id.root_view_character, appOpenIntent)
            }
            // 名称
            remoteViews.setTextViewText(R.id.tv_name, recCharacter.getName())

            // 背景头像
            AppWidgetUtils.loadBitmap(recCharacter.getAvatarUrl(), 480, 480, 22.dpToPx()) {
                remoteViews.setImageViewBitmap(R.id.iv_avatar, it)
                appWidgetManager.updateAppWidget(appWidgetIds, remoteViews)
            }

            // 群聊头像
            if (!recCharacter.characterList.isNullOrEmpty()) {
                getMemberAvatarBitmap(recCharacter.getGroupMemberUrlList()) {
                    remoteViews.setImageViewBitmap(R.id.iv_group_member, it)
                    Log.i("AppWidget", "$TAG updateWidget appWidgetId: $appWidgetIds ${appWidgetIds.toList()}")
                    appWidgetManager.updateAppWidget(appWidgetIds, remoteViews)
                }
            }
        }
    }


    /**
     * 将成员多个头像绘制到一个Bitmap上
     */
    private fun getMemberAvatarBitmap(urlList: List<String>, invoke: (Bitmap) -> Unit) {
        val rectSize = 36.dpToPx() // 圆形图片的宽度
        val imageSize = 28.dpToPx()
        val borderWidth = 4.dpToPx() // 边框宽度
        val overlapOffset = 8.dpToPx() // 第 n-1 张图片压在第 n 张图片上方的偏移量
        val memberCount = urlList.size // 图片数量

        val totalWidth = imageSize * memberCount + borderWidth * (memberCount - 1)
        val totalHeight = imageSize
        //TODO 修正totalWidth
        val bitmap = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(ANTI_ALIAS_FLAG)

        val requestOptions = RequestOptions()
            .override(480, 480)
            .centerCrop()

        var loadedImages = 0 // 已加载的图片数量
        for (i in 0 until memberCount) {
            val index = i
            Glide.with(applicationContext)
                .asBitmap()
                .load(urlList[i])
                .apply(requestOptions)
                .into(object : SimpleTarget<Bitmap?>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap?>?
                    ) {
                        Log.i("AppWidget", "$TAG onResourceReady index: $index url:${urlList[index]}")
                        // 计算当前图片的绘制位置，考虑重叠偏移量
                        val left = (imageSize + borderWidth - overlapOffset) * index

                        // 创建一个矩形用于绘制当前图片
                        val rectF = RectF(
                            left.toFloat() + borderWidth,
                            borderWidth.toFloat(),
                            (left + imageSize - borderWidth).toFloat(),
                            (imageSize - borderWidth).toFloat()
                        )

                        // 绘制圆形图片
                        paint.reset()
                        canvas.drawOval(rectF, paint)
                        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
                        canvas.drawBitmap(resource, null, rectF, paint)

                        // 清除 Xfermode，以便绘制边框
                        paint.xfermode = null

                        // 绘制边框
                        paint.style = Paint.Style.STROKE
                        paint.strokeWidth = borderWidth.toFloat()
                        paint.color = Color.WHITE
                        val borderRectF = RectF(
                            rectF.left - borderWidth.div(2),
                            rectF.top - borderWidth.div(2),
                            rectF.right + borderWidth.div(2),
                            rectF.bottom + borderWidth.div(2)
                        )
                        canvas.drawOval(borderRectF, paint)

                        loadedImages++

                        // 所有图片加载完成后，显示最终结果
                        if (loadedImages == memberCount) {
                            Log.w("AppWidget", "$TAG all image loaded")
                            invoke.invoke(bitmap)
                        }

                    }

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        super.onLoadFailed(errorDrawable)
                        Log.e("AppWidget", "$TAG onLoadFailed index: $index url:${urlList[index]}")

                    }


                })
        }
    }

}