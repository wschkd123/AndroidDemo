package com.example.beyond.demo.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.example.base.BaseActivity
import com.example.beyond.demo.databinding.ActivityMainBinding
import com.example.beyond.demo.ui.trackanimation.AudioTrackFragment


class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        showFragment(AppWidgetFragment())
//        showFragment(TransformerFragment())
//        ConfessionAnimationDialog().show(supportFragmentManager)
        showFragment(AudioTrackFragment())
    }

    private fun showFragment(fragment: Fragment, tag: String = "") {
        supportFragmentManager.commit(true) {
            add(binding.fragmentContainerView.id, fragment, tag)
            addToBackStack("")
        }
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount <= 1) {
            finish()
        } else {
            super.onBackPressed()
        }
    }
}
