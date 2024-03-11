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
        val characterList: List<Character?>? = null,
        val recUser: RecUser? = null,
        val roomId: Any? = null,
        val roomName: Any? = null,
        val sort: Int = 0,
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