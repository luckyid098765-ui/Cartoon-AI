package com.example.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.media.MediaMuxer
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import com.example.data.model.CameraMotion
import com.example.data.model.SceneRecord
import com.example.data.model.VideoAspectRatio
import com.example.data.model.VideoResolution
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private const val TAG = "CartoonVideoRenderer"

data class SceneRenderInput(
    val scene: SceneRecord,
    val bitmap: Bitmap,
    val audio: SynthesizedSceneAudio
)

data class RenderResult(
    val outputFile: File,
    val actualWidth: Int,
    val actualHeight: Int,
    val durationSeconds: Int
)

class CartoonVideoRenderer(private val context: Context) {

    suspend fun renderMovieMp4(
        projectId: String,
        movieTitle: String,
        aspectRatio: VideoAspectRatio,
        resolution: VideoResolution,
        sceneInputs: List<SceneRenderInput>,
        onProgress: (Int, String) -> Unit
    ): RenderResult = withContext(Dispatchers.IO) {
        val moviesDir = File(context.filesDir, "movies").apply { mkdirs() }
        val outFile = File(moviesDir, "${projectId}.mp4")
        if (outFile.exists()) outFile.delete()

        val (targetW, targetH) = resolveSupportedVideoDimensions(aspectRatio, resolution)
        val fps = VoiceAndAudioEngine.LIP_SYNC_FPS
        val sampleRate = VoiceAndAudioEngine.TARGET_SAMPLE_RATE

        // Combine all scene PCM audio into one continuous audio buffer
        val totalAudioSamples = sceneInputs.sumOf { it.audio.pcmSamples.size }
        val combinedPcm = ShortArray(totalAudioSamples)
        var audioCursor = 0
        for (input in sceneInputs) {
            input.audio.pcmSamples.copyInto(combinedPcm, destinationOffset = audioCursor)
            audioCursor += input.audio.pcmSamples.size
        }
        val totalDurationSec = (totalAudioSamples.toFloat() / sampleRate).coerceAtLeast(1f)

        val videoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, targetW, targetH).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, (targetW * targetH * 3).coerceIn(1_500_000, 12_000_000))
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        val audioFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, 1).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, 96_000)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
        }

        val videoEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        val audioEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        var muxer: MediaMuxer? = null

        try {
            videoEncoder.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val inputSurface = videoEncoder.createInputSurface()
            videoEncoder.start()

            audioEncoder.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            audioEncoder.start()

            muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var videoTrackIndex = -1
            var audioTrackIndex = -1
            var muxerStarted = false

            val videoBufferInfo = MediaCodec.BufferInfo()
            val audioBufferInfo = MediaCodec.BufferInfo()

            val totalVideoFrames = sceneInputs.sumOf {
                (it.audio.durationSec * fps).toInt().coerceAtLeast(fps)
            }
            var globalFrame = 0
            var audioSampleOffset = 0
            var audioEosSent = false
            var videoEosSent = false
            var videoDone = false
            var audioDone = false

            val sceneFrameList = buildList {
                sceneInputs.forEachIndexed { sIdx, input ->
                    val frameCount = (input.audio.durationSec * fps).toInt().coerceAtLeast(fps)
                    for (f in 0 until frameCount) {
                        add(Triple(sIdx, input, f to frameCount))
                    }
                }
            }

            while (!videoDone || !audioDone) {
                // Feed one video frame per loop iteration if frames remain
                if (!videoEosSent) {
                    if (globalFrame < sceneFrameList.size) {
                        val (sIdx, input, framePair) = sceneFrameList[globalFrame]
                        val (localFrame, sceneTotalFrames) = framePair
                        val canvas = inputSurface.lockCanvas(null)
                        try {
                            drawCartoonFrame(
                                canvas = canvas,
                                width = targetW,
                                height = targetH,
                                movieTitle = movieTitle,
                                input = input,
                                nextBitmap = sceneInputs.getOrNull(sIdx + 1)?.bitmap,
                                localFrame = localFrame,
                                sceneTotalFrames = sceneTotalFrames,
                                isFirstSceneOpening = sIdx == 0 && localFrame < fps * 2,
                                fps = fps
                            )
                        } finally {
                            inputSurface.unlockCanvasAndPost(canvas)
                        }
                        globalFrame++
                        if (globalFrame % 12 == 0 || globalFrame == sceneFrameList.size) {
                            val pct = ((globalFrame.toFloat() / max(1, totalVideoFrames)) * 100).toInt()
                            onProgress(pct.coerceIn(0, 99), "Encoding frame $globalFrame / $totalVideoFrames")
                        }
                    } else {
                        videoEncoder.signalEndOfInputStream()
                        videoEosSent = true
                    }
                }

                // Feed audio PCM chunks
                if (!audioEosSent) {
                    val inIndex = audioEncoder.dequeueInputBuffer(5_000)
                    if (inIndex >= 0) {
                        val inBuf = audioEncoder.getInputBuffer(inIndex)!!
                        inBuf.clear()
                        if (audioSampleOffset >= combinedPcm.size) {
                            val ptsUs = (audioSampleOffset.toLong() * 1_000_000L) / sampleRate
                            audioEncoder.queueInputBuffer(
                                inIndex, 0, 0, ptsUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            audioEosSent = true
                        } else {
                            val maxSamples = inBuf.remaining() / 2
                            val samplesToWrite = min(maxSamples, combinedPcm.size - audioSampleOffset)
                            val byteBuf = ByteBuffer.allocate(samplesToWrite * 2).order(ByteOrder.LITTLE_ENDIAN)
                            for (i in 0 until samplesToWrite) {
                                byteBuf.putShort(combinedPcm[audioSampleOffset + i])
                            }
                            inBuf.put(byteBuf.array())
                            val ptsUs = (audioSampleOffset.toLong() * 1_000_000L) / sampleRate
                            audioEncoder.queueInputBuffer(inIndex, 0, samplesToWrite * 2, ptsUs, 0)
                            audioSampleOffset += samplesToWrite
                        }
                    }
                }

                // Drain video encoder
                if (!videoDone) {
                    drainLoop@ while (true) {
                        val outIdx = videoEncoder.dequeueOutputBuffer(videoBufferInfo, 0)
                        when {
                            outIdx == MediaCodec.INFO_TRY_AGAIN_LATER -> break@drainLoop
                            outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                                videoTrackIndex = muxer.addTrack(videoEncoder.outputFormat)
                                if (videoTrackIndex >= 0 && audioTrackIndex >= 0 && !muxerStarted) {
                                    muxer.start()
                                    muxerStarted = true
                                }
                            }
                            outIdx >= 0 -> {
                                val encoded = videoEncoder.getOutputBuffer(outIdx)!!
                                if ((videoBufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                                    videoBufferInfo.size = 0
                                }
                                if (videoBufferInfo.size > 0 && muxerStarted) {
                                    encoded.position(videoBufferInfo.offset)
                                    encoded.limit(videoBufferInfo.offset + videoBufferInfo.size)
                                    muxer.writeSampleData(videoTrackIndex, encoded, videoBufferInfo)
                                }
                                videoEncoder.releaseOutputBuffer(outIdx, false)
                                if ((videoBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                    videoDone = true
                                    break@drainLoop
                                }
                            }
                        }
                    }
                }

                // Drain audio encoder
                if (!audioDone) {
                    drainAudio@ while (true) {
                        val outIdx = audioEncoder.dequeueOutputBuffer(audioBufferInfo, 0)
                        when {
                            outIdx == MediaCodec.INFO_TRY_AGAIN_LATER -> break@drainAudio
                            outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                                audioTrackIndex = muxer.addTrack(audioEncoder.outputFormat)
                                if (videoTrackIndex >= 0 && audioTrackIndex >= 0 && !muxerStarted) {
                                    muxer.start()
                                    muxerStarted = true
                                }
                            }
                            outIdx >= 0 -> {
                                val encoded = audioEncoder.getOutputBuffer(outIdx)!!
                                if ((audioBufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                                    audioBufferInfo.size = 0
                                }
                                if (audioBufferInfo.size > 0 && muxerStarted) {
                                    encoded.position(audioBufferInfo.offset)
                                    encoded.limit(audioBufferInfo.offset + audioBufferInfo.size)
                                    muxer.writeSampleData(audioTrackIndex, encoded, audioBufferInfo)
                                }
                                audioEncoder.releaseOutputBuffer(outIdx, false)
                                if ((audioBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                    audioDone = true
                                    break@drainAudio
                                }
                            }
                        }
                    }
                }
            }

            onProgress(100, "MP4 export ready")
            RenderResult(
                outputFile = outFile,
                actualWidth = targetW,
                actualHeight = targetH,
                durationSeconds = totalDurationSec.toInt().coerceAtLeast(1)
            )
        } finally {
            runCatching { videoEncoder.stop() }
            runCatching { videoEncoder.release() }
            runCatching { audioEncoder.stop() }
            runCatching { audioEncoder.release() }
            runCatching { muxer?.stop() }
            runCatching { muxer?.release() }
        }
    }

    private fun resolveSupportedVideoDimensions(
        aspectRatio: VideoAspectRatio,
        resolution: VideoResolution
    ): Pair<Int, Int> {
        val desired = when (resolution) {
            VideoResolution.RES_480P -> aspectRatio.width480 to aspectRatio.height480
            VideoResolution.RES_720P -> aspectRatio.width720 to aspectRatio.height720
            VideoResolution.RES_1080P -> aspectRatio.width1080 to aspectRatio.height1080
            VideoResolution.RES_4K -> when (aspectRatio) {
                VideoAspectRatio.RATIO_16_9 -> 3840 to 2160
                VideoAspectRatio.RATIO_9_16 -> 2160 to 3840
                VideoAspectRatio.RATIO_1_1 -> 2160 to 2160
            }
        }
        val caps = runCatching {
            val list = MediaCodecList(MediaCodecList.REGULAR_CODECS)
            list.codecInfos.firstOrNull {
                it.isEncoder && it.supportedTypes.contains(MediaFormat.MIMETYPE_VIDEO_AVC)
            }?.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC)?.videoCapabilities
        }.getOrNull()

        if (caps != null && caps.isSizeSupported(desired.first, desired.second)) {
            return desired
        }
        val fallback1080 = aspectRatio.width1080 to aspectRatio.height1080
        if (caps != null && caps.isSizeSupported(fallback1080.first, fallback1080.second)) {
            return fallback1080
        }
        Log.w(TAG, "Clamping render dimensions to 720p tier for hardware encoder compatibility.")
        return aspectRatio.width720 to aspectRatio.height720
    }

    private fun drawCartoonFrame(
        canvas: Canvas,
        width: Int,
        height: Int,
        movieTitle: String,
        input: SceneRenderInput,
        nextBitmap: Bitmap?,
        localFrame: Int,
        sceneTotalFrames: Int,
        isFirstSceneOpening: Boolean,
        fps: Int
    ) {
        canvas.drawColor(Color.BLACK)
        val t = (localFrame.toFloat() / max(1, sceneTotalFrames - 1)).coerceIn(0f, 1f)
        val currentSec = localFrame.toFloat() / fps

        // Ken Burns motion calculation
        val motion = CameraMotion.fromKey(input.scene.cameraMotion)
        val zoom = when (motion) {
            CameraMotion.ZOOM_IN -> 1.0f + 0.14f * t
            CameraMotion.ZOOM_OUT -> 1.14f - 0.14f * t
            CameraMotion.PAN_LEFT, CameraMotion.PAN_RIGHT -> 1.10f
            CameraMotion.STATIC -> 1.03f
        }
        val panX = when (motion) {
            CameraMotion.PAN_LEFT -> (0.5f - t) * 0.08f * width
            CameraMotion.PAN_RIGHT -> (t - 0.5f) * 0.08f * width
            else -> 0f
        }

        drawScaledCropBitmap(canvas, input.bitmap, width, height, zoom, panX, 255)

        // Subtle cross-fade at scene end
        val fadeFrames = (fps * 0.45f).toInt().coerceAtLeast(1)
        if (nextBitmap != null && localFrame > sceneTotalFrames - fadeFrames) {
            val fadeAlpha = (((localFrame - (sceneTotalFrames - fadeFrames)).toFloat() / fadeFrames) * 255).toInt()
                .coerceIn(0, 255)
            drawScaledCropBitmap(canvas, nextBitmap, width, height, 1.0f, 0f, fadeAlpha)
        }

        // Animated ambient sparkles
        val sparklePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(110, 178, 152, 255)
        }
        for (s in 0..7) {
            val px = ((s * 173 + localFrame * (2 + s % 3)) % width).toFloat()
            val py = ((s * 119 + sin((localFrame + s * 10) * 0.08) * 35) % height).toFloat()
            val radius = 2.5f + (s % 3) * 1.5f
            canvas.drawCircle(px, py, radius, sparklePaint)
        }

        // Active dialogue, lip-sync mouth badge & subtitle bar
        val activeDialogue = input.audio.dialogueTimings.firstOrNull {
            currentSec >= it.startSec && currentSec <= it.endSec
        }
        val mouthOpen = input.audio.lipSyncEnvelope.getOrElse(localFrame) { 0f }

        if (activeDialogue != null) {
            drawDialogueAndLipSyncOverlay(
                canvas = canvas,
                width = width,
                height = height,
                speaker = activeDialogue.speaker,
                expression = activeDialogue.expression,
                line = activeDialogue.line,
                mouthOpen = mouthOpen
            )
        }

        // Opening movie title card badge
        if (isFirstSceneOpening) {
            val alpha = ((1f - (localFrame / (fps * 2f))) * 255).toInt().coerceIn(0, 255)
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(alpha, 255, 255, 255)
                textSize = width * 0.042f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                setShadowLayer(12f, 0f, 4f, Color.argb(alpha, 90, 40, 200))
            }
            canvas.drawText(movieTitle, width * 0.05f, height * 0.11f, titlePaint)
        }
    }

    private fun drawScaledCropBitmap(
        canvas: Canvas,
        bmp: Bitmap,
        targetW: Int,
        targetH: Int,
        zoom: Float,
        panX: Float,
        alpha: Int
    ) {
        val baseScale = max(targetW.toFloat() / bmp.width, targetH.toFloat() / bmp.height) * zoom
        val drawW = bmp.width * baseScale
        val drawH = bmp.height * baseScale
        val left = (targetW - drawW) / 2f + panX
        val top = (targetH - drawH) / 2f
        val dst = RectF(left, top, left + drawW, top + drawH)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply { this.alpha = alpha }
        canvas.drawBitmap(bmp, Rect(0, 0, bmp.width, bmp.height), dst, paint)
    }

    private fun drawDialogueAndLipSyncOverlay(
        canvas: Canvas,
        width: Int,
        height: Int,
        speaker: String,
        expression: String,
        line: String,
        mouthOpen: Float
    ) {
        val boxPadX = width * 0.05f
        val boxBottom = height * 0.94f
        val boxHeight = height * 0.17f
        val boxRect = RectF(boxPadX, boxBottom - boxHeight, width - boxPadX, boxBottom)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                boxRect.left, boxRect.top, boxRect.right, boxRect.bottom,
                Color.argb(215, 16, 12, 38),
                Color.argb(215, 24, 34, 82),
                Shader.TileMode.CLAMP
            )
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = Color.argb(200, 139, 92, 246)
        }
        canvas.drawRoundRect(boxRect, 24f, 24f, bgPaint)
        canvas.drawRoundRect(boxRect, 24f, 24f, borderPaint)

        // Animated lip-sync avatar mouth circle on left of subtitle card
        val avatarCx = boxRect.left + height * 0.065f
        val avatarCy = boxRect.centerY()
        val avatarR = height * 0.042f
        val avatarRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(124, 58, 237)
        }
        canvas.drawCircle(avatarCx, avatarCy, avatarR, avatarRingPaint)

        // Lip-sync mouth opening & closing with voice amplitude
        val mouthH = (avatarR * 0.18f) + (avatarR * 0.55f * mouthOpen)
        val mouthRect = RectF(
            avatarCx - avatarR * 0.45f,
            avatarCy + avatarR * 0.05f - mouthH * 0.5f,
            avatarCx + avatarR * 0.45f,
            avatarCy + avatarR * 0.05f + mouthH * 0.5f
        )
        val mouthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(255, 190, 215)
        }
        canvas.drawOval(mouthRect, mouthPaint)

        // Eyes above animated mouth
        val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        canvas.drawCircle(avatarCx - avatarR * 0.3f, avatarCy - avatarR * 0.28f, avatarR * 0.13f, eyePaint)
        canvas.drawCircle(avatarCx + avatarR * 0.3f, avatarCy - avatarR * 0.28f, avatarR * 0.13f, eyePaint)

        // Speaker header + Subtitle text
        val textLeft = avatarCx + avatarR + width * 0.025f
        val speakerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(96, 165, 250)
            textSize = height * 0.028f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(
            "$speaker • ${expression.lowercase().replaceFirstChar { it.uppercase() }}",
            textLeft,
            boxRect.top + height * 0.045f,
            speakerPaint
        )

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = height * 0.032f
        }
        val availWidth = (boxRect.right - textLeft - width * 0.025f).toInt().coerceAtLeast(100)
        val layout = StaticLayout.Builder.obtain(line, 0, line.length, textPaint, availWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setMaxLines(2)
            .build()
        canvas.save()
        canvas.translate(textLeft, boxRect.top + height * 0.06f)
        layout.draw(canvas)
        canvas.restore()
    }
}
