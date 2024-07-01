package com.example.beyond.demo.ui.tts

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
    val ttsDir =
        YWFileUtil.getStorageFileDir(AppContext.application)?.path + "/tts/"

    /**
     * tts分片音频临时目录
     */
    val ttsChunkDir =
        YWFileUtil.getStorageFileDir(AppContext.application)?.path + "/tts/chunk/"

    /**
     * 检查是否存在缓存
     */
    fun checkCacheFileFromKey(ttsKey: String): File? {
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
     * 通过文件格式创建缓存路径
     */
    fun createCacheFileFromKey(ttsKey: String, format: String): File {
        return File("$ttsDir$ttsKey.$format")
    }

    /**
     * 通过key和url创建缓存路径
     */
    fun createCacheFileFromUrl(ttsKey: String, url: String): File {
        val fileFormat = YWFileUtil.getFormatFromUrl(url)
        return createCacheFileFromKey(ttsKey, fileFormat)
    }


}