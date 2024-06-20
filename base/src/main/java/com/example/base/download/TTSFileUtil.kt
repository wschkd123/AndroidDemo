package com.example.base.download

import com.example.base.AppContext
import com.example.base.util.YWFileUtil
import java.io.File

/**
 * 音频支持 mp3,pcm,flac 格式
 * 
 * @author wangshichao
 * @date 2024/6/19
 */
object TTSFileUtil {

    private val formatList = mutableListOf("mp3", "pcm", "flac")

    /**
     * tts完整音频目录
     */
    private val ttsDir =
        YWFileUtil.getStorageFileDir(AppContext.application)?.path + "/tts/"

    /**
     * tts分片音频临时目录
     */
    val ttsChunkDir =
        YWFileUtil.getStorageFileDir(AppContext.application)?.path + "/tts/chunk/"

    /**
     * 不知道格式，获取缓存文件
     */
    fun getCacheFile(ttsKey: String): File? {
        formatList.forEach { format ->
            val path = "$ttsDir$ttsKey.$format"
            val file = File(path)
            if (file.exists()) {
                return file
            }
        }
        return null
    }

    /**
     * 指定格式，获取缓存文件
     */
    fun getCacheFile(ttsKey: String, format: String): File {
        return File("$ttsDir$ttsKey.$format")
    }
}