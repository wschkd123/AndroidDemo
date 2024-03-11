package com.example.beyond.demo.net

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitFactory {

    private val TAG = "RetrofitFactory"

    private const val BASE_URL = "http://www.wanandroid.com"

    private var mRetrofit: Retrofit? = null

    private var mClient: OkHttpClient? = null


    fun getRetrofit(): Retrofit {
        if (mRetrofit == null) {
            val builder =
                    Retrofit.Builder().baseUrl(BASE_URL).client(getOkHttpClient())
                            .addConverterFactory(GsonConverterFactory.create())
            mRetrofit = builder.build()
        }
        return mRetrofit!!
    }

    private fun getOkHttpClient(): OkHttpClient {
        if (mClient == null) {
            val builder = OkHttpClient.Builder()
            mClient = builder.build()
        }
        return mClient!!
    }
}