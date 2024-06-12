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

        initPlayer()

        binding.tvPlay.setOnClickListener {
            mediaClient.play("aa")
        }

        binding.tvStop.setOnClickListener {
            mediaClient.pause("aa")
        }
    }

    private fun initPlayer() {

        mediaClient.create(
            "https://downsc.chinaz.net/Files/DownLoad/sound1/201906/11582.mp3",
            "aa",
            { key, player ->
                Log.i(TAG, "$key onReady")
                mediaClient.play("aa")
            },
            { desc ->
                Log.i(TAG, "onError $desc")
            })

        mediaClient.setOnCompleteListener {
            Log.i(TAG, "complete $it")
        }

        mediaClient.setOnPlaybackStateChangedListener { key, time ->
            Log.i(TAG, "playback key:$key time:$time")
        }

        mediaClient.setOnErrorListener { key, desc ->
            Log.i(TAG, "error key:$key desc:$desc")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaClient.release()
    }
}
