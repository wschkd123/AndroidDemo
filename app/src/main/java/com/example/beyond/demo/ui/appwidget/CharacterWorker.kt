package com.example.beyond.demo.ui.appwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.graphics.RectF
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.WorkerThread
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.base.util.YWNetUtil
import com.example.base.util.ext.dpToPx
import com.example.base.util.ext.dpToPxFloat
import com.example.beyond.demo.R
import com.example.beyond.demo.net.NetResult
import com.example.beyond.demo.ui.MainActivity
import com.example.beyond.demo.ui.appwidget.bean.AppRecResult
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
        //刷新widget
        if (appWidgetIds == null || appWidgetIds.isEmpty() || appWidgetIds[0] == 0) {
            Log.w("AppWidget", "$TAG appWidgetIds is empty, return")
            return Result.success()
        }

        if (!YWNetUtil.isNetworkAvailable(applicationContext)) {
            Log.w("AppWidget", "$TAG doWork network not available")
            return Result.success()
        }

        //TODO 网络请求
        val type = object : TypeToken<NetResult<AppRecResult>>() {}.type
        val recList =
            Gson().fromJson<NetResult<AppRecResult>>(AppRecResult.MOCK_1, type).data?.recList

        updateAppWidgetFromServer(
            applicationContext,
            AppWidgetManager.getInstance(applicationContext),
            appWidgetIds,
            recList
        )
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
        when {
            // 数据异常
            recList.isNullOrEmpty() -> {
                RemoteViews(context.packageName, R.layout.widget_character_empty).apply {
                    setOnClickPendingIntent(R.id.root_view_character_empty, appOpenIntent)
                    appWidgetManager.updateAppWidget(appWidgetIds, this)
                }
                Log.w("AppWidget", "$TAG updateWidget empty")
            }

            // 群聊
            recList[0].isGroupChat() -> {
                val recCharacter = recList[0]
                val remoteViews =
                    RemoteViews(context.packageName, R.layout.widget_character_group).apply {
                        setOnClickPendingIntent(R.id.root_view_character, appOpenIntent)

                    }
                // 名称
                remoteViews.setTextViewText(R.id.tv_name, recCharacter.getName())

                // 背景头像
                AppWidgetUtils.loadBitmapSync(
                    TAG,
                    recCharacter.getAvatarUrl(),
                    context.resources.getDimension(R.dimen.widget_two_cell_height).toInt(),
                    context.resources.getDimension(R.dimen.widget_two_cell_height).toInt(),
                    22.dpToPx()
                )?.let {
                    remoteViews.setImageViewBitmap(R.id.iv_avatar, it)
                }

                // 群聊头像
                getMemberAvatarBitmapSync(recCharacter.getGroupMemberUrlList()).let {
                    remoteViews.setImageViewBitmap(R.id.iv_group_member, it)
                }

                Log.w("AppWidget", "$TAG updateWidget groupChat")
                appWidgetManager.updateAppWidget(appWidgetIds, remoteViews)
            }

            // 非群聊
            else -> {
                val recCharacter = recList[0]
                val remoteViews =
                    RemoteViews(context.packageName, R.layout.widget_character).apply {
                        setOnClickPendingIntent(R.id.root_view_character, appOpenIntent)

                    }
                // 名称
                remoteViews.setTextViewText(R.id.tv_name, recCharacter.getName())

                // 背景头像
                AppWidgetUtils.loadBitmapSync(
                    TAG,
                    recCharacter.getAvatarUrl(),
                    context.resources.getDimension(R.dimen.widget_two_cell_height).toInt(),
                    context.resources.getDimension(R.dimen.widget_two_cell_height).toInt(),
                    22.dpToPx()
                )?.let {
                    remoteViews.setImageViewBitmap(R.id.iv_avatar, it)
                }

                Log.w("AppWidget", "$TAG updateWidget")
                appWidgetManager.updateAppWidget(appWidgetIds, remoteViews)
            }
        }
    }

    /**
     * 将成员多个头像绘制到一个Bitmap上
     */
    private fun getMemberAvatarBitmapSync(urlList: List<String>): Bitmap {
        // 头像的尺寸
        val avatarSize =
            applicationContext.resources.getDimension(R.dimen.widget_group_avatar_size).toInt()
        // 第 n-1 张图片压在第 n 张图片上方的偏移量
        val overlapOffset = 8.dpToPx()

        // 加载带边框和圆角的图片
        val originBitmapList = mutableListOf<Bitmap>()
        for (url in urlList) {
            AppWidgetUtils.loadCircleBitmapSync(
                TAG,
                url,
                avatarSize,
                avatarSize,
                borderWidth = 4.dpToPxFloat(),
                borderColor = ContextCompat.getColor(
                    applicationContext,
                    R.color.widget_group_avatar_border
                )
            )?.let {
                originBitmapList.add(it)
            }
        }

        // 加载群聊图标
        AppWidgetUtils.loadLocalCircleBitmap(
            R.drawable.widget_ic_group,
            avatarSize,
            avatarSize,
            borderWidth = 2.dpToPxFloat(),
            borderColor = ContextCompat.getColor(
                applicationContext,
                R.color.widget_group_avatar_border
            )
        )?.let {
            originBitmapList.add(it)
        }

        // 从右往左绘制图片
        val canvasWidth =
            originBitmapList.size * avatarSize - (originBitmapList.size - 1) * overlapOffset
        val bitmap = Bitmap.createBitmap(canvasWidth, avatarSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(ANTI_ALIAS_FLAG)
        paint.color = ContextCompat.getColor(applicationContext, R.color.widget_group_avatar_bg)
        canvas.drawRoundRect(
            0f,
            0f,
            canvasWidth.toFloat(),
            avatarSize.toFloat(),
            canvasWidth.div(2f),
            canvasWidth.div(2f),
            paint
        )
        for (index in originBitmapList.size - 1 downTo 0) {
            val left = (avatarSize - overlapOffset) * index
            val rectF = RectF(
                left.toFloat(),
                0f,
                (left + avatarSize).toFloat(),
                avatarSize.toFloat()
            )
            canvas.drawBitmap(originBitmapList[index], null, rectF, paint)
        }
        return bitmap
    }

}