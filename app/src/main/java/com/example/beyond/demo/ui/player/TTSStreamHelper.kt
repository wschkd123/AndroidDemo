package com.example.beyond.demo.ui.player

import android.util.Log
import androidx.annotation.WorkerThread
import com.example.base.AppContext
import com.example.base.util.HttpLogInterceptor
import com.example.base.util.JsonUtilKt
import com.example.base.util.YWFileUtil
import com.example.beyond.demo.ui.player.data.MediaDataSource
import com.example.beyond.demo.ui.player.data.TTSChunkResult
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
import java.util.concurrent.TimeUnit

/**
 * 通过sse协议实现文本流式输入语音流式输出（TTS）
 *
 * https://platform.minimaxi.com/document/guides/T2A-model/stream?id=65701c77024fd5d1dffbb8fe
 *
 * @author wangshichao
 * @date 2024/6/14
 */
class TTSStreamHelper(
    private var listener: TTSStreamListener
) {
    private val TAG = "ExoPlayerTTS"
    private val apiKey =
        "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJHcm91cE5hbWUiOiLkuIrmtbfnrZHmoqblspvkurrlt6Xmmbrog73np5HmioDmnInpmZDlhazlj7giLCJVc2VyTmFtZSI6ImNsaWVudHRlc3QiLCJBY2NvdW50IjoiY2xpZW50dGVzdEAxNzgyNTg4NTA5Njk4MTM0NDU1IiwiU3ViamVjdElEIjoiMTgwMTE5NDU2ODkwNTkyNDYwOSIsIlBob25lIjoiIiwiR3JvdXBJRCI6IjE3ODI1ODg1MDk2OTgxMzQ0NTUiLCJQYWdlTmFtZSI6IiIsIk1haWwiOiIiLCJDcmVhdGVUaW1lIjoiMjAyNC0wNi0xMyAyMToxNzoyMCIsImlzcyI6Im1pbmltYXgifQ.T-09xCHVDtou3vpO_gIxJW8dg9yOw8BQ_gIpDffhWWAzZb5R6Tv2Q6UJdMRxdPdCYWjqRnOBRS8dEf2Wu9rukhFY9CoDoeYQ7hNwB8472aoz67hJnv0420PlOXTV9VH5MB648lC0uYcdmOQ7-VH7MF5NSyvYr-rRvyL2UVJr2zyGlsS40ngzygoaIJK3ZmD7O-v1ko-JRBiFTFFfzb6Kp6lRnc20HKnK35gpJVY2OkmtoxxFCXm8rJvFuj0dlijmoeqKG8hS8f6JDpkybp1pqlwzOSg15f1rDstYOAtL8OYkYuJeNZFkZ9sUCPyqQPVkQhDJLZhJS9VaVzJmkLTpBw"
    private val format = "mp3"
    private val mediaType = MediaType.parse("application/json; charset=utf-8");
    private val okHttpClient: OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.MINUTES)
            .readTimeout(10, TimeUnit.MINUTES)
            .addNetworkInterceptor(HttpLogInterceptor())
            .build()
    private val ttsCompleteDir =
        YWFileUtil.getStorageFileDir(AppContext.application).path + "/tts/"
    private val ttsChunkDir =
        YWFileUtil.getStorageFileDir(AppContext.application).path + "/tts/chunk/"

    /**
     * ttsKey列表
     */
    private val requestSet = hashSetOf<String>()

    fun startConnect(
        content: String = "你好",
        ttsKey: String
    ) {
        Log.i(TAG, "ttsStreamFetch content:${content} ttsKey:${ttsKey}")

        //TODO 已缓存


        //TODO 正在请求
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
                "    \"format\": \"$format\"\n" +
                "  }"
        val requestBody = RequestBody.create(mediaType, json)
        val request = Request.Builder()
            .url("https://api.minimax.chat/v1/tts/stream?GroupId=1782588509698134455")
            .addHeader("accept", "application/json, text/plain, */*")
            .addHeader("Authorization", "Bearer $apiKey")
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
            Log.i(TAG, "chunk is null")
            return
        }
        val traceId = chunk.trace_id
        val audio = chunk.data?.audio
        val baseResp = chunk.base_resp
        if (baseResp?.isSuccess() != true) {
            listener?.onReceiveLimit(baseResp?.status_code ?: 0, baseResp?.status_msg ?: "")
            return
        }
        if (audio.isNullOrEmpty()) {
            Log.i(TAG, "audio is empty, trace_id:$traceId")
            return
        }

        val chunkPath = if (chunk.data.isLastComplete()) {
            //TODO 最后一个完整资源缓存，以
            val path = "$ttsCompleteDir$ttsKey.$format"
            Log.w(TAG, "parser complete content:${audio.length} path:$path ttsKey:${ttsKey}")
            saveAudioChunkToFile(audio, path)
        } else {
            val path = "$ttsChunkDir${chunk.trace_id}_${System.currentTimeMillis()}.$format"
            Log.i(TAG, "parser content:${audio.length} path:$path path:${ttsKey}")
            saveAudioChunkToFile(audio, path)
        }
        val mediaDataSource = MediaDataSource(
            traceId = chunk.trace_id,
            ttsKey = ttsKey,
            audioChunk = MediaDataSource.AudioChunk(
                chunkPath,
                chunk.data.isLastComplete(),
            )
        )
        listener?.onReceiveChunk(mediaDataSource)
    }

    private fun saveAudioChunkToFile(data: String, path: String): String {
        val byteArray = decodeHex(data)
        YWFileUtil.saveByteArrayToFile(byteArray, path)
        return path
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
     * 接收音频片段
     */
    @WorkerThread
    fun onReceiveChunk(dataSource: MediaDataSource)

    /**
     * 触发速率限制
     */
    @WorkerThread
    fun onReceiveLimit(code: Int, msg: String)
}
