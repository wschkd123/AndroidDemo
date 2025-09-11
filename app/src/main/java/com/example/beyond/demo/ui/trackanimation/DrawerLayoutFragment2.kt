package com.example.beyond.demo.ui.trackanimation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.base.BaseFragment
import com.example.beyond.demo.databinding.FragmentDrawerLayout2Binding


/**
 * 日常模式聊天室
 *
 * @author wangshichao
 * @date 2025/9/10
 */
class DrawerLayoutFragment2 : BaseFragment() {

    private var _binding: FragmentDrawerLayout2Binding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDrawerLayout2Binding.inflate(inflater, container, false)
        return _binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    private fun initView() {
        binding.openTv.setOnClickListener {
            // 展开
            binding.zoomTranslateLayout.expand()
// 重置
//            zoomLayout.reset();
        }
    }

}