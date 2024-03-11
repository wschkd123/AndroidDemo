package com.example.beyond.demo.net

import retrofit2.Call
import retrofit2.http.GET

interface WanAndroidService {
    @GET("banner/json")
    fun getBannerInfo(): Call<NetResult<Any>>
}