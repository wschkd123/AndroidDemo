package com.example.base.player

import androidx.annotation.IntDef
import com.example.base.player.PlayState.Companion.IDLE
import com.example.base.player.PlayState.Companion.LOADING
import com.example.base.player.PlayState.Companion.PLAYING

/**
 * 播放状态
 *
 * @author wangshichao
 * @date 2024/6/24
 */
@Retention(AnnotationRetention.SOURCE)
@IntDef(IDLE, LOADING, PLAYING)
annotation class PlayState {
    companion object {
        /**
         * 包括未准备好和播放完成
         */
        const val IDLE = 0

        /**
         * 加载中
         */
        const val LOADING = 1

        /**
         * 播放中
         */
        const val PLAYING = 2
    }
}