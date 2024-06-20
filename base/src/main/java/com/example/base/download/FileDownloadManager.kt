package com.example.base.download

import android.util.Log
import com.example.base.util.ThreadUtil
import com.example.base.util.YWFileUtil
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
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.MINUTES)
        .readTimeout(10, TimeUnit.MINUTES)
        .build()
    /**
     * 正在请求任务列表。
     */
    private val downCallMap: MutableMap<String, Call> = HashMap()


    /**
     * 下载url
     *
     * @param url 请求地址
     * @param savePath 保存的文件路径
     */
    fun download(
        url: String,
        savePath: String,
        listener: FileDownloadListener? = null
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
            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body()
                val file = saveResponseBodyToFile(responseBody, url, savePath, listener)
                downCallMap.remove(url)
                ThreadUtil.runOnUiThread {
                    if (file != null) {
                        listener?.onSuccess(url, file)
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



    @Throws(IOException::class)
    private fun saveResponseBodyToFile(
        responseBody: ResponseBody?,
        url: String,
        savePath: String,
        listener: FileDownloadListener?
    ): File? {
        if (responseBody == null) return null
        val tempPath = YWFileUtil.replacePathSuffix(savePath, "temp")
        val tempFile = YWFileUtil.createNewFile(tempPath) ?: return null
        val inputStream = responseBody.byteStream()
        val outputStream: OutputStream = FileOutputStream(tempFile)
        val contentLength = responseBody.contentLength()
        val buffer = ByteArray(8192)
        var bytesRead: Int
        var totalBytesRead = 0L
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            outputStream.write(buffer, 0, bytesRead)
            totalBytesRead += bytesRead.toLong()
            listener?.onProgress(url, totalBytesRead, contentLength, totalBytesRead == contentLength)
            Log.i(TAG, "progress:${bytesRead / totalBytesRead.toFloat()} url:${url}")
        }
        outputStream.close()
        inputStream.close()
        val finalFile = YWFileUtil.createNewFile(savePath) ?: return null
        val renameResult = tempFile.renameTo(finalFile)
        if (renameResult.not()) {
            Log.e(TAG, "${tempFile.path} renameTo $finalFile fail")
            return null
        }
        return finalFile
    }
}