package com.example.beyond.demo.ui.transformer

import TextLinesOverlay
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.media3.common.C
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.ClippingConfiguration
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.effect.TextureOverlay
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.example.base.Init.getApplicationContext
import com.example.base.player.exoplayer.ExoPlayerWrapper
import com.example.base.util.YWDeviceUtil
import com.example.base.util.YWFileUtil
import com.example.beyond.demo.R
import com.example.beyond.demo.databinding.FragmentTransformerBinding
import com.example.beyond.demo.ui.transformer.overlay.ChatBoxInOverlay
import com.example.beyond.demo.ui.transformer.overlay.ChatBoxOutOverlay
import com.example.beyond.demo.ui.transformer.overlay.ChatBoxOverlay
import com.example.beyond.demo.ui.transformer.overlay.CoverOverlay
import com.example.beyond.demo.ui.transformer.overlay.FullscreenAlphaInOverlay
import com.example.beyond.demo.ui.transformer.overlay.FullscreenAlphaOutOverlay
import com.example.beyond.demo.ui.transformer.overlay.FullscreenImageOverlay
import com.example.beyond.demo.ui.transformer.util.JsonUtil
import com.google.common.base.Stopwatch
import com.google.common.base.Ticker
import com.google.common.collect.ImmutableList
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit


/**
 *
 * @author wangshichao
 * @date 2024/7/10
 */
@UnstableApi
class TransformerFragment : Fragment() {

    companion object {
        private const val TAG = "TransformerFragment"

        /**
         * 占位音频长度（微秒）
         */
        private const val PLACEHOLDER_AUDIO_DURATION_US = 180_000_000L

        /**
         * 占位音频
         */
        private const val PLACEHOLDER_AUDIO = "asset:///placeholder.mp3"

        /**
         * 占位图片
         */
        private const val PLACEHOLDER_IMAGE = "asset:///placeholder.png"

        private const val MAIN_MP4 = "asset:///1723183588304.mp4"
        private const val END_MP4_URL = "https://imgservices-1252317822.image.myqcloud.com/coco/s08082024/33a67f6d.8ge341.mp4"
        private const val END_MP4 = "asset:///end25.mp4"
    }

    private var _binding: FragmentTransformerBinding? = null
    private val binding get() = _binding!!
    private val playerWrapper = ExoPlayerWrapper()
    private lateinit var exportStopwatch: Stopwatch
    private lateinit var outputFile: File
    private lateinit var finalOutputFile: File
    private var transformer: Transformer? = null
    private val mainHandler = Handler(Looper.getMainLooper())


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentTransformerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        exportStopwatch = Stopwatch.createUnstarted(
            object : Ticker() {
                override fun read(): Long {
                    return SystemClock.elapsedRealtimeNanos()
                }
            })
        // 播放器9:16
        binding.playerView.layoutParams = binding.playerView.layoutParams.apply {
            width = YWDeviceUtil.getScreenWidth()
            height = width * 16 / 9
        }
        binding.playerView.player = playerWrapper.player
        binding.tvOutput.setOnClickListener {
            start()
        }
    }

    private fun start() {
        if (exportStopwatch.isRunning) {
            Log.w(TAG, "is transformer")
            return
        }
        outputFile =
            YWFileUtil.createNewFile(YWFileUtil.getStorageFileDir(context)?.path + "/" + System.currentTimeMillis() + ".mp4")
                ?: return
        finalOutputFile =
            YWFileUtil.createNewFile(YWFileUtil.getStorageFileDir(context)?.path + "/final-" + System.currentTimeMillis() + ".mp4")
                ?: return
        val outputFilePath = outputFile.absolutePath
        transformer = createTransformer(outputFilePath)
        val composition: Composition = createComposition()
        exportStopwatch.reset()
        exportStopwatch.start()
        transformer?.start(composition, outputFilePath)

        val progressHolder = ProgressHolder()
        mainHandler.post(object : Runnable {
            override fun run() {
                if (transformer != null && transformer?.getProgress(progressHolder) != Transformer.PROGRESS_STATE_NOT_STARTED) {
                    updateProgress((progressHolder.progress * 0.9f).toInt())
                    mainHandler.postDelayed(this, 500)
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        playerWrapper.release()
        transformer?.cancel()
        mainHandler.removeCallbacksAndMessages(null)
    }

    private fun createComposition(): Composition {
        val imageItemList = mutableListOf<EditedMediaItem>()
        val audioItemList = mutableListOf<EditedMediaItem>()
        val list = ChatMsgItem.convertList()

        // 封面部分
        val coverDurationUs = 200_000L
        val coverUrl = ChatMsgItem.findFirstCharacter(list)?.backgroundUrl ?: ""
        imageItemList.add(createCoverImageItem(coverDurationUs, coverUrl))
        audioItemList.add(createPlaceHolderAudioItem(coverDurationUs))

        // 聊天主体部分
        list.forEach { chatMsg ->
            // 1. 聊天文本框进入动画
            val chatInDurationUs = 400_000L
            if (chatMsg.textBoxInAnimation || chatMsg.bgInAnimation) {
                imageItemList.add(createChatInItem(chatMsg, chatInDurationUs))
                audioItemList.add(createPlaceHolderAudioItem(chatInDurationUs))
            }


            // 2. 聊天文本播放
            imageItemList.add(createChatItem(chatMsg))
            val audioItem = if (chatMsg.havaAudio()) {
                EditedMediaItem.Builder(MediaItem.fromUri(chatMsg.audioUrl ?: "")).build()
            } else {
                createPlaceHolderAudioItem(chatMsg.getDurationUs())
            }
            audioItemList.add(audioItem)


            // 3. 聊天文本框退出动画
            if (chatMsg.textBoxOutAnimation || chatMsg.bgOutAnimation) {
                val chatOutDurationUs = 400_000L
                imageItemList.add(createChatOutItem(chatMsg, chatOutDurationUs))
                audioItemList.add(createPlaceHolderAudioItem(chatOutDurationUs))
            }
        }


        return Composition.Builder(
            EditedMediaItemSequence(imageItemList),
            EditedMediaItemSequence(audioItemList)
        ).build()
    }

    private fun createCoverImageItem(durationUs: Long, url: String): EditedMediaItem {
        val videoEffects = createVideoEffects { overlaysBuilder ->
            overlaysBuilder.add(CoverOverlay(requireContext(), url, durationUs))
        }
        return createPlaceHolderImageItem(durationUs, videoEffects)
    }

    /**
     * 聊天文本框进入动画Item
     */
    private fun createChatInItem(chatMsg: ChatMsgItem, durationUs: Long): EditedMediaItem {
        val videoEffects = createVideoEffects { overlaysBuilder ->
            overlaysBuilder.add(
                FullscreenAlphaInOverlay(
                    requireContext(),
                    chatMsg.backgroundUrl ?: "",
                    durationUs,
                    chatMsg.bgInAnimation
                )
            )
            overlaysBuilder.add(
                if (chatMsg.textBoxInAnimation) {
                    ChatBoxInOverlay(requireContext(), chatMsg, durationUs)
                } else {
                    ChatBoxOverlay(requireContext(), chatMsg, false)
                }
            )
        }
        return createPlaceHolderImageItem(durationUs, videoEffects)
    }

    /**
     * 聊天文本框退出动画Item
     */
    private fun createChatOutItem(chatMsg: ChatMsgItem, durationUs: Long): EditedMediaItem {
        val videoEffects = createVideoEffects { overlaysBuilder ->
            overlaysBuilder.add(
                FullscreenAlphaOutOverlay(
                    requireContext(),
                    chatMsg.backgroundUrl ?: "",
                    durationUs,
                    chatMsg.bgInAnimation
                )
            )
            overlaysBuilder.add(
                if (chatMsg.textBoxOutAnimation) {
                    ChatBoxOutOverlay(requireContext(), chatMsg, durationUs)
                } else {
                    ChatBoxOverlay(requireContext(), chatMsg, false)
                }
            )
        }
        return createPlaceHolderImageItem(durationUs, videoEffects)
    }


    /**
     * 聊天文本播放Item
     */
    private fun createChatItem(chatMsg: ChatMsgItem): EditedMediaItem {
        val durationUs = chatMsg.getDurationUs()
        val videoEffects = createVideoEffects { overlaysBuilder ->
            overlaysBuilder.add(
                FullscreenImageOverlay(
                    requireContext(),
                    chatMsg.backgroundUrl ?: "",
                    durationUs
                )
            )
            overlaysBuilder.add(ChatBoxOverlay(requireContext(), chatMsg))
            overlaysBuilder.add(TextLinesOverlay(requireContext(), chatMsg))
        }
        return createPlaceHolderImageItem(durationUs, videoEffects)
    }

    /**
     * 创建占位图片Item
     */
    private fun createPlaceHolderImageItem(
        durationUs: Long = 10_000_000L,
        videoEffects: ImmutableList<Effect>? = null
    ): EditedMediaItem {
        return EditedMediaItem.Builder(MediaItem.fromUri(PLACEHOLDER_IMAGE))
            .setDurationUs(durationUs)
            .setFrameRate(TransformerConstant.FRAME_RATE)
            .setEffects(Effects(ImmutableList.of(), videoEffects ?: ImmutableList.of()))
            .build()
    }

    /**
     * 创建占位音频Item。需要根据音频长度剪裁
     */
    private fun createPlaceHolderAudioItem(durationUs: Long): EditedMediaItem {
        // 如果超出音频长度，使用音频长度
        var finalDurationUs = durationUs
        if (durationUs > PLACEHOLDER_AUDIO_DURATION_US) {
            finalDurationUs = PLACEHOLDER_AUDIO_DURATION_US
        }
        val silenceItem = MediaItem.Builder().setUri(PLACEHOLDER_AUDIO)
            .setClippingConfiguration(
                ClippingConfiguration.Builder()
                    .setStartPositionMs(0)
                    .setEndPositionMs(finalDurationUs.div(C.MILLIS_PER_SECOND))
                    .build()
            ).build()
        return EditedMediaItem.Builder(silenceItem).build()
    }

    private fun createTransformer(filePath: String): Transformer {
        return Transformer.Builder(requireContext())
            .addListener(
                object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        this@TransformerFragment.onCompleted(filePath, exportResult)
                    }

                    override fun onError(
                        composition: Composition,
                        exportResult: ExportResult,
                        exportException: ExportException
                    ) {
                        this@TransformerFragment.onError(exportException)
                    }
                })
            .build()
    }

    private fun createVideoEffects(block: (ImmutableList.Builder<TextureOverlay>) -> Unit): ImmutableList<Effect> {
        val effects = ImmutableList.Builder<Effect>()
        // 配置输出视频分辨率。需要放前面，后续Overlay中configure尺寸才生效
        effects.add(
            Presentation.createForWidthAndHeight(
                TransformerConstant.OUT_VIDEO_WIDTH,
                TransformerConstant.OUT_VIDEO_HEIGHT,
                Presentation.LAYOUT_SCALE_TO_FIT
            )
        )

        // 添加overlay
        val overlaysBuilder = ImmutableList.Builder<TextureOverlay>()
        block(overlaysBuilder)
        val overlayEffect = OverlayEffect(overlaysBuilder.build())
        effects.add(overlayEffect)
        return effects.build()
    }

    private fun onCompleted(filePath: String, exportResult: ExportResult) {
        exportStopwatch.stop()
        val elapsedTimeMs: Long = exportStopwatch.elapsed(TimeUnit.MILLISECONDS)
        binding.informationTextView.text =
            getString(R.string.export_completed, elapsedTimeMs / 1000f, filePath)
        generateFinalVideo()
        Log.d(
            TAG,
            "Output file path: file://$filePath"
        )
        try {
            val resultJson: JSONObject = JsonUtil.exportResultAsJsonObject(exportResult)
                .put("elapsedTimeMs", elapsedTimeMs)
                .put("device", JsonUtil.getDeviceDetailsAsJsonObject())
            for (line in Util.split(resultJson.toString(2), "\n")) {
                Log.d(TAG, line)
            }
        } catch (e: JSONException) {
            Log.d(
                TAG,
                "Unable to convert exportResult to JSON",
                e
            )
        }
    }

    private fun onError(exportException: ExportException) {
        exportStopwatch.stop()
        binding.informationTextView.text = "Export error"
        Toast.makeText(getApplicationContext(), "Export error: $exportException", Toast.LENGTH_LONG)
            .show()
        Log.e(
            TAG,
            "Export error",
            exportException
        )
    }

    /**
     * 拼接结尾mp4
     */
    private fun generateFinalVideo() {
        val list = mutableListOf(
            EditedMediaItem.Builder(MediaItem.fromUri(MAIN_MP4)).build(),
            EditedMediaItem.Builder(MediaItem.fromUri(END_MP4_URL)).build(),
        )


        val composition = Composition.Builder(EditedMediaItemSequence(list)).build()
        transformer = createFinalTransformer()
        transformer?.start(composition, finalOutputFile.absolutePath)

        val progressHolder = ProgressHolder()
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.post(object : Runnable {
            override fun run() {
                if (transformer != null && transformer?.getProgress(progressHolder) != Transformer.PROGRESS_STATE_NOT_STARTED) {
                    updateProgress((90 + progressHolder.progress * 0.1f).toInt())
                    mainHandler.postDelayed(this, 500)
                }
            }
        })
    }

    private fun createFinalTransformer(): Transformer {
        return Transformer.Builder(requireContext())
            .addListener(
                object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        Log.w(TAG, "final video completed")
                        playerWrapper.addMediaItem("file://$finalOutputFile")
                    }

                    override fun onError(
                        composition: Composition,
                        exportResult: ExportResult,
                        exportException: ExportException
                    ) {
                        exportException.printStackTrace()
                    }
                })
            .build()
    }

    private fun updateProgress(process: Int) {
        binding.tvProgress.text = "${process}%"
    }

}
