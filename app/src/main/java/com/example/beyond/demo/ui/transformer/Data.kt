package com.example.beyond.demo.ui.transformer

import com.example.base.bean.IgnoreProguard
import com.google.gson.annotations.SerializedName


data class EditedItemChunk(
    val nickname: String? = null,
    val text: String? = null,
    val backgroundImage: String? = null,
    @SerializedName("audioAdress")
    val audioUrl: String? = null,
    /**
     * 音频时长（单位：秒）
     */
    val audioDuration: Long = 0,
    val avatar: String? = null,
    val msgId: String? = null,
    val senderId: String? = null,
    val senderType: Int = 0,
) : IgnoreProguard() {

    fun getDurationUs(): Long {
        val durationS = if (audioDuration > 0) audioDuration else (text?.length ?: 0L).toLong()
        return durationS * 1000000L
    }

    companion object {
        private const val ONE_ONE_AVATAR = "https://zmdcharactercdn.zhumengdao.com/2365d825482a71b62b59a7db80b88fa2.jpg"
        private const val THREE_THREE_AVATAR = "https://zmdcharactercdn.zhumengdao.com/34487524784424960048.png"
        private const val NINE_SIXTEEN_AVATAR = "https://zmdcharactercdn.zhumengdao.com/34459418686279680012.png"
        private const val TTS_SHORT = "asset:///media/mp3/short_tts.mp3"
        private const val TTS_LONG = "asset:///media/mp3/long_tts.mp3"
        fun mock(): List<EditedItemChunk> {
            return mutableListOf(
                EditedItemChunk(
                    "林泽林泽林泽",
                    "毒鸡汤大魔王",
                    ONE_ONE_AVATAR,
                    TTS_SHORT,
                    5
                ),
                EditedItemChunk(
                    "林泽林泽林泽",
                    "毒鸡汤大魔王，会收集负面情绪，贱贱毒舌却又心地善良的好哥哥，也是持之以恒、霸气侧漏的灵气复苏时代的最强王者、星图战神。\n" +
                            "吕树，别名为第九天罗，依靠毒鸡汤成为大魔王。身世成谜，自小在福利院中长大，16岁后脱离福利院，与吕小鱼相依为命，通过卖煮鸡蛋维持生计。擅长怼人、噎人、气人，却从不骂人。平时说话贱贱的，被京都天罗地网同仁称为“贱圣”，但从不骂人，喜欢用讲道理却不似道理的话怼人。\n" +
                            "无父无母，",
                    NINE_SIXTEEN_AVATAR,
                    TTS_LONG,
                    94
                )
            )
        }
    }
}