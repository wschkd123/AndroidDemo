package com.example.beyond.demo.ui.transformer

import android.os.Bundle
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
import androidx.media3.transformer.Transformer
import com.example.base.AppContext
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
        private const val TTS_PLACEHOLDER = "asset:///media/mp3/tts_placeholder.mp3"
        private const val PLACEHOLDER_IMAGE = "asset:///media/img/img_empty.png"
        private const val THREE_THREE_AVATAR = "https://zmdcharactercdn.zhumengdao.com/34487524784424960048.png"
    }

    private var _binding: FragmentTransformerBinding? = null
    private val binding get() = _binding!!
    private val playerWrapper = ExoPlayerWrapper()
    private lateinit var exportStopwatch: Stopwatch
    private var outputFile: File? = null

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
        outputFile =
            YWFileUtil.createNewFile(YWFileUtil.getStorageFileDir(context)?.path + "/" + System.currentTimeMillis() + ".mp4")
                ?: return
        val outputFilePath = outputFile!!.absolutePath
        val transformer: Transformer = createTransformer(outputFilePath)
        val composition: Composition = createComposition()
        exportStopwatch.start()
        transformer.start(composition, outputFilePath)
    }

    override fun onDestroy() {
        super.onDestroy()
        playerWrapper.release()
    }

    private fun createComposition(): Composition {
        val list = EditedItemChunk.mock()
        val imageItemList = mutableListOf<EditedMediaItem>()
        val audioItemList = mutableListOf<EditedMediaItem>()

        // 封面部分
        val coverDurationUs = 5_000_000L
        imageItemList.add(createCoverImageItem(coverDurationUs))
        audioItemList.add(createSilenceItem(coverDurationUs))

        // 主体部分
        list.forEach { chunk ->
            val beforeDurationUs = 2_000_000L
            val chunkBeforeItem = createChunkBeforeItem(chunk, beforeDurationUs)
            imageItemList.add(chunkBeforeItem)
            audioItemList.add(createSilenceItem(beforeDurationUs))

            val chunkItem = createChunkItem(chunk)
            imageItemList.add(chunkItem)
            audioItemList.add(createSilenceItem(coverDurationUs))

            val afterDurationUs = 2_000_000L
            val chunkAfterItem = createChunkAfterItem(chunk, afterDurationUs)
            imageItemList.add(chunkAfterItem)
            audioItemList.add(createSilenceItem(afterDurationUs))
        }

        // 聊天
        val compositionBuilder = Composition.Builder(
            EditedMediaItemSequence(imageItemList),
            EditedMediaItemSequence(audioItemList)
        )
        return compositionBuilder.build()
    }

    private fun createSilenceItem(durationUs: Long): EditedMediaItem {
        val silenceItem = MediaItem.Builder().setUri(TTS_PLACEHOLDER)
            .setClippingConfiguration(
                ClippingConfiguration.Builder()
                    .setStartPositionMs(0)
                    .setEndPositionMs(durationUs.div(C.MILLIS_PER_SECOND))
                    .build()
            ).build()
        return EditedMediaItem.Builder(silenceItem).build()
    }

    private fun createCoverImageItem(durationUs: Long): EditedMediaItem {
        val videoEffects = createVideoEffects{ overlaysBuilder ->
            overlaysBuilder.add(CoverOverlay(requireContext(), THREE_THREE_AVATAR, durationUs))
        }
        return createPlaceHolderItem(durationUs, videoEffects)
    }

    private fun createChunkBeforeItem(chunk: EditedItemChunk, durationUs: Long): EditedMediaItem {
        val videoEffects = createVideoEffects { overlaysBuilder ->
            // B背景图渐显，文本对话框上滑位移+渐显
            overlaysBuilder.add(
                FullscreenAlphaInOverlay(
                    requireContext(),
                    chunk.backgroundImage?:"",
                    durationUs
                )
            )
            overlaysBuilder.add(ChatBoxInOverlay(requireContext(), durationUs))
        }
        return createPlaceHolderItem(durationUs, videoEffects)
    }

    private fun createChunkAfterItem(chunk: EditedItemChunk, durationUs: Long): EditedMediaItem {
        val videoEffects = createVideoEffects { overlaysBuilder ->
            // 背景图渐隐，文本对话框渐隐
            overlaysBuilder.add(
                FullscreenAlphaOutOverlay(
                    requireContext(),
                    chunk.backgroundImage?:"",
                    durationUs
                )
            )
            overlaysBuilder.add(ChatBoxOutOverlay(requireContext(), durationUs))
        }
        return createPlaceHolderItem(durationUs, videoEffects)
    }

    private fun createChunkItem(chunk: EditedItemChunk): EditedMediaItem {
        val durationUs = chunk.getDurationUs()
        val videoEffects = createVideoEffects { overlaysBuilder ->
            overlaysBuilder.add(
                FullscreenImageOverlay(
                    requireContext(),
                    chunk.backgroundImage?:"",
                    durationUs
                )
            )
            overlaysBuilder.add(ChatBoxOverlay(requireContext(), durationUs))
        }
        return createPlaceHolderItem(durationUs, videoEffects)
    }


    private fun createPlaceHolderItem(
        durationUs: Long = 10_000_000L,
        videoEffects: ImmutableList<Effect>
    ): EditedMediaItem {
        return EditedMediaItem.Builder(MediaItem.fromUri(PLACEHOLDER_IMAGE))
            .setDurationUs(durationUs)
            .setFrameRate(TransformerConstant.FRAME_RATE)
            .setEffects(Effects(ImmutableList.of(), videoEffects))
            .build()
    }

    private fun createTransformer(filePath: String): Transformer {
        val transformerBuilder: Transformer.Builder = Transformer.Builder(AppContext.application)
        // mp4格式
//        transformerBuilder.setTransformationRequest(
//            TransformationRequest.Builder().setVideoMimeType(MimeTypes.VIDEO_MP4).build()
//        )
        return transformerBuilder
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
        //TODO 改为应用到composition
        effects.add(
            Presentation.createForWidthAndHeight(
            TransformerConstant.OUT_VIDEO_WIDTH, TransformerConstant.OUT_VIDEO_HEIGHT, Presentation.LAYOUT_SCALE_TO_FIT
        ))

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
        playerWrapper.addMediaItem("file://$filePath")
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

}
