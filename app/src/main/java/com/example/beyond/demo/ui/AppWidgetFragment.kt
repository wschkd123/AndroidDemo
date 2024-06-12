package com.example.beyond.demo.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.beyond.demo.ui.appwidget.CharacterWidgetReceiver
import com.example.beyond.demo.ui.appwidget.MultiCharacterWidgetReceiver
import com.example.beyond.demo.ui.appwidget.test.TestWidgetReceiver
import com.example.beyond.demo.base.BaseFragment
import com.example.beyond.demo.databinding.FragmentAppwidgetBinding

/**
 * 小组件
 *
 * @author wangshichao
 * @date 2024/6/12
 */
class AppWidgetFragment: BaseFragment() {

    private var _binding: FragmentAppwidgetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAppwidgetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvTestRefresh.setOnClickListener {
            val intent = Intent(context, TestWidgetReceiver::class.java)
            intent.setAction(TestWidgetReceiver.REFRESH_ACTION)
            context?.sendBroadcast(intent)
        }

        binding.tvCharacterRefresh.setOnClickListener {
            val intent = Intent(context, CharacterWidgetReceiver::class.java)
            intent.setAction(CharacterWidgetReceiver.ACTION_APPWIDGET_CHARACTER_REFRESH)
            context?.sendBroadcast(intent)
        }

        binding.tvMultiCharacterRefresh.setOnClickListener {
            val intent = Intent(context, MultiCharacterWidgetReceiver::class.java)
            intent.setAction(MultiCharacterWidgetReceiver.ACTION_APPWIDGET_MULTI_CHARACTER_REFRESH)
            context?.sendBroadcast(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}