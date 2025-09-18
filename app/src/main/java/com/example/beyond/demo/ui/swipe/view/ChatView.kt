package com.example.beyond.demo.ui.swipe.view

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.TextView
import com.example.base.util.ext.dpToPx
import com.example.beyond.demo.databinding.ViewChatBinding

/**
 *
 * @author wangshichao
 * @date 2025/9/18
 */
class ChatView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ClipView(context, attrs) {

    private val binding = ViewChatBinding.inflate(android.view.LayoutInflater.from(context), this)

    init {
        generateScrollView()
    }

    private fun generateScrollView() {
        for (i in 0 until 50) {
            val textView = TextView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    30.dpToPx()
                )
                textSize = 20f
                text = "Item $i"
            }
            binding.listView.addView(textView)
        }
    }

}