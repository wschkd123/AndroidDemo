package com.example.base.download

import android.util.Log
import com.example.base.util.HttpLogInterceptor
import com.example.base.util.ThreadUtil
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Okio
import java.io.File
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
    private val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.MINUTES)
            .readTimeout(10, TimeUnit.MINUTES)
            .addNetworkInterceptor(HttpLogInterceptor())
            .build()
    /**
     * 正在请求任务列表。
     */
    private val downCallMap: MutableMap<String, Call> = HashMap()

    /**
     * 下载临时文件
     */
    private fun getTempFile(ttsKey: String) = TTSFileUtil.getCacheFile(ttsKey, "temp")

    /**
     * 下载url
     *
     * @param url 请求地址
     * @param fileName 保存文件名
     */
    fun download(
        url: String,
        fileName: String,
        listener: AudioProgressListener? = null
    ) {
        if (downCallMap.containsKey(url)) {
            Log.w(TAG, "$url is requesting")
            return
        }
        val request = Request.Builder()
            .url(url)
            .build()
        val call = okHttpClient.newCall(request)
        downCallMap[url] = call
        Log.w(TAG, "down start url:${url}")
        call.enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body()
                saveResponseBodyToFile(responseBody, url, fileName, listener)
                downCallMap.remove(url)
            }

            override fun onFailure(call: Call, e: IOException) {
                Log.w(TAG, "error url:${url}")
                downCallMap.remove(url)
                ThreadUtil.runOnUiThread {
                    listener?.onError(url, e.message)
                }
            }

        })
    }

    fun cancel(url: String) {
        val call = downCallMap[url]
        call?.cancel()
        downCallMap.remove(url)
    }

    private fun createSaveFile(url: String, fileName: String): File? {
        val fileFormat = url.substring(url.lastIndexOf('.') + 1)
        val file = TTSFileUtil.getCacheFile(fileName, fileFormat)
        if (!file.exists()) {
            file.parentFile?.mkdirs()
            val createResult = file.createNewFile()
            if (!createResult) {
                Log.e(TAG, "$url create ${file.path} fail")
                return null
            }
        } else {
            Log.e(TAG, "$url $file is exists")
        }
        return file
    }

    private fun saveResponseBodyToFile(
        responseBody: ResponseBody?,
        url: String,
        fileName: String,
        listener: AudioProgressListener?
    ) {
        val tempFile = getTempFile(fileName);
        if (responseBody != null) {
            try {
                responseBody.source().use { source ->
                    Okio.buffer(Okio.sink(tempFile)).use { sink ->
                        var totalBytesRead: Long = 0
                        val contentLength: Long = responseBody.contentLength()
                        var bytesRead: Long
                        while (source.read(sink.buffer(), 8192).also { bytesRead = it } != -1L) {
                            sink.emitCompleteSegments()
                            totalBytesRead += bytesRead
//                            callback.onProgress(url, totalBytesRead, contentLength)
                            Log.i(TAG, "onProgress totalBytesRead:${totalBytesRead}")
                        }
                        sink.flush()
                        val file = createSaveFile(url, fileName) ?: return
                        tempFile.renameTo(file)
                        listener?.onSuccess(url, fileName, file.path)
                    }
                }
            } catch (e: IOException) {
                listener?.onError(url, "文件写入失败: " + e.message)
            }
        } else {
            listener?.onError(url, "文件内容为空")
        }
    }

}

interface AudioProgressListener {
    fun onSuccess(url: String, fileName: String, path: String)
    fun onError(url: String, msg: String?)
}