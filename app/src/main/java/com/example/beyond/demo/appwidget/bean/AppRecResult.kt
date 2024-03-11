package com.example.beyond.demo.appwidget.bean

import java.io.Serializable

/**
 *
 * @author wangshichao
 * @date 2024/3/11
 */
data class AppRecResult(
    val recList: List<Rec>? = null
) : Serializable {
    data class Rec(
        /**
         * 单聊一个人物信息 群聊多个人物信息
         */
        val characterList: List<Character>? = null,
        /**
         * 用户信息
         */
        val recUser: RecUser? = null,
        /**
         * 存在房间会返回 房间id，无房间 值为空
         */
        val roomId: String? = null,
        /**
         * 存在房间 会返回房间名称  无房间值为空
         */
        val roomName: String? = null,
        val sort: Int = 0,
        /**
         * 1:单聊 2:群聊
         */
        val type: Int = 0
    ) : Serializable {
        data class Character(
            val characterAvatar: String? = null,
            val characterId: String? = null,
            val characterName: String? = null
        ) : Serializable

        data class RecUser(
            val userAvatar: String? = null,
            val userName: String? = null
        ) : Serializable

        /**
         * 单聊，人物昵称；群聊，房间名称
         */
        fun getName(): String {
            return if (type == 1) {
                characterList?.get(0)?.characterName ?: ""
            } else {
                roomName ?: ""
            }
        }

        fun getAvatarUrl(): String {
            return characterList?.get(0)?.characterAvatar ?: ""
        }

        fun getGroupMemberUrlList(): List<String> {
            return characterList?.map { it.characterAvatar ?: "" } ?: emptyList()
        }

        fun getCharacterName(): String {
            return characterList?.get(0)?.characterName ?: ""
        }
    }

    companion object {
        const val MOCK_DATA = "{\n" +
                "\t\"code\": 0,\n" +
                "\t\"msg\": \"success\",\n" +
                "\t\"data\": {\n" +
                "\t\t\"recList\": [{\n" +
                "\t\t\t\"roomId\": null,\n" +
                "\t\t\t\"roomName\": null,\n" +
                "\t\t\t\"type\": 1,\n" +
                "\t\t\t\"sort\": 0,\n" +
                "\t\t\t\"recUser\": {\n" +
                "\t\t\t\t\"userName\": \"书友5368\",\n" +
                "\t\t\t\t\"userAvatar\": \"https://imgservices-1252317822.image.myqcloud.com/coco/s11172022/1dba2686.ors0vg.png\"\n" +
                "\t\t\t},\n" +
                "\t\t\t\"characterList\": [{\n" +
                "\t\t\t\t\"characterId\": \"2530643091\",\n" +
                "\t\t\t\t\"characterName\": \"小伙伴\",\n" +
                "\t\t\t\t\"characterAvatar\": \"https://zmdcharactercdn.zhumengdao.com/30de37f801b41aec7e949022fcbd9c6d\"\n" +
                "\t\t\t}]\n" +
                "\t\t}, {\n" +
                "\t\t\t\"roomId\": null,\n" +
                "\t\t\t\"roomName\": null,\n" +
                "\t\t\t\"type\": 1,\n" +
                "\t\t\t\"sort\": 0,\n" +
                "\t\t\t\"recUser\": {\n" +
                "\t\t\t\t\"userName\": \"书友5368\",\n" +
                "\t\t\t\t\"userAvatar\": \"https://imgservices-1252317822.image.myqcloud.com/coco/s11172022/1dba2686.ors0vg.png\"\n" +
                "\t\t\t},\n" +
                "\t\t\t\"characterList\": [{\n" +
                "\t\t\t\t\"characterId\": \"4686135830\",\n" +
                "\t\t\t\t\"characterName\": \"王一博\",\n" +
                "\t\t\t\t\"characterAvatar\": \"https://zmdcharactercdn.zhumengdao.com/4a6169364e2a25f38681ac7c5329a4c7.png\"\n" +
                "\t\t\t}]\n" +
                "\t\t}, {\n" +
                "\t\t\t\"roomId\": null,\n" +
                "\t\t\t\"roomName\": null,\n" +
                "\t\t\t\"type\": 1,\n" +
                "\t\t\t\"sort\": 0,\n" +
                "\t\t\t\"recUser\": {\n" +
                "\t\t\t\t\"userName\": \"书友5368\",\n" +
                "\t\t\t\t\"userAvatar\": \"https://imgservices-1252317822.image.myqcloud.com/coco/s11172022/1dba2686.ors0vg.png\"\n" +
                "\t\t\t},\n" +
                "\t\t\t\"characterList\": [{\n" +
                "\t\t\t\t\"characterId\": \"2766320741\",\n" +
                "\t\t\t\t\"characterName\": \"小苹果2\",\n" +
                "\t\t\t\t\"characterAvatar\": \"https://zmdcharactercdn.zhumengdao.com/ddbd6081fcf9dcfd9db8cb43c2444d1c.png\"\n" +
                "\t\t\t}]\n" +
                "\t\t}, {\n" +
                "\t\t\t\"roomId\": null,\n" +
                "\t\t\t\"roomName\": null,\n" +
                "\t\t\t\"type\": 1,\n" +
                "\t\t\t\"sort\": 0,\n" +
                "\t\t\t\"recUser\": {\n" +
                "\t\t\t\t\"userName\": \"书友5368\",\n" +
                "\t\t\t\t\"userAvatar\": \"https://imgservices-1252317822.image.myqcloud.com/coco/s11172022/1dba2686.ors0vg.png\"\n" +
                "\t\t\t},\n" +
                "\t\t\t\"characterList\": [{\n" +
                "\t\t\t\t\"characterId\": \"8041069549\",\n" +
                "\t\t\t\t\"characterName\": \"小苹果3\",\n" +
                "\t\t\t\t\"characterAvatar\": \"https://zmdcharactercdn.zhumengdao.com/f501bd8e52f7c5c3de26f26aefc417e7.png\"\n" +
                "\t\t\t}]\n" +
                "\t\t}]\n" +
                "\t},\n" +
                "\t\"success\": true\n" +
                "}"
    }
}