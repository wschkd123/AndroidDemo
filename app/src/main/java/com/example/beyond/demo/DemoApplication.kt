package com.example.beyond.demo

import android.app.Application
import com.example.base.AppContext
import com.example.base.Init

/**
 *
 * @author wangshichao
 * @date 2024/3/11
 */
class DemoApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        Init.setApplication(this)
        AppContext.init(this)
    }
}