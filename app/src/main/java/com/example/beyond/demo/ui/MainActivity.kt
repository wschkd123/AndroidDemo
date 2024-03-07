package com.example.beyond.demo.ui

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.example.beyond.demo.databinding.ActivityMainBinding
import com.example.beyond.demo.net.WanAndroidService
import com.example.beyond.demo.net.NetResult
import com.example.beyond.demo.net.RetrofitFactory
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater).apply {
            setContentView(root)
        }

        binding.tvClick.setOnClickListener {
            fetchData().observe(this) {
                Log.i(TAG, "fetchData $it")
            }
        }
    }

    private fun fetchData(): MutableLiveData<NetResult<Any>> {
        val liveData = MutableLiveData<NetResult<Any>>()
        lifecycleScope.launch {
            try {
                val result = RetrofitFactory.getRetrofit().create(WanAndroidService::class.java)
                        .getBannerInfo()
                liveData.postValue(result)
            } catch (e: Exception) {
                e.printStackTrace()
                liveData.postValue(NetResult.badResult())
            }
        }
        return liveData
    }

}
