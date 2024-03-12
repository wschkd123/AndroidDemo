package com.example.beyond.demo.appwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
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
 * 单个人物 WorkerManager
 *
 * @author wangshichao
 * @date 2024/3/8
 */
class CharacterWorker(context: Context, val workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    companion object {
        private const val TAG = "CharacterWorker"
    }

    init {
        Log.i("AppWidget", "$TAG init workerParams:${workerParams.id}")
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
        val appWidgetIds = workerParams.inputData.getIntArray("appWidgetIds")
        Log.i("AppWidget", "$TAG doWork widgetIds:${appWidgetIds?.toList()}")
        //刷新widget
        if (appWidgetIds == null || appWidgetIds.isEmpty() || appWidgetIds[0] == 0) {
            Log.w("AppWidget", "$TAG appWidgetIds is empty, return")
            return Result.success()
        }

        //TODO 网络请求
        val type = object : TypeToken<NetResult<AppRecResult>>() {}.type
        val recList =
            Gson().fromJson<NetResult<AppRecResult>>(AppRecResult.MOCK_1_GROUP, type).data?.recList

        updateAppWidgetFromServer(applicationContext, AppWidgetManager.getInstance(applicationContext), appWidgetIds, recList)
        Log.i("AppWidget", "$TAG doWork end")
        return Result.success()
    }

    override fun onStopped() {
        super.onStopped()
        Log.i("AppWidget", "$TAG onStopped")
    }

    /**
     * 更新widget
     */
    private fun updateAppWidgetFromServer(
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
            Log.i("AppWidget", "$TAG updateWidget empty")
        } else {
            val recCharacter = recList[0]
            val remoteViews = RemoteViews(context.packageName, R.layout.widget_character).apply {
//                setOnClickPendingIntent(R.id.root_view_character, appOpenIntent)
                //TODO 跳转个人页
                setOnClickPendingIntent(R.id.root_view_character, appOpenIntent)
            }
            // 名称
            remoteViews.setTextViewText(R.id.tv_name, recCharacter.getName())

            // 背景头像
            AppWidgetUtils.loadBitmapSync(TAG, recCharacter.getAvatarUrl(), 480, 480, 22.dpToPx())
                ?.let {
                    remoteViews.setImageViewBitmap(R.id.iv_avatar, it)
                }

            // 群聊头像
            if (!recCharacter.characterList.isNullOrEmpty()) {
                getMemberAvatarBitmapSync(recCharacter.getGroupMemberUrlList()).let {
                    remoteViews.setImageViewBitmap(R.id.iv_group_member, it)
                }
            }

            Log.i("AppWidget", "$TAG updateWidget")
            appWidgetManager.updateAppWidget(appWidgetIds, remoteViews)
        }
    }


    /**
     * 将成员多个头像绘制到一个Bitmap上
     */
    private fun getMemberAvatarBitmapSync(urlList: List<String>): Bitmap {
        val rectSize = 36.dpToPx() // 圆形图片的宽度
        val imageSize = 28.dpToPx()
        val borderWidth = 4.dpToPx() // 边框宽度
        val overlapOffset = 8.dpToPx() // 第 n-1 张图片压在第 n 张图片上方的偏移量
        val memberCount = urlList.size // 图片数量
        val totalWidth = imageSize * memberCount + borderWidth * (memberCount - 1)
        val totalHeight = imageSize
        //TODO 修正totalWidth
        // 创建一个空白的Bitmap
        val bitmap = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(ANTI_ALIAS_FLAG)
        for (index in 0 until memberCount) {
            AppWidgetUtils.loadBitmapSync(TAG, urlList[index], imageSize, imageSize, 99.dpToPx())
                ?.let { curBitmap ->
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
                    canvas.drawBitmap(curBitmap, null, rectF, paint)

                    // 清除 Xfermode，以便绘制边框
                    paint.xfermode = null
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
                }
        }
        return bitmap
    }

}