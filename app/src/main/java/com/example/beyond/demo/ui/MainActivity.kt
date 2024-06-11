package com.example.beyond.demo.ui

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.beyond.demo.databinding.ActivityMainBinding
import com.example.beyond.demo.player.MediaPlugin

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val mediaClient = MediaPlugin.Client()

    companion object {
        private const val TAG = "MainActivity"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("AppWidget", "$TAG onCreate")
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvPlay.setOnClickListener {
            play()
        }
    }

    private fun play() {

        mediaClient.create("https://downsc.chinaz.net/Files/DownLoad/sound1/201906/11582.mp3", "aa", { key, player ->
            Log.i(TAG, "$key onComplete")
        }, { desc ->
            Log.i(TAG, "onError $desc")
        })


        binding.root.postDelayed({
            mediaClient.play("aa")
        }, 500)
    }
}
