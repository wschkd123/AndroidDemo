package com.example.base.download

import android.util.Log
import com.example.base.AppContext
import com.example.base.util.HttpLogInterceptor
import com.example.base.util.YWFileUtil
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 音频下载管理
 *
 * @author wangshichao
 * @date 2024/6/18
 */
object AudioDownloadManager {
    private const val TAG = "ExoPlayerDownload"
    private val okHttpClient: OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.MINUTES)
            .readTimeout(10, TimeUnit.MINUTES)
            .addNetworkInterceptor(HttpLogInterceptor())
            .build()
    private val audioDir =
        YWFileUtil.getStorageFileDir(AppContext.application).path + "/audio/"

    /**
     * 正在请求任务列表。
     */
    private val downCallMap: MutableMap<String, Call> = HashMap()

    /**
     * 下载url
     *
     * @param fileName 保存文件名
     * @param url 请求地址
     */
    fun download(
        ttsKey: String,
        url: String,
        listener: AudioProgressListener? = null
    ) {
        if (downCallMap.containsKey(url)) {
            Log.w(TAG, "$url is requesting")
            return
        }
        val file = File(audioDir + File.separator + ttsKey)
        if (!file.exists()) {
            file.parentFile?.mkdirs()
            val createResult = file.createNewFile()
            if (!createResult) {
                Log.e(TAG, "$url create ${file.path} fail")
                return
            }
        } else {
            //TODO 已缓存不会请求
            Log.w(TAG, "$url $file is exists")
        }
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        val call = okHttpClient.newCall(request)
        downCallMap[url] = call
        listener?.onStart()
        Log.i(TAG, "down start url:${url}")
        call.enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body()
                if (responseBody != null) {
                    val inputStream = responseBody.byteStream()
                    val contentLength = responseBody.contentLength()
                    val buffer = ByteArray(4096)
                    var bytesRead: Int
                    var totalBytesRead: Long = 0
                    val outputStream = FileOutputStream(file)
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead.toLong()
                        listener?.onProgress(
                            totalBytesRead,
                            contentLength,
                            totalBytesRead == contentLength
                        )
                        Log.i(
                            TAG,
                            "progress ${totalBytesRead / contentLength.toFloat()} url:${url}"
                        )
                    }
                    outputStream.flush()
                    outputStream.close()
                    listener?.onSuccess(ttsKey, file.path)
                    Log.w(TAG, "load success url:${url}")
                } else {
                    listener?.onError(-1, "Response body is null")
                    Log.w(TAG, "load error url:${url}")
                }
                downCallMap.remove(url)
            }

            override fun onFailure(call: Call, e: IOException) {
                listener?.onError(-1, e.message)
                downCallMap.remove(url)
                Log.w(TAG, "error url:${url}")
            }

        })
    }

    fun cancel(url: String) {
        val call = downCallMap[url]
        call?.cancel()
        downCallMap.remove(url)
    }

}

interface AudioProgressListener {

    fun onStart() {

    }

    fun onProgress(bytesRead: Long, contentLength: Long, done: Boolean) {

    }

    fun onSuccess(ttsKey: String, path: String)
    fun onError(code: Int, msg: String?)

}