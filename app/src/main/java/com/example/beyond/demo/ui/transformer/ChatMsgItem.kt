package com.example.beyond.demo.ui.transformer

import android.text.TextUtils
import com.example.base.bean.IgnoreProguard
import com.example.beyond.demo.R
import com.google.gson.annotations.SerializedName

/**
 * 用于生成视频的聊天消息Item
 *
 * @author wangshichao
 * @date 2024/7/30
 */
data class ChatMsgItem(
    var nickname: String? = null,
    val text: String? = null,
    @SerializedName("backgroundImage")
    var backgroundUrl: String? = null,
    @SerializedName("audioAddress")
    val audioUrl: String? = null,
    /**
     * 音频时长（单位：毫秒）
     */
    val audioDuration: Long = 0,
    /**
     * 发送类型，1-用户 2-人物 3-主控卡牌 4-私密主控
     */
    val senderType: Int = 0,
    val avatar: String? = null,
    val msgId: String? = null,
    val senderId: String? = null,
) : IgnoreProguard() {

    /**
     * 生成item长度。单位us
     */
    fun getDurationUs(): Long {
        if (havaAudio()) {
            return audioDuration * 1_000L
        }
        // 无语音时，一个文字0.12s，最短总共1.2s
        var textDurationS = (text?.length ?: 0L).toLong().times(0.12f)
        if (textDurationS < 1.2) {
            textDurationS = 1.2f
        }
        return textDurationS.times(1_000_000).toLong()
    }

    fun havaAudio() = audioDuration > 0 && !TextUtils.isEmpty(audioUrl)

    fun isUser() = senderType != 2

    fun getChatBoxBgResId(): Int {
        return when  {
            isUser() -> R.drawable.user_text_bg
            else -> R.drawable.character_text_bg
        }
    }

    companion object {
        private const val ONE_ONE_AVATAR =
            "https://zmdcharactercdn.zhumengdao.com/2365d825482a71b62b59a7db80b88fa2.jpg"
        private const val THREE_THREE_AVATAR =
            "https://zmdcharactercdn.zhumengdao.com/34487524784424960048.png"
        private const val NINE_SIXTEEN_AVATAR =
            "https://zmdcharactercdn.zhumengdao.com/34459418686279680012.png"
        private const val TTS_SHORT = "asset:///short_tts.mp3"
        private const val TTS_LONG = "asset:///long_tts.mp3"

        fun getChatList(): List<ChatMsgItem> {
            val list = mock()
            list.forEachIndexed { index, chatMsg ->
                // 用户发言时，仅切换对话文本框，不切换背景
                if (index > 0 && chatMsg.isUser()) {
                    chatMsg.backgroundUrl = list.get(index - 1).backgroundUrl
                }
                // 用户发言显示“我”
                if (chatMsg.isUser()) {
                    chatMsg.nickname = "我"
                }
            }
            return list
        }
        private fun mock(): List<ChatMsgItem> {
            return mutableListOf(
                ChatMsgItem(
                    "林泽林泽林泽",
                    "毒鸡汤大魔王",
                    ONE_ONE_AVATAR,
                    TTS_SHORT,
                    5000,
                    senderType = 2
                ),
                ChatMsgItem(
                    "beyond",
                    "哈哈哈哈",
                    senderType = 1
                ),
                ChatMsgItem(
                    "林泽林泽林泽",
                    "毒鸡汤大魔王，会收集负面情绪，贱贱毒舌却又心地善良的好哥哥，也是持之以恒、霸气侧漏的灵气复苏时代的最强王者、星图战神。\n" +
                            "吕树，别名为第九天罗，依靠毒鸡汤成为大魔王。身世成谜，自小在福利院中长大，16岁后脱离福利院，与吕小鱼相依为命，通过卖煮鸡蛋维持生计。擅长怼人、噎人、气人，却从不骂人。平时说话贱贱的，被京都天罗地网同仁称为“贱圣”，但从不骂人，喜欢用讲道理却不似道理的话怼人。无父无母，从小吃了了",
                    NINE_SIXTEEN_AVATAR,
                    TTS_LONG,
                    94000,
                    senderType = 2
                )
            )
        }
    }
}