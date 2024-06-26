package com.example.beyond.demo.ui.appwidget

import android.graphics.Bitmap
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.example.base.Init.applicationContext
import com.example.beyond.demo.util.CircleCropTransform
import java.util.concurrent.TimeUnit

/**
 * @author wangshichao
 * @date 2024/3/12
 */
internal object AppWidgetUtils {

    /**
     * 修复WorkManager更新小部件时一直刷新
     *  https://issuetracker.google.com/issues/241076154
     */
    fun fixWorkManagerRefresh(tag: String, workerClass: Class<out ListenableWorker>) {
        WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork(
                "${tag}_not_executed_work",
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequest.Builder(workerClass)
                    .setInitialDelay(365 * 10, TimeUnit.DAYS)
                    .build()
            )
    }

    /**
     * 同步加载图片
     */
    @WorkerThread
    fun loadBitmapSync(
        tag: String,
        url: String,
        width: Int,
        height: Int,
        radius: Int
    ): Bitmap? {
        var bitmap: Bitmap? = null
        val requestOptions = RequestOptions()
            .transform(CenterCrop(), RoundedCorners(radius))
        try {
            val futureTarget = Glide.with(applicationContext)
                .asBitmap()
                .load(url)
                .apply(requestOptions)
                .submit(width, height)
            bitmap = futureTarget.get()
            Log.i("AppWidget", "$tag loadBitmapSync success, url:${url}")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("AppWidget", "$tag loadBitmapSync fail, url:${url}", e)
        }
        return bitmap
    }

    /**
     * 同步加载圆形图片
     */
    @WorkerThread
    fun loadCircleBitmapSync(
        tag: String,
        url: String,
        width: Int,
        height: Int,
        borderColor: Int,
        borderWidth: Float
    ): Bitmap? {
        var bitmap: Bitmap? = null
        val requestOptions = RequestOptions()
            .transform(CenterCrop(), CircleCropTransform(borderWidth, borderColor))
            .override(width, height)
        try {
            val futureTarget = Glide.with(applicationContext)
                .asBitmap()
                .load(url)
                .apply(requestOptions)
                .submit(width, height)
            bitmap = futureTarget.get()
            Log.i("AppWidget", "$tag loadBitmapSync success, url:${url}")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("AppWidget", "$tag loadBitmapSync fail, url:${url}", e)
        }
        return bitmap
    }

    /**
     * 加载本地圆形图片
     */
    @WorkerThread
    fun loadLocalCircleBitmap(
        resId: Int,
        width: Int,
        height: Int,
        borderColor: Int,
        borderWidth: Float
    ): Bitmap? {
        var bitmap: Bitmap? = null
        val requestOptions = RequestOptions()
            .transform(CenterCrop(), CircleCropTransform(borderWidth, borderColor))
            .override(width, height)
        try {
            val futureTarget = Glide.with(applicationContext)
                .asBitmap()
                .load(resId)
                .apply(requestOptions)
                .submit(width, height)
            bitmap = futureTarget.get()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return bitmap
    }
}
