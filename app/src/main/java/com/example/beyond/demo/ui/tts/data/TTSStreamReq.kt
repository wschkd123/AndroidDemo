package com.example.beyond.demo.ui.tts.data

import com.example.base.bean.IgnoreProguard

/**
 * 音频流式请求
 *
 * https://platform.minimaxi.com/document/guides/T2A-model/stream?id=65701c77024fd5d1dffbb8fe
 *
 * @author wangshichao
 * @date 2024/6/17
 */

/**
 * 流式语音生成请求入参
 */
data class TTSStreamReq(
    /**
     * 调用的模型版本
     */
    val model: String? = null,
    /**
     * 待合成的文本
     */
    val text: String? = null,
    /**
     * 音色编号
     */
    val voice_id: String? = null,
    /**
     * 音色相关信息
     */
    val timber_weights: List<TimberWeight?>? = null,
    /**
     * 生成声音的采样率
     */
    val audio_sample_rate: Int = 0,
    /**
     * 生成声音的比特率
     */
    val bitrate: Int = 0,
    /**
     * 生成的音频格式
     */
    val format: String? = null,
    /**
     * 生成声音的音量
     */
    val vol: Int = 0,
    /**
     * 生成声音的语调
     */
    val pitch: Int = 0,
    /**
     * 生成声音的语速
     */
    val speed: Int = 0,
) : IgnoreProguard() {
    data class TimberWeight(
        /**
         * 请求的音色编号
         */
        val voice_id: String? = null,
        /**
         * 权重
         */
        val weight: Int = 0
    ) : IgnoreProguard()
}

/**
 * 流式语音生成请求返回单个片段数据
 */
data class TTSChunkResult(
    val base_resp: BaseResp? = null,
    val `data`: Data? = null,
    val extra_info: ExtraInfo? = null,
    /**
     * 本次会话的id
     */
    val trace_id: String? = null,
    /**
     * 数据类型 // 1 完整url 2.音频片段
     */
    val type: Int = 0,
    /**
     * 完整音频地址
     */
    val url: String? = null,
) : IgnoreProguard() {
    fun isCompleteUrl() = type == 1
    data class BaseResp(
        /**
         * 1000，未知错误1001，超时1002，触发限流1004，鉴权失败1013，服务内部错误及非法字符超过10%2013，输入格式信息不正常
         */
        val status_code: Int = 0,
        val status_msg: String? = null
    ) : IgnoreProguard() {
        fun isSuccess() = status_code == 0
        /**
         * 登录态失效
         */
        fun isLoginInvalid() = status_code == -3

        /**
         * 触发速率限制
         * 1. 1041 conn limit
         * 2. 1002 rate limit
         */
        fun onRateLimit() = status_code == 1041 || status_code == 1002
    }

    data class Data(
        /**
         * 合成后的音频片段，采用hex编码，按照输入定义的格式进行生成（mp3/pcm/flac）
         */
        val audio: String? = null,
        val ced: String? = null,
        /**
         * 当前音频流状态，1表示合成中，2表示合成结束
         */
        val status: Int = 0
    ) : IgnoreProguard() {
        /**
         * 是否是最后一个完整资源
         */
        fun isLastComplete() = status == 2
    }

    data class ExtraInfo(
        /**
         * 音频时长，精确到毫秒
         */
        val audio_length: Int = 0,
        /**
         * 单位为字节
         */
        val audio_size: Int = 0,
        /**
         * 默认为24000，如客户请求参数进行调整，会根据请求参数生成
         */
        val audio_sample_rate: Int = 0,
        /**
         * 默认为168000，如客户请求参数进行调整，会根据请求参数生成
         */
        val bitrate: Int = 0,
        /**
         * 非法字符不超过10%（包含10%），音频会正常生成并返回非法字符占比；最大不超过0.1（10%），超过进行报错
         */
        val invisible_character_ratio: Int = 0,
        /**
         * 本次语音生成的计费字符数
         */
        val usage_characters: Int = 0,
        /**
         * 已经发音的字数统计（不算标点等其他符号，包含汉字数字字母）
         */
        val word_count: Int = 0
    ) : IgnoreProguard()
}