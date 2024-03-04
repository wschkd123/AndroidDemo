package com.example.beyond.demo.net

import java.io.Serializable

class NetResult<T> : Serializable {
    var errorCode: Int = 0
    var errorMsg: String? = null
    var data: T? = null

    fun isSuccess(): Boolean {
        return errorCode == 0
    }

    companion object {

        /**
         * 构造一个，网络异常的结果
         */
        fun <T> badResult(): NetResult<T> {
            return badResult(-1, "网络异常，请稍后再试")
        }

        fun <T> badResult(msg: String? = null): NetResult<T> {
            val badResult = NetResult<T>()
            badResult.errorCode = -1
            badResult.errorMsg = msg ?: "网络异常，请稍后再试"
            return badResult
        }

        /**
         * 构建一个错误响应
         */
        fun <T> badResult(code: Int, msg: String? = null): NetResult<T> {
            val badResult = NetResult<T>()
            badResult.errorCode = code
            badResult.errorMsg = msg ?: "网络异常，请稍后再试"
            return badResult
        }

        /**
         * mock一个正确的响应
         */
        fun <T> mockResult(data: T?): NetResult<T> {
            val result = NetResult<T>()
            result.errorCode = 0
            result.errorMsg = ""
            result.data = data
            return result
        }
    }

    override fun toString(): String {
        return "NetResult(code=$errorCode, msg=$errorMsg, data=$data)"
    }

}