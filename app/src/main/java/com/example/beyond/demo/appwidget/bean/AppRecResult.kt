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

        fun isGroupChat(): Boolean {
            return type == 2
        }
    }

    companion object {
        const val MOCK_1_GROUP = "{\n" +
                "    \"code\": 0,\n" +
                "    \"msg\": \"success\",\n" +
                "    \"data\": {\n" +
                "        \"recList\": [\n" +
                "            {\n" +
                "                \"roomId\": null,\n" +
                "                \"roomName\": \"我是群聊我是群聊我是群聊\",\n" +
                "                \"type\": 2,\n" +
                "                \"sort\": 0,\n" +
                "                \"recUser\": {\n" +
                "                    \"userName\": \"书友9348\",\n" +
                "                    \"userAvatar\": \"https://imgservices-1252317822.image.myqcloud.com/coco/s11172022/f86e1431.loolr2.png\"\n" +
                "                },\n" +
                "                \"characterList\": [\n" +
                "                    {\n" +
                "                        \"characterId\": \"2910868605\",\n" +
                "                        \"characterName\": \"噗噗噗\",\n" +
                "                        \"characterAvatar\": \"https://zmdcharactercdn.zhumengdao.com/6D4A597E3E781A6DC1E8F77E6C8D849D.jpg\"\n" +
                "                    },\n" +
                "                    {\n" +
                "                        \"characterId\": \"2910273255\",\n" +
                "                        \"characterName\": \"小皮衣\",\n" +
                "                        \"characterAvatar\": \"https://zmdcharactercdn.zhumengdao.com/c318df9e17873ac866e68756e08c5b0c\"\n" +
                "                    },\n" +
                "                    {\n" +
                "                        \"characterId\": \"8218676691\",\n" +
                "                        \"characterName\": \"朱汝正\",\n" +
                "                        \"characterAvatar\": \"https://zmdcharactercdn.zhumengdao.com/DDF646BD4FD7D3D02B799DD55C33F84F.jpg\"\n" +
                "                    }\n" +
                "                ]\n" +
                "            }\n" +
                "        ]\n" +
                "    },\n" +
                "    \"success\": true\n" +
                "}"
        const val MOCK_1 = "{\n" +
                "\t\"code\": 0,\n" +
                "\t\"msg\": \"success\",\n" +
                "\t\"data\": {\n" +
                "\t\t\"recList\": [{\n" +
                "\t\t\t\"roomId\": null,\n" +
                "\t\t\t\"roomName\": null,\n" +
                "\t\t\t\"type\": 1,\n" +
                "\t\t\t\"sort\": 0,\n" +
                "\t\t\t\"recUser\": {\n" +
                "\t\t\t\t\"userName\": \"书友9348\",\n" +
                "\t\t\t\t\"userAvatar\": \"https://imgservices-1252317822.image.myqcloud.com/coco/s11172022/f86e1431.loolr2.png\"\n" +
                "\t\t\t},\n" +
                "\t\t\t\"characterList\": [{\n" +
                "\t\t\t\t\"characterId\": \"2910868605\",\n" +
                "\t\t\t\t\"characterName\": \"噗噗噗\",\n" +
                "\t\t\t\t\"characterAvatar\": \"https://zmdcharactercdn.zhumengdao.com/6D4A597E3E781A6DC1E8F77E6C8D849D.jpg\"\n" +
                "\t\t\t}]\n" +
                "\t\t}]\n" +
                "\t},\n" +
                "\t\"success\": true\n" +
                "}"

        const val MOCK_2 = "{\n" +
                "\t\"code\": 0,\n" +
                "\t\"msg\": \"success\",\n" +
                "\t\"data\": {\n" +
                "\t\t\"recList\": [{\n" +
                "\t\t\t\"roomId\": null,\n" +
                "\t\t\t\"roomName\": null,\n" +
                "\t\t\t\"type\": 1,\n" +
                "\t\t\t\"sort\": 0,\n" +
                "\t\t\t\"recUser\": {\n" +
                "\t\t\t\t\"userName\": \"书友9348\",\n" +
                "\t\t\t\t\"userAvatar\": \"https://imgservices-1252317822.image.myqcloud.com/coco/s11172022/f86e1431.loolr2.png\"\n" +
                "\t\t\t},\n" +
                "\t\t\t\"characterList\": [{\n" +
                "\t\t\t\t\"characterId\": \"2910273255\",\n" +
                "\t\t\t\t\"characterName\": \"小皮衣\",\n" +
                "\t\t\t\t\"characterAvatar\": \"https://zmdcharactercdn.zhumengdao.com/c318df9e17873ac866e68756e08c5b0c\"\n" +
                "\t\t\t}]\n" +
                "\t\t}, {\n" +
                "\t\t\t\"roomId\": null,\n" +
                "\t\t\t\"roomName\": null,\n" +
                "\t\t\t\"type\": 1,\n" +
                "\t\t\t\"sort\": 0,\n" +
                "\t\t\t\"recUser\": {\n" +
                "\t\t\t\t\"userName\": \"书友9348\",\n" +
                "\t\t\t\t\"userAvatar\": \"https://imgservices-1252317822.image.myqcloud.com/coco/s11172022/f86e1431.loolr2.png\"\n" +
                "\t\t\t},\n" +
                "\t\t\t\"characterList\": [{\n" +
                "\t\t\t\t\"characterId\": \"8218676691\",\n" +
                "\t\t\t\t\"characterName\": \"朱汝正\",\n" +
                "\t\t\t\t\"characterAvatar\": \"https://zmdcharactercdn.zhumengdao.com/DDF646BD4FD7D3D02B799DD55C33F84F.jpg\"\n" +
                "\t\t\t}]\n" +
                "\t\t}, {\n" +
                "\t\t\t\"roomId\": null,\n" +
                "\t\t\t\"roomName\": null,\n" +
                "\t\t\t\"type\": 1,\n" +
                "\t\t\t\"sort\": 0,\n" +
                "\t\t\t\"recUser\": {\n" +
                "\t\t\t\t\"userName\": \"书友9348\",\n" +
                "\t\t\t\t\"userAvatar\": \"https://imgservices-1252317822.image.myqcloud.com/coco/s11172022/f86e1431.loolr2.png\"\n" +
                "\t\t\t},\n" +
                "\t\t\t\"characterList\": [{\n" +
                "\t\t\t\t\"characterId\": \"2910868605\",\n" +
                "\t\t\t\t\"characterName\": \"噗噗噗\",\n" +
                "\t\t\t\t\"characterAvatar\": \"https://zmdcharactercdn.zhumengdao.com/6D4A597E3E781A6DC1E8F77E6C8D849D.jpg\"\n" +
                "\t\t\t}]\n" +
                "\t\t}]\n" +
                "\t},\n" +
                "\t\"success\": true\n" +
                "}"
    }
}