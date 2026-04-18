package com.example.beyond.demo.ui.alarm

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * 铃声辅助类 - 处理音频下载和闹钟设置
 *
 * @author wangshichao
 * @date 2026/4/16
 */
object RingtoneHelper {

    private const val TAG = "RingtoneHelper"
    private const val RINGTONE_DIR = "custom_ringtones"
    private const val DEFAULT_RINGTONE_URL = "https://zmdcharactercdn-new.zhumengdao.com/test/voice/20260417/mp3/57367262214076825692.mp3"

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /**
     * 获取默认铃声URL
     */
    fun getDefaultRingtoneUrl(): String = DEFAULT_RINGTONE_URL

    /**
     * 生成铃声文件名
     */
    fun generateFileName(): String = "ringtone_${System.currentTimeMillis()}.mp3"

    /**
     * 从URL下载音频文件到公共目录（系统闹钟可访问）
     * @param context 上下文
     * @param urlString 音频URL
     * @param fileName 保存的文件名
     * @return 下载后的文件URI（MediaStore content:// URI）
     */
    fun downloadToPublicDirectory(context: Context, urlString: String, fileName: String): Uri? {
        return try {
            Log.d(TAG, "开始下载: $urlString")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                downloadWithMediaStore(context, urlString, fileName)
            } else {
                downloadToLegacyPublicDir(context, urlString, fileName)
            }
        } catch (e: Exception) {
            Log.e(TAG, "下载失败: ${e.message}", e)
            null
        }
    }

    /**
     * Android 10+ 使用 MediaStore API 下载文件
     */
    private fun downloadWithMediaStore(context: Context, urlString: String, fileName: String): Uri? {
        // 检查是否已存在
        queryExistingAudio(context, fileName)?.let { return it }

        // 创建新文件条目
        val audioUri = createMediaStoreEntry(context, fileName) ?: return null

        // 下载并写入
        return try {
            downloadAndWrite(context, urlString, audioUri)
            Log.d(TAG, "下载完成: $audioUri")
            audioUri
        } catch (e: Exception) {
            context.contentResolver.delete(audioUri, null, null)
            throw e
        }
    }

    /**
     * 查询已存在的音频文件
     */
    private fun queryExistingAudio(context: Context, fileName: String): Uri? {
        val projection = arrayOf(MediaStore.Audio.Media._ID)
        val selection = "${MediaStore.Audio.Media.DISPLAY_NAME} = ?"

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            arrayOf(fileName),
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                return Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id.toString())
            }
        }
        return null
    }

    /**
     * 创建 MediaStore 条目
     */
    private fun createMediaStoreEntry(context: Context, fileName: String): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
            put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/$RINGTONE_DIR")
            put(MediaStore.Audio.Media.IS_RINGTONE, 1)
            put(MediaStore.Audio.Media.IS_ALARM, 1)
            put(MediaStore.Audio.Media.IS_NOTIFICATION, 0)
            put(MediaStore.Audio.Media.IS_MUSIC, 1)
        }
        return context.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
    }

    /**
     * 下载并写入数据（使用 OkHttp + 缓冲区）
     */
    private fun downloadAndWrite(context: Context, urlString: String, audioUri: Uri) {
        val request = Request.Builder().url(urlString).build()
        val response = okHttpClient.newCall(request).execute()

        if (!response.isSuccessful) {
            throw Exception("HTTP error: ${response.code()}")
        }

        response.body()?.byteStream()?.use { inputStream ->
            context.contentResolver.openOutputStream(audioUri)?.use { outputStream ->
                // 使用缓冲区写入，提高大文件性能
                val buffer = ByteArray(8192) // 8KB 缓冲区
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }
                outputStream.flush()
            }
        }
    }

    /**
     * Android 9及以下使用传统方式下载到公共目录
     */
    private fun downloadToLegacyPublicDir(context: Context, urlString: String, fileName: String): Uri? {
        val outputDir = getPublicMusicDir()
        val outputFile = File(outputDir, fileName)

        if (outputFile.exists()) {
            Log.d(TAG, "文件已存在: ${outputFile.absolutePath}")
            return scanFileToMediaStore(context, outputFile)
        }

        val request = Request.Builder().url(urlString).build()
        val response = okHttpClient.newCall(request).execute()

        if (!response.isSuccessful) {
            throw Exception("HTTP error: ${response.code()}")
        }

        response.body()?.byteStream()?.use { inputStream ->
            FileOutputStream(outputFile).use { outputStream ->
                // 使用缓冲区写入，提高大文件性能
                val buffer = ByteArray(8192) // 8KB 缓冲区
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }
                outputStream.flush()
            }
        }

        Log.d(TAG, "下载完成: ${outputFile.absolutePath}")
        return scanFileToMediaStore(context, outputFile)
    }

    /**
     * 获取公共音乐目录
     */
    private fun getPublicMusicDir(): File {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), RINGTONE_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * 扫描文件到 MediaStore（Android 9及以下）
     */
    private fun scanFileToMediaStore(context: Context, file: File): Uri? {
        var scannedUri: Uri? = null

        MediaScannerConnection.scanFile(
            context,
            arrayOf(file.absolutePath),
            arrayOf("audio/mpeg")
        ) { path, uri ->
            Log.d(TAG, "扫描完成: $path -> $uri")
            scannedUri = uri
        }

        // 等待扫描完成（最多3秒）
        repeat(30) {
            if (scannedUri != null) return@repeat
            Thread.sleep(100)
        }

        return scannedUri ?: Uri.fromFile(file)
    }
}
