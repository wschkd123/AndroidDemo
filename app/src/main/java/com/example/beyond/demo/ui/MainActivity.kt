package com.example.beyond.demo.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.beyond.demo.appwidget.CharacterWidgetReceiver
import com.example.beyond.demo.appwidget.CharacterWidgetReceiver.Companion.ACTION_APPWIDGET_CHARACTER_REFRESH
import com.example.beyond.demo.appwidget.MultiCharacterWidgetReceiver
import com.example.beyond.demo.appwidget.test.TestWidgetReceiver
import com.example.beyond.demo.appwidget.test.TestWidgetReceiver.Companion.REFRESH_ACTION
import com.example.beyond.demo.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("AppWidget", "$TAG onCreate")
        binding = ActivityMainBinding.inflate(layoutInflater).apply {
            setContentView(root)
            tvTestRefresh.setOnClickListener {
                val intent = Intent(this@MainActivity, TestWidgetReceiver::class.java)
                intent.setAction(REFRESH_ACTION)
                sendBroadcast(intent)
            }

            tvCharacterRefresh.setOnClickListener {
                val intent = Intent(this@MainActivity, CharacterWidgetReceiver::class.java)
                intent.setAction(ACTION_APPWIDGET_CHARACTER_REFRESH)
                sendBroadcast(intent)
            }

            tvMultiCharacterRefresh.setOnClickListener {
                val intent = Intent(this@MainActivity, MultiCharacterWidgetReceiver::class.java)
                intent.setAction(REFRESH_ACTION)
                sendBroadcast(intent)
            }
        }

    }

}
