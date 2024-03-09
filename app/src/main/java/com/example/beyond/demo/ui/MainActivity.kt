package com.example.beyond.demo.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.beyond.demo.appwidget.CharacterWidgetProviderTest
import com.example.beyond.demo.appwidget.CharacterWidgetProviderTest.Companion.REFRESH_ACTION
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
            tvClick.setOnClickListener {
                val intent = Intent(this@MainActivity, CharacterWidgetProviderTest::class.java)
                intent.setAction(REFRESH_ACTION)
                sendBroadcast(intent)
            }
        }

    }

}
