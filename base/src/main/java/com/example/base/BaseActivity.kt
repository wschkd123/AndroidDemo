package com.example.base

import android.os.Bundle
import android.view.Window
import androidx.appcompat.app.AppCompatActivity

/**
 *
 * @author SawRen
 * @email: sawren@tencent.com
 * @date 2010-10-9
 */
open class BaseActivity : AppCompatActivity() {

    protected open val TAG = javaClass.simpleName
    override fun onCreate(savedInstanceState: Bundle?) {
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
    }

}
