package com.example.beyond.demo.net

import retrofit2.http.POST

interface WanAndroidService {
    @POST("banner/json")
    suspend fun getBannerInfo(): NetResult<Any>
}