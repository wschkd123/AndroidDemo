package androidx.media3.demo.transformer.overlay

/**
 *
 * @author wangshichao
 * @date 2024/7/10
 */
data class EditedChunkData(
        /**
         * 持续时长 微秒
         */
        val duration: Long,
        /**
         * 视频的帧率。单位为帧每秒
         */
        val frameRate: Long = 30,
) {
    /**
     * 每一帧间隔。单位为微秒
     */
    fun framePeriod() = 1000 / 30
}