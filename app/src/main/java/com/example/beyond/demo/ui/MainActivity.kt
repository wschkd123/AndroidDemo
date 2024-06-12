package com.example.beyond.demo.ui

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.example.beyond.demo.databinding.ActivityMainBinding
import com.example.beyond.demo.base.BaseActivity

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        showFragment(PlayFragment())
    }

    private fun showFragment(fragment: Fragment, tag: String = "") {
        supportFragmentManager.commit(true) {
            add(binding.fragmentContainerView.id, fragment, tag)
        }
    }

    override fun onBackPressed() {
        Log.w(TAG, "backStackEntryCount:${supportFragmentManager.backStackEntryCount}")
        // 返回键直接退出App
        if (supportFragmentManager.backStackEntryCount <= 1 ) {
            finish()
        } else {
            super.onBackPressed()
        }
    }
}
