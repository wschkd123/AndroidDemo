package com.example.base

import android.os.Bundle
import androidx.fragment.app.Fragment

/**
 * Fragment基类
 *
 * 1. 沉浸式状态栏
 * 2. 加载框
 *
 * @author SawRen
 * @email: sawren@tencent.com
 * @date 2010-10-9
 */
open class BaseFragment : Fragment() {
    protected open val TAG = javaClass.simpleName
    private var isFirstLoad = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isFirstLoad = true
    }

    override fun onResume() {
        super.onResume()
        if (isFirstLoad) {
            isFirstLoad = false
            lazyLoadData()
        }
    }

    /**
     * 延迟加载数据。用于ViewPager2的子Fragment
     */
    protected open fun lazyLoadData() {

    }


}