package com.example.beyond.demo.ui.player

import android.util.Log
import com.example.base.AppContext
import com.example.base.download.AudioDownloadManager
import com.example.base.util.HttpLogInterceptor
import com.example.base.util.JsonUtilKt
import com.example.base.util.ThreadUtil
import com.example.base.util.YWFileUtil
import com.example.beyond.demo.ui.player.TTSStreamManager.startConnect
import com.example.beyond.demo.ui.player.data.MediaDataSource
import com.example.beyond.demo.ui.player.data.TTSChunkResult
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.internal.sse.RealEventSource
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okio.Buffer
import java.io.EOFException
import java.io.File
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

/**
 * 通过sse协议实现文本流式输入语音流式输出（TTS）
 *
 * 1. 单例设计。长连接在App生命周期中保持连接
 * 2. 功能包括tts流式请求、资源缓存 [startConnect]
 *
 * https://platform.minimaxi.com/document/guides/T2A-model/stream?id=65701c77024fd5d1dffbb8fe
 *
 * @author wangshichao
 * @date 2024/6/14
 */
object TTSStreamManager {
    private const val TAG = "ExoPlayerTTS"
    private const val API_KEY =
        "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJHcm91cE5hbWUiOiLkuIrmtbfnrZHmoqblspvkurrlt6Xmmbrog73np5HmioDmnInpmZDlhazlj7giLCJVc2VyTmFtZSI6ImNsaWVudHRlc3QiLCJBY2NvdW50IjoiY2xpZW50dGVzdEAxNzgyNTg4NTA5Njk4MTM0NDU1IiwiU3ViamVjdElEIjoiMTgwMTE5NDU2ODkwNTkyNDYwOSIsIlBob25lIjoiIiwiR3JvdXBJRCI6IjE3ODI1ODg1MDk2OTgxMzQ0NTUiLCJQYWdlTmFtZSI6IiIsIk1haWwiOiIiLCJDcmVhdGVUaW1lIjoiMjAyNC0wNi0xMyAyMToxNzoyMCIsImlzcyI6Im1pbmltYXgifQ.T-09xCHVDtou3vpO_gIxJW8dg9yOw8BQ_gIpDffhWWAzZb5R6Tv2Q6UJdMRxdPdCYWjqRnOBRS8dEf2Wu9rukhFY9CoDoeYQ7hNwB8472aoz67hJnv0420PlOXTV9VH5MB648lC0uYcdmOQ7-VH7MF5NSyvYr-rRvyL2UVJr2zyGlsS40ngzygoaIJK3ZmD7O-v1ko-JRBiFTFFfzb6Kp6lRnc20HKnK35gpJVY2OkmtoxxFCXm8rJvFuj0dlijmoeqKG8hS8f6JDpkybp1pqlwzOSg15f1rDstYOAtL8OYkYuJeNZFkZ9sUCPyqQPVkQhDJLZhJS9VaVzJmkLTpBw"
    private const val AUDIO_FORMAT = "mp3"
    private val mediaType = MediaType.parse("application/json; charset=utf-8");
    private val okHttpClient: OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.MINUTES)
            .readTimeout(10, TimeUnit.MINUTES)
            .addNetworkInterceptor(HttpLogInterceptor())
            .build()

    /**
     * tts完整音频目录
     */
    private val ttsCompleteDir =
        YWFileUtil.getStorageFileDir(AppContext.application).path + "/tts/"

    /**
     * tts分片音频目录
     */
    private val ttsChunkDir =
        YWFileUtil.getStorageFileDir(AppContext.application).path + "/tts/chunk/"
    private fun getCompletePath(ttsKey: String, format: String) = "$ttsCompleteDir$ttsKey.$format"

    /**
     * ttsKey列表。用于请求唯一标识，避免重复请求。
     */
    private val requestSet = hashSetOf<String>()
    var listener: TTSStreamListener? = null

    init {
        // 每次初始化删除片段缓存。考虑修改删除时机
        GlobalScope.launch {
            deleteAllChunkFile()
        }
    }

    fun startConnect(
        content: String = "你好",
        ttsKey: String
    ) {
        Log.i(TAG, "ttsStreamFetch content:${content} ttsKey:${ttsKey}")

        // 是否存在缓存
        val cachePath = getCompletePath(ttsKey, AUDIO_FORMAT)
        if (File(cachePath).exists()) {
            Log.w(TAG, "exist cache")
            listener?.onExistCache(ttsKey, cachePath)
            return
        }

        // 是否正在请求
        if (requestSet.contains(ttsKey)) {
            Log.w(TAG, "$content $ttsKey is requesting")
            return
        }
        requestSet.add(ttsKey)
        val json = "{\n" +
                "    \"timber_weights\": [\n" +
                "      {\n" +
                "        \"voice_id\": \"male-qn-qingse\",\n" +
                "        \"weight\": 1\n" +
                "      },\n" +
                "      {\n" +
                "        \"voice_id\": \"female-shaonv\",\n" +
                "        \"weight\": 1\n" +
                "      }\n" +
                "    ],\n" +
                "    \"text\": \"${content}\",\n" +
                "    \"voice_id\": \"\",\n" +
                "    \"model\": \"speech-01\",\n" +
                "    \"speed\": 1,\n" +
                "    \"vol\": 1,\n" +
                "    \"pitch\": 0,\n" +
                "    \"audio_sample_rate\": 32000,\n" +
                "    \"bitrate\": 128000,\n" +
                "    \"format\": \"$AUDIO_FORMAT\"\n" +
                "  }"
        val requestBody = RequestBody.create(mediaType, json)
        val request = Request.Builder()
            .url("https://api.minimax.chat/v1/tts/stream?GroupId=1782588509698134455")
            .addHeader("accept", "application/json, text/plain, */*")
            .addHeader("Authorization", "Bearer $API_KEY")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        val realEventSource = RealEventSource(request, object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: Response) {
                super.onOpen(eventSource, response)
                Log.i(TAG, "已连接")
            }

            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String
            ) {
                super.onEvent(eventSource, id, type, data)
                parserMessageContent(ttsKey, data)
            }

            override fun onClosed(eventSource: EventSource) {
                super.onClosed(eventSource)
                requestSet.remove(ttsKey)
                Log.i(TAG, "已断开")
            }

            override fun onFailure(
                eventSource: EventSource,
                t: Throwable?,
                response: Response?
            ) {
                super.onFailure(eventSource, t, response)
                requestSet.remove(ttsKey)
                Log.w(TAG, "连接失败 ${t?.message} ttsKey:$ttsKey")
                val data = readStringFromBuffer(response)
                parserMessageContent(ttsKey, data)
            }
        })
        realEventSource.connect(okHttpClient)

    }

    /**
     * 删除分片缓存临时文件夹
     */
    private fun deleteAllChunkFile() {
        //TODO 考虑子线程
        val startTime = System.currentTimeMillis()
        val deleteResult = File(ttsChunkDir).deleteRecursively()
        Log.w(TAG, "deleteChunkFile cost ${System.currentTimeMillis() - startTime} deleteResult:$deleteResult")
    }

    private fun readStringFromBuffer(response: Response?): String {
        val buffer: Buffer = response?.body()?.source()?.buffer ?: return ""
        val bufferSize = buffer.size()
        var body = ""
        try {
            body = buffer.readString(bufferSize, Charset.forName("UTF-8"))
        } catch (e: EOFException) {
            body += "\\n\\n--- Unexpected end of content ---"
        }
        return body
    }

    private fun parserMessageContent(
        ttsKey: String,
        data: String
    ) {
        val chunk = JsonUtilKt.toObject(data, TTSChunkResult::class.java)
        if (chunk == null) {
            Log.w(TAG, "chunk is null")
            ThreadUtil.runOnUiThread {
                listener?.onNetError("")
            }
            return
        }
        // 存在完整音频地址
        if (chunk.isCompleteUrl()) {
            val url = chunk.url ?: ""
            AudioDownloadManager.download(ttsKey, url)
            ThreadUtil.runOnUiThread {
                listener?.onReceiveCompleteUrl(ttsKey, url)
            }
            return
        }

        val baseResp = chunk.base_resp
        if (baseResp?.isSuccess() != true) {
            val code = baseResp?.status_code ?: 0
            val msg = baseResp?.status_msg ?: ""
            Log.w(TAG, "receive limit code:$code msg:$msg")
            ThreadUtil.runOnUiThread {
                listener?.onRateLimit(code, msg)
            }
            return
        }
        decodeAudioAndSave(chunk, ttsKey)
    }

    private fun decodeAudioAndSave(chunk: TTSChunkResult, ttsKey: String) {
        val traceId = chunk.trace_id
        val audio = chunk.data?.audio
        // 存在片段内容为空，直接忽略
        if (audio.isNullOrEmpty()) {
            Log.i(TAG, "audio is empty, trace_id:$traceId")
            return
        }

        // 最后一个完整资源以ttsKey缓存，其它片段保存临时文件
        val chunkPath = if (chunk.data.isLastComplete()) {
            getCompletePath(ttsKey, AUDIO_FORMAT)
        } else {
            "$ttsChunkDir${traceId}_${System.currentTimeMillis()}.$AUDIO_FORMAT"
        }
        Log.w(TAG, "parser content:${audio.length} path:$chunkPath ttsKey:${ttsKey}")
        val saveResult = YWFileUtil.saveByteArrayToFile(decodeHex(audio), chunkPath)
        if (saveResult) {
            val mediaDataSource = MediaDataSource(
                traceId = traceId,
                ttsKey = ttsKey,
                audioChunk = MediaDataSource.AudioChunk(
                    chunkPath,
                    chunk.data.isLastComplete(),
                )
            )
            ThreadUtil.runOnUiThread {
                listener?.onReceiveChunk(mediaDataSource)
            }
        } else {
            Log.e(TAG, "save fail path:$chunkPath ttsKey:${ttsKey}")
        }
    }

    private fun decodeHex(hexString: String): ByteArray {
        val byteArray = ByteArray(hexString.length / 2)
        var i = 0
        while (i < hexString.length) {
            val hex = hexString.substring(i, i + 2)
            byteArray[i / 2] = hex.toInt(16).toByte()
            i += 2
        }
        return byteArray
    }

}

interface TTSStreamListener {
    /**
     * 存在缓存
     */
    fun onExistCache(ttsKey: String, cachePath: String)

    /**
     * 完整音频地址
     */
    fun onReceiveCompleteUrl(ttsKey: String, url: String)

    /**
     * 音频片段
     */
    fun onReceiveChunk(dataSource: MediaDataSource)

    /**
     * 触发速率限制
     * 1. 1041 conn limit
     * 2. 1002 rate limit
     */
    fun onRateLimit(code: Int, msg: String)

    /**
     * 网络错误
     */
    fun onNetError(msg: String)
}
