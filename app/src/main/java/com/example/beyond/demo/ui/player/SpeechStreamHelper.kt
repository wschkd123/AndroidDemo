package com.example.beyond.demo.ui.player

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.base.util.HttpLogInterceptor
import com.example.base.util.JsonUtilKt
import com.example.base.util.YWFileUtil
import com.example.beyond.demo.ui.player.data.AudioChunkResult
import com.example.beyond.demo.ui.player.data.MediaDataSource
import com.yuewen.baseutil.ext.getActivity
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
 * 通过sse协议实现文本流式输入语音流式输出
 *
 * https://platform.minimaxi.com/document/guides/T2A-model/stream?id=65701c77024fd5d1dffbb8fe
 *
 * @author wangshichao
 * @date 2024/6/14
 */
class SpeechStreamHelper(
    private val context: Context,
    private val listener: SpeechStreamListener? = null
) {
    private val TAG = "ExoHelper"
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

    private var chunkIndex = 0


    fun loadData(content: String = "你好") {
        chunkIndex = 0
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
                processMessageContent(data)
            }

            override fun onClosed(eventSource: EventSource) {
                super.onClosed(eventSource)
                Log.i(TAG, "已断开")
            }

            override fun onFailure(
                eventSource: EventSource,
                t: Throwable?,
                response: Response?
            ) {
                super.onFailure(eventSource, t, response)
                Log.i(TAG, "连接失败 ${t?.message}")
                val data = readStringFromBuffer(response)
                processMessageContent(data)
            }
        })
        realEventSource.connect(okHttpClient)

    }

    private fun readStringFromBuffer(response: Response?): String {
        val buffer: Buffer = response?.body()?.source()?.buffer()?:return ""
        val bufferSize = buffer.size()
        var body = ""
        try {
            body = buffer.readString(bufferSize, Charset.forName("UTF-8"))
        } catch (e: EOFException) {
            body += "\\n\\n--- Unexpected end of content ---"
        }
        return body
    }

    private fun processMessageContent(data: String) {
        val chunk = JsonUtilKt.toObject(data, AudioChunkResult::class.java)
        if (chunk == null) {
            Log.i(TAG, "chunk is null")
            return
        }
        val traceId = chunk.trace_id
        if (chunk.base_resp.isSuccess().not()) {
            Log.w(TAG, "chunk is fail, code:${chunk.base_resp.status_code} msg:${chunk.base_resp.status_msg} trace_id:$traceId")
            context.getActivity()?.runOnUiThread {
                Toast.makeText(context, "您点的太快啦", Toast.LENGTH_SHORT).show()
            }
            return
        }
        if (chunk.data.audio.isNullOrEmpty()) {
            Log.i(TAG, "audio is null, trace_id:$traceId")
            return
        }

        context.getActivity()?.runOnUiThread {
        val chunkPath = if (chunk.data.isEnd()) {
            //TODO 缓存
            Log.w(TAG, "content:${chunk.data.audio.length} end, trace_id:$traceId")
            saveAudioLocal(chunk.data.audio, chunk.trace_id)
        } else {
            Log.i(TAG, "content:${chunk.data.audio.length}, trace_id:$traceId")
            saveAudioLocal(chunk.data.audio, chunk.trace_id + "_" + chunkIndex)
        }
        listener?.onReceiveChunk(
            MediaDataSource(
                chunk.trace_id,
                chunkPath,
                format,
                chunk.data.isEnd(),
                chunkIndex
            )
        )
        chunkIndex++
        }
    }

    private fun saveAudioLocal(data: String, key: String): String {
        val byteArray = decodeHex(data)
        val path = YWFileUtil.getStorageFileDir(context).path + "/" + key + ".mp3"
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

interface SpeechStreamListener {
    fun onReceiveChunk(dataSource: MediaDataSource)
}
