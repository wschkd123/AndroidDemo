package com.example.beyond.demo.ui.transformer

import android.text.TextUtils
import android.util.Log
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
     * 发送类型，1-用户（群聊用户） 2-人物 3-主控卡牌 4-私密主控（单聊用户）
     */
    val senderType: Int = 0,
    val avatar: String? = null,
    val msgId: String? = null,
    val senderId: String? = null,

    /**
     * 背景进入动画
     */
    var bgInAnimation: Boolean = true,
    /**
     * 背景退出动画
     */
    var bgOutAnimation: Boolean = true,
    /**
     * 文本框进入动画
     */
    var textBoxInAnimation: Boolean = true,
    /**
     * 文本框退出动画
     */
    var textBoxOutAnimation: Boolean = true
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
        private const val TAG = "ChatMsgItem-Overlay"
        private const val ONE_ONE_AVATAR =
            "https://zmdcharactercdn.zhumengdao.com/2365d825482a71b62b59a7db80b88fa2.jpg"
        private const val THREE_THREE_AVATAR =
            "https://zmdcharactercdn.zhumengdao.com/34487524784424960048.png"
        private const val NINE_SIXTEEN_AVATAR =
            "https://zmdcharactercdn.zhumengdao.com/34459418686279680012.png"
        private const val TTS_SHORT = "asset:///short_tts.mp3"
        private const val TTS_LONG = "asset:///long_tts.mp3"

        /**
         * 生成视频数据组装。包括图片背景和动画逻辑
         */
        @JvmStatic
        fun convertList(singleChat: Boolean = false): List<ChatMsgItem> {
            val list = mock()
            list.forEachIndexed { index, chatMsg ->
                // 用户发言显示“我”
                if (chatMsg.isUser()) {
                    chatMsg.nickname = "我"
                }

                // 第一个背景无进入动画
                if (index == 0) {
                    chatMsg.bgInAnimation = false
                }

                // 最后一个背景和文本框无退出动画
                if (index == list.size - 1) {
                    chatMsg.bgOutAnimation = false
                    chatMsg.textBoxOutAnimation = false
                }

                if (chatMsg.isUser()) {
                    adapterUserMsgItem(list, chatMsg, index)
                } else {
                    // 上一条消息是同一个梦中人，气泡位置不变。气泡无进入动画，背景无进出动画。
                    if (index > 0) {
                        val preChatMsg = list.getOrNull(index - 1)
                        if (preChatMsg != null && preChatMsg.senderId == chatMsg.senderId) {
                            preChatMsg.bgOutAnimation = false
                            preChatMsg.textBoxOutAnimation = false
                            chatMsg.bgInAnimation = false
                            chatMsg.textBoxInAnimation = false
                        }
                    }
                }

                // 单聊固定单个梦中人图片为背景。无背景动画
                if (singleChat) {
                    chatMsg.bgInAnimation = false
                    chatMsg.bgOutAnimation = false
                }

                Log.d(TAG, "final chatMsg=$chatMsg")
            }
            return list
        }

        /**
         * 查找第一梦中人
         */
        fun findFirstCharacter(list: List<ChatMsgItem>): ChatMsgItem? {
            return list.firstOrNull { it.isUser().not() }
        }

        /**
         * 适配用户消息数据。包括图片背景和动画逻辑
         */
        private fun adapterUserMsgItem(list: List<ChatMsgItem>, chatMsg: ChatMsgItem, index: Int) {
            if (chatMsg.isUser().not()) {
                return
            }

            // 向前查
            if (index > 0) {
                // 向前查最近梦中人消息，处理背景和动画特殊逻辑
                findPreNearestCharacter(list, index)?.let { preNearestCharacter ->
                    // 最近梦中人消息背景无退出动画
                    preNearestCharacter.bgOutAnimation = false
                    // 用户使用梦中人消息背景，背景无进入动画
                    chatMsg.backgroundUrl = preNearestCharacter.backgroundUrl
                    chatMsg.bgInAnimation = false
                }

                // 上一条消息也是用户，气泡位置不变。气泡无进入动画，背景无进出动画。
                val preChatMsg = list.getOrNull(index - 1)
                if (preChatMsg?.isUser() == true) {
                    preChatMsg.bgOutAnimation = false
                    preChatMsg.textBoxOutAnimation = false
                    chatMsg.bgInAnimation = false
                    chatMsg.textBoxInAnimation = false
                }
            }

            // 前面n条都为用户时，使用第一个梦中人背景
            if (chatMsg.backgroundUrl.isNullOrEmpty()) {
                chatMsg.backgroundUrl = findFirstCharacter(list)?.backgroundUrl
            }
        }

        /**
         * 向前查最近梦中人消息
         */
        private fun findPreNearestCharacter(list: List<ChatMsgItem>, curIndex: Int): ChatMsgItem? {
            var index = curIndex
            var preCharacter: ChatMsgItem?
            while (index > 0) {
                index--
                preCharacter = list.getOrNull(index)
                if (preCharacter?.isUser() == false) {
                    return preCharacter
                }
            }
            return null
        }

        private fun mock(): List<ChatMsgItem> {
            return mutableListOf(
                ChatMsgItem(
                    "吴雨欣",
                    "今天天气不错，心情也很好吧？",
                    ONE_ONE_AVATAR,
                    "https://zmdcharactercdn.zhumengdao.com/mp3/35066344422507724820.mp3",
                    3455,
                    senderType = 2,
                    senderId = "61425628576936",
                ),
//                ChatMsgItem(
//                    "林泽林泽林泽",
//                    "毒鸡汤大魔王1",
//                    ONE_ONE_AVATAR,
//                    TTS_SHORT,
//                    5000,
//                    senderType = 2,
//                    senderId = "1",
//                ),
//                ChatMsgItem(
//                    "爱莉希雅",
//                    "毒鸡汤大魔王，会收集负面情绪，贱贱毒舌却又心地善良的好哥哥，也是持之以恒、霸气侧漏的灵气复苏时代的最强王者、星图战神。\n" +
//                            "吕树，别名为第九天罗，依靠毒鸡汤成为大魔王。身世成谜，自小在福利院中长大，16岁后脱离福利院，与吕小鱼相依为命，通过卖煮鸡蛋维持生计。擅长怼人、噎人、气人，却从不骂人。平时说话贱贱的，被京都天罗地网同仁称为“贱圣”，但从不骂人，喜欢用讲道理却不似道理的话怼人。无父无母，从小吃了了",
//                    NINE_SIXTEEN_AVATAR,
//                    TTS_LONG,
//                    94000,
//                    senderType = 2,
//                ),
//                ChatMsgItem(
//                    "beyond",
//                    "主控发言主控发言1",
//                    senderType = 1
//                )
            )
        }
    }
}