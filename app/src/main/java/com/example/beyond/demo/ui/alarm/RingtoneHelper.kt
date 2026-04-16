package com.example.beyond.demo.ui.alarm

import android.content.Context
import android.net.Uri
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * 铃声辅助类 - 处理内置和URL音频文件
 *
 * @author wangshichao
 * @date 2026/4/16
 */
object RingtoneHelper {

    private const val RINGTONE_DIR = "custom_ringtones"

    /**
     * 获取自定义铃声目录
     */
    fun getRingtoneDir(context: Context): File {
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), RINGTONE_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * 从assets复制MP3文件到外部存储
     * @param context 上下文
     * @param assetFileName assets中的文件名
     * @return 复制后的文件URI
     */
    fun copyAssetToStorage(context: Context, assetFileName: String): Uri? {
        return try {
            val inputStream: InputStream = context.assets.open(assetFileName)
            val outputFile = File(getRingtoneDir(context), assetFileName)

            // 如果文件已存在,直接返回
            if (outputFile.exists()) {
                return Uri.fromFile(outputFile)
            }

            FileOutputStream(outputFile).use { outputStream ->
                val buffer = ByteArray(4096)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }
                outputStream.flush()
            }
            inputStream.close()

            Uri.fromFile(outputFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 从URL下载音频文件
     * @param context 上下文
     * @param urlString 音频URL
     * @param fileName 保存的文件名
     * @return 下载后的文件URI
     */
    fun downloadFromUrl(context: Context, urlString: String, fileName: String): Uri? {
        return try {
            val outputFile = File(getRingtoneDir(context), fileName)

            // 如果文件已存在,直接返回
            if (outputFile.exists()) {
                return Uri.fromFile(outputFile)
            }

            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 30000
            connection.readTimeout = 30000
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw Exception("HTTP error code: ${connection.responseCode}")
            }

            connection.inputStream.use { inputStream ->
                FileOutputStream(outputFile).use { outputStream ->
                    val buffer = ByteArray(4096)
                    var bytesRead: Int
                    var totalBytesRead = 0L
                    val fileSize = connection.contentLengthLong

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                    }
                    outputStream.flush()
                }
            }

            connection.disconnect()
            Uri.fromFile(outputFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 获取所有可用的内置铃声列表
     */
    fun getAvailableBuiltInRingtones(): List<RingtoneInfo> {
        return listOf(
            RingtoneInfo("test.mp3", "测试铃声", true),
            RingtoneInfo("long_tts.mp3", "长语音", true),
            RingtoneInfo("short_tts.mp3", "短语音", true),
            RingtoneInfo("placeholder.mp3", "占位符", true)
        )
    }

    /**
     * 铃声信息数据类
     */
    data class RingtoneInfo(
        val fileName: String,
        val displayName: String,
        val isBuiltIn: Boolean = false,
        val url: String? = null
    )
}
