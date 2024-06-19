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
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.concurrent.TimeUnit

/**
 * 文件下载管理
 *
 * @author wangshichao
 * @date 2024/6/19
 */
object FileDownloadManager {
    private const val TAG = "FileDownloadManager"
    private var okHttpClient = OkHttpClient.Builder()
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
        listener: FileDownloadListener? = null
    ) {
        if (downCallMap.containsKey(url)) {
            Log.w(TAG, "$url is requesting")
            return
        }
        val request = Request.Builder()
            .url(url)
            .build()
        // 进度监听
        okHttpClient = okHttpClient.newBuilder()
            .addNetworkInterceptor { chain ->
                val originalResponse = chain.proceed(chain.request())
                originalResponse.newBuilder()
                    .body(DownloadResponseBody(originalResponse.body()!!, listener, url))
                    .build()
            }
            .build()
        val call = okHttpClient.newCall(request)
        downCallMap[url] = call
        Log.w(TAG, "down start url:${url}")
        call.enqueue(object : Callback {
            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body()
                val file = saveResponseBodyToFile(responseBody, url, fileName)
                downCallMap.remove(url)
                ThreadUtil.runOnUiThread {
                    if (file != null) {
                        listener?.onSuccess(url, fileName, file)
                    } else {
                        listener?.onFail(url, "文件内容为空")
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                Log.w(TAG, "error url:${url}")
                downCallMap.remove(url)
                ThreadUtil.runOnUiThread {
                    listener?.onFail(url, e.message ?: "")
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

    @Throws(IOException::class)
    private fun saveResponseBodyToFile(responseBody: ResponseBody?, url: String, fileName: String): File? {
        if (responseBody == null) return null
        val tempFile = getTempFile(fileName)
        val inputStream = responseBody.byteStream()
        val outputStream: OutputStream = FileOutputStream(tempFile)
        val buffer = ByteArray(8192)
        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            outputStream.write(buffer, 0, bytesRead)
        }
        outputStream.close()
        inputStream.close()
        val finalFile = createSaveFile(url, fileName) ?: return null
        tempFile.renameTo(finalFile)
        return finalFile
    }
}