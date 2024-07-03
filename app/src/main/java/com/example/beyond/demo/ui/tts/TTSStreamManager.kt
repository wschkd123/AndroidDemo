package com.example.beyond.demo.ui.tts

import android.util.Log
import com.example.base.download.FileDownloadManager
import com.example.base.util.HttpLogInterceptor
import com.example.base.util.JsonUtilKt
import com.example.base.util.ThreadUtil
import com.example.base.util.YWFileUtil
import com.example.beyond.demo.ui.tts.TTSStreamManager.startConnect
import com.example.beyond.demo.ui.tts.data.ChunkDataSource
import com.example.beyond.demo.ui.tts.data.TTSChunkResult
import com.example.beyond.demo.ui.tts.mock.AudioData
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
import java.nio.charset.Charset
import java.util.concurrent.ConcurrentHashMap
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
//            .addNetworkInterceptor(HttpLogInterceptor())
            .build()

    /**
     * ttsKey列表。用于请求唯一标识，避免重复请求。
     */
    private val requestMap = ConcurrentHashMap<String, RealEventSource>()
    private val ttsStreamListenerList: MutableList<TTSStreamListener> = mutableListOf()

    fun startWithCompleteData(ttsKey: String, content: String) {
        Log.w(TAG, "startWithCompleteData content:${content} ttsKey:${ttsKey}")
        val audioArrayList = mutableListOf(
            AudioData.audioComplete,
            ""
        )
        audioArrayList.forEach {
            val byteArray = decodeHex(it)
            receiveChunk(byteArray, ttsKey)
        }
    }

    fun startWithMockData(ttsKey: String, content: String) {
        val audioArrayList = mutableListOf(
            AudioData.audio1,
            AudioData.audio2,
            AudioData.audio3,
            AudioData.audio4,
            AudioData.audio5,
            AudioData.audio6,
            AudioData.audio7,
            AudioData.audio8,
            AudioData.audio7,
            AudioData.audio7,
            ""
        )
        val chunkSb = StringBuilder()
        for (i in 0..20) {
            chunkSb.append(AudioData.audioComplete)
        }
        for (i in 0..10) {
            val byteArray = decodeHex(chunkSb.toString())
            receiveChunk(byteArray, ttsKey)
        }

        // 合成结束，回调空数据
        receiveChunk(ByteArray(0), ttsKey)
    }

    private fun receiveChunk(byteArray: ByteArray, ttsKey: String) {
        // 音频片段保存在临时文件，然后回调路径等信息
        Log.d(TAG, "receiveChunk: threadName=" + Thread.currentThread().name)
        Log.i(TAG, "receiveChunk content:${byteArray.size / 1000} kb")
        ttsStreamListenerList.forEach {
            it.onReceiveChunk(
                ChunkDataSource(
                    traceId = "",
                    ttsKey = ttsKey,
                    byteArray
                )
            )
        }
    }

    /**
     * 取消连接
     */
    fun cancelConnect(ttsKey: String?) {
        ttsKey ?: return
        if (requestMap.containsKey(ttsKey).not()) {
            return
        }
        Log.w(TAG, "cancelConnect ttsKey=$ttsKey")
        requestMap[ttsKey]?.cancel()
        requestMap.remove(ttsKey)
    }

    fun addTTSStreamListener(listener: TTSStreamListener) {
        if (!ttsStreamListenerList.contains(listener)) {
            ttsStreamListenerList.add(listener)
            Log.i(TAG, "add $listener size: ${ttsStreamListenerList.size}")
        }
    }

    fun removeTTSStreamListener(listener: TTSStreamListener) {
        Log.i(TAG, "remove $listener")
        ttsStreamListenerList.remove(listener)
    }

    /**
     * 开始连接
     */
    fun startConnect(
        ttsKey: String,
        content: String,
    ) {
        // 是否正在请求
        if (requestMap.containsKey(ttsKey)) {
            Log.w(TAG, "is requesting content=$content ttsKey=$ttsKey")
            return
        }
        Log.w(TAG, "startConnect content=${content} ttsKey=${ttsKey}")
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
                requestMap.remove(ttsKey)
                Log.i(TAG, "已断开")
            }

            override fun onFailure(
                eventSource: EventSource,
                t: Throwable?,
                response: Response?
            ) {
                super.onFailure(eventSource, t, response)
                requestMap.remove(ttsKey)
                Log.w(TAG, "连接失败 ${t?.message} ttsKey=$ttsKey")
                val data = readStringFromBuffer(response)
                parserMessageContent(ttsKey, data)
            }
        })
        requestMap[ttsKey] = realEventSource
        realEventSource.connect(okHttpClient)

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
            netErrorOnUiThread(ttsKey)
            return
        }

        // 不同网络状态处理
        val baseResp = chunk.base_resp
        val code = baseResp?.status_code ?: -1
        val msg = baseResp?.status_msg
        when {
            // 后端调用minimax出错 {"type":2,"url":null,"baseResp":null,"data":null,"extraInfo":null,"traceId":null}
            baseResp == null -> {
                Log.w(TAG, "baseResp is null $chunk")
                netErrorOnUiThread(ttsKey)
            }

            // 登录态失效
            baseResp.isLoginInvalid() -> {
                Log.w(TAG, "login invalid code=$code msg=$msg")
                ThreadUtil.runOnUiThread {
                    ttsStreamListenerList.forEach {
                        it.onLoginInvalid(ttsKey, code, msg ?: "")
                    }
                }
            }

            // minimax触发速率限制等错误
            baseResp.onRateLimit() -> {
                Log.w(TAG, "receive limit code=$code msg=$msg")
                ThreadUtil.runOnUiThread {
                    ttsStreamListenerList.forEach {
                        it.onRateLimit(ttsKey, code, msg ?: "")
                    }
                }
            }

            // 成功
            baseResp.isSuccess() -> {
                if (chunk.isCompleteUrl()) {
                    // 存在完整音频地址
                    val url = chunk.url ?: ""
                    Log.w(TAG, "server exist cache, play and download $url")
                    val file = TTSFileUtil.createCacheFileFromUrl(ttsKey, url)
                    FileDownloadManager.download(url, file.path)
                    ThreadUtil.runOnUiThread {
                        ttsStreamListenerList.forEach {
                            it.onReceiveCompleteUrl(ttsKey, url)
                        }
                    }
                } else {
                    // 解码音频片段
                    decodeAudioChunkAndSave(chunk, ttsKey)
                }
            }

            else -> {
                Log.w(TAG, "net error code=$code msg=$msg")
                netErrorOnUiThread(ttsKey, msg ?: "")
            }

        }
    }

    /**
     * 解码音频片段并缓存
     */
    private fun decodeAudioChunkAndSave(chunk: TTSChunkResult, ttsKey: String) {
        val traceId = chunk.trace_id
        val audio = chunk.data?.audio
        // 存在片段内容为空，直接忽略
        if (audio.isNullOrEmpty()) {
            Log.i(TAG, "audio is empty, trace_id=$traceId")
            return
        }

        // 解码源数据为字节数组
        val decodeData = decodeHex(audio)

        if (chunk.data.isLastComplete()) {
            // 合成结束，回调空数据
            ThreadUtil.runOnUiThread {
                ttsStreamListenerList.forEach {
                    it.onReceiveChunk(
                        ChunkDataSource(
                            traceId = traceId,
                            ttsKey = ttsKey,
                            audioData = ByteArray(0)
                        )
                    )
                }
            }
            // 最后一个完整音频缓存下来
            val chunkPath = TTSFileUtil.createCacheFileFromKey(ttsKey, AUDIO_FORMAT).path
            YWFileUtil.saveByteArrayToFile(decodeData, chunkPath)
            Log.i(TAG, "parser last content=${audio.length} path=$chunkPath")
        } else {
            // 音频片段回调给业务方播放
            Log.i(TAG, "parser content=${audio.length}")
            ThreadUtil.runOnUiThread {
                ttsStreamListenerList.forEach {
                    it.onReceiveChunk(
                        ChunkDataSource(
                            traceId = traceId,
                            ttsKey = ttsKey,
                            audioData = decodeData
                        )
                    )
                }
            }
        }

    }

    /**
     * 解码十六进制数据
     */
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

    private fun netErrorOnUiThread(ttsKey: String, msg: String? = null) {
        ThreadUtil.runOnUiThread {
            ttsStreamListenerList.forEach {
                it.onNetError(ttsKey, msg)
            }
        }
    }

}

interface TTSStreamListener {

    /**
     * 完整音频地址
     */
    fun onReceiveCompleteUrl(ttsKey: String, url: String)

    /**
     * 音频片段
     */
    fun onReceiveChunk(dataSource: ChunkDataSource)

    /**
     * 触发速率限制
     * 1. 1041 conn limit
     * 2. 1002 rate limit
     */
    fun onRateLimit(ttsKey: String, code: Int, msg: String)

    /**
     * 登录态失效
     */
    fun onLoginInvalid(ttsKey: String, code: Int, msg: String)

    /**
     * 网络错误
     */
    fun onNetError(ttsKey: String, msg: String?)
}
