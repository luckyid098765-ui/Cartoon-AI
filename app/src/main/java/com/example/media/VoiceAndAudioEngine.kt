package com.example.media

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.data.model.CartoonLanguage
import com.example.data.model.DialogueLine
import com.example.data.model.MoodMusic
import com.example.data.model.SfxCue
import com.example.data.model.VoiceGender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sin

private const val TAG = "VoiceAndAudioEngine"

data class SynthesizedSceneAudio(
    val sceneIndex: Int,
    val pcmSamples: ShortArray,
    val sampleRate: Int,
    val durationSec: Float,
    val lipSyncEnvelope: FloatArray,
    val dialogueTimings: List<DialogueTiming>
)

data class DialogueTiming(
    val speaker: String,
    val expression: String,
    val line: String,
    val startSec: Float,
    val endSec: Float
)

class VoiceAndAudioEngine(private val context: Context) {

    companion object {
        const val TARGET_SAMPLE_RATE = 22050
        const val LIP_SYNC_FPS = 24
    }

    suspend fun synthesizeSceneAudio(
        sceneIndex: Int,
        dialogues: List<DialogueLine>,
        language: CartoonLanguage,
        moodMusic: MoodMusic,
        sfxCue: SfxCue,
        targetMinSeconds: Int,
        characterPitches: Map<String, Float>
    ): SynthesizedSceneAudio = withContext(Dispatchers.IO) {
        val tts = initTextToSpeech(language)
        val tempDir = File(context.cacheDir, "tts_tmp").apply { mkdirs() }

        val dialogueSegments = mutableListOf<Pair<DialogueTiming, ShortArray>>()
        var cursorSec = 0.4f

        try {
            for ((idx, dlg) in dialogues.withIndex()) {
                val wavFile = File(tempDir, "scene_${sceneIndex}_dlg_${idx}.wav")
                val customPitch = characterPitches.entries
                    .firstOrNull { it.key.equals(dlg.speaker, ignoreCase = true) }?.value
                val basePitch = customPitch ?: if (dlg.gender == VoiceGender.FEMALE.key) 1.24f else 0.90f
                val speechRate = if (dlg.gender == VoiceGender.FEMALE.key) 1.04f else 0.96f

                val synthesizedOk = if (tts != null) {
                    synthesizeToWav(tts, dlg.line, basePitch, speechRate, language, wavFile)
                } else false

                val rawSamples = if (synthesizedOk && wavFile.exists() && wavFile.length() > 44) {
                    readWavAsMonoPcm(wavFile, TARGET_SAMPLE_RATE)
                } else {
                    // Fallback spoken cadence envelope if device lacks specific TTS language pack
                    ShortArray((dlg.durationSec * TARGET_SAMPLE_RATE).coerceAtLeast(TARGET_SAMPLE_RATE))
                }
                wavFile.delete()

                val segDurSec = (rawSamples.size.toFloat() / TARGET_SAMPLE_RATE)
                    .coerceAtLeast(dlg.durationSec.toFloat().coerceAtLeast(2.5f))

                dialogueSegments.add(
                    DialogueTiming(
                        speaker = dlg.speaker,
                        expression = dlg.expression,
                        line = dlg.line,
                        startSec = cursorSec,
                        endSec = cursorSec + segDurSec
                    ) to rawSamples
                )
                cursorSec += segDurSec + 0.45f
            }
        } finally {
            tts?.stop()
            tts?.shutdown()
        }

        val totalDurationSec = maxOf(cursorSec + 0.4f, targetMinSeconds.toFloat())
        val totalSamples = (totalDurationSec * TARGET_SAMPLE_RATE).toInt().coerceAtLeast(TARGET_SAMPLE_RATE)
        val mixed = FloatArray(totalSamples)

        // 1. Add procedural cartoon background music
        val bgm = generateCartoonBgm(moodMusic, totalSamples, TARGET_SAMPLE_RATE)
        for (i in 0 until totalSamples) {
            mixed[i] += bgm[i] * 0.22f
        }

        // 2. Add scene SFX cue near opening
        val sfx = generateSfx(sfxCue, TARGET_SAMPLE_RATE)
        val sfxOffset = (0.15f * TARGET_SAMPLE_RATE).toInt()
        for (i in sfx.indices) {
            val idx = sfxOffset + i
            if (idx in mixed.indices) {
                mixed[idx] += sfx[i] * 0.42f
            }
        }

        // 3. Mix dialogue voices and compute lip-sync mouth envelope at 24fps
        val totalFrames = (totalDurationSec * LIP_SYNC_FPS).toInt().coerceAtLeast(1)
        val lipEnvelope = FloatArray(totalFrames)
        val samplesPerFrame = TARGET_SAMPLE_RATE / LIP_SYNC_FPS

        for ((timing, samples) in dialogueSegments) {
            val startIdx = (timing.startSec * TARGET_SAMPLE_RATE).toInt()
            for (i in samples.indices) {
                val outIdx = startIdx + i
                if (outIdx in mixed.indices) {
                    val v = samples[i] / 32768f
                    mixed[outIdx] += v * 0.95f
                }
            }
            // Compute lip-sync envelope from actual dialogue waveform energy
            val startFrame = (timing.startSec * LIP_SYNC_FPS).toInt()
            val endFrame = (timing.endSec * LIP_SYNC_FPS).toInt().coerceAtMost(totalFrames - 1)
            for (f in startFrame..endFrame) {
                val relSampleStart = ((f - startFrame) * samplesPerFrame).coerceAtLeast(0)
                if (relSampleStart < samples.size) {
                    val relEnd = min(relSampleStart + samplesPerFrame, samples.size)
                    var peak = 0f
                    for (s in relSampleStart until relEnd) {
                        val a = abs(samples[s] / 32768f)
                        if (a > peak) peak = a
                    }
                    lipEnvelope[f] = (peak * 2.4f).coerceIn(0.08f, 1.0f)
                } else {
                    // Gentle cadence pulse if audio ended slightly before duration
                    lipEnvelope[f] = if (f % 4 < 2) 0.45f else 0.15f
                }
            }
        }

        val finalPcm = ShortArray(totalSamples) { i ->
            (mixed[i].coerceIn(-0.98f, 0.98f) * 32767f).toInt().toShort()
        }

        SynthesizedSceneAudio(
            sceneIndex = sceneIndex,
            pcmSamples = finalPcm,
            sampleRate = TARGET_SAMPLE_RATE,
            durationSec = totalDurationSec,
            lipSyncEnvelope = lipEnvelope,
            dialogueTimings = dialogueSegments.map { it.first }
        )
    }

    private suspend fun initTextToSpeech(language: CartoonLanguage): TextToSpeech? =
        suspendCancellableCoroutine { cont ->
            var engine: TextToSpeech? = null
            engine = TextToSpeech(context.applicationContext) { status ->
                if (!cont.isActive) return@TextToSpeech
                if (status == TextToSpeech.SUCCESS) {
                    val locale = Locale.forLanguageTag(language.localeTag)
                    val res = engine?.setLanguage(locale)
                    if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.w(TAG, "Locale ${language.localeTag} not fully available on device; using default fallback.")
                        engine?.setLanguage(Locale.US)
                    }
                    cont.resume(engine)
                } else {
                    Log.w(TAG, "TextToSpeech init failed with status=$status")
                    engine?.shutdown()
                    cont.resume(null)
                }
            }
            cont.invokeOnCancellation {
                engine?.shutdown()
            }
        }

    private suspend fun synthesizeToWav(
        tts: TextToSpeech,
        text: String,
        pitch: Float,
        speechRate: Float,
        language: CartoonLanguage,
        outFile: File
    ): Boolean = suspendCancellableCoroutine { cont ->
        val utteranceId = "utt_${System.nanoTime()}"
        tts.setPitch(pitch.coerceIn(0.65f, 1.55f))
        tts.setSpeechRate(speechRate.coerceIn(0.8f, 1.25f))

        // Try matching a gender/locale voice if installed on device
        runCatching {
            val tagPrefix = language.localeTag.substringBefore("-")
            val matchingVoices = tts.voices?.filter {
                it.locale.language.equals(tagPrefix, ignoreCase = true)
            }.orEmpty()
            if (matchingVoices.isNotEmpty()) {
                val chosen = if (pitch > 1.05f) matchingVoices.first() else matchingVoices.last()
                tts.voice = chosen
            }
        }

        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {}
            override fun onDone(id: String?) {
                if (id == utteranceId && cont.isActive) cont.resume(true)
            }
            @Deprecated("Deprecated in Java")
            override fun onError(id: String?) {
                if (id == utteranceId && cont.isActive) cont.resume(false)
            }
            override fun onError(id: String?, errorCode: Int) {
                if (id == utteranceId && cont.isActive) cont.resume(false)
            }
        })

        val res = tts.synthesizeToFile(text, Bundle(), outFile, utteranceId)
        if (res != TextToSpeech.SUCCESS && cont.isActive) {
            cont.resume(false)
        }
    }

    private fun readWavAsMonoPcm(file: File, targetRate: Int): ShortArray {
        val bytes = file.readBytes()
        if (bytes.size < 44) return ShortArray(0)
        val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val channels = bb.getShort(22).toInt().coerceIn(1, 2)
        val srcRate = bb.getInt(24).coerceIn(8000, 48000)
        val bitsPerSample = bb.getShort(34).toInt().coerceIn(8, 16)

        // Locate "data" chunk safely
        var dataOffset = 12
        var dataSize = bytes.size - 44
        while (dataOffset + 8 < bytes.size) {
            val id = String(bytes, dataOffset, 4, Charsets.US_ASCII)
            val chunkSize = bb.getInt(dataOffset + 4)
            if (id == "data") {
                dataOffset += 8
                dataSize = chunkSize.coerceAtMost(bytes.size - dataOffset)
                break
            }
            dataOffset += 8 + chunkSize.coerceAtLeast(0)
        }

        val frameBytes = (bitsPerSample / 8) * channels
        val numFrames = (dataSize / frameBytes).coerceAtLeast(0)
        val mono = ShortArray(numFrames)
        for (i in 0 until numFrames) {
            val pos = dataOffset + i * frameBytes
            if (pos + 1 < bytes.size && bitsPerSample == 16) {
                mono[i] = bb.getShort(pos)
            }
        }

        if (srcRate == targetRate || mono.isEmpty()) return mono
        val outLen = ((mono.size.toLong() * targetRate) / srcRate).toInt().coerceAtLeast(1)
        return ShortArray(outLen) { i ->
            val srcIdx = ((i.toLong() * srcRate) / targetRate).toInt().coerceIn(0, mono.lastIndex)
            mono[srcIdx]
        }
    }

    private fun generateCartoonBgm(mood: MoodMusic, sampleCount: Int, sampleRate: Int): FloatArray {
        val out = FloatArray(sampleCount)
        val freqs = when (mood) {
            MoodMusic.PLAYFUL -> floatArrayOf(261.63f, 329.63f, 392.00f, 523.25f)
            MoodMusic.ADVENTURE -> floatArrayOf(220.00f, 261.63f, 329.63f, 440.00f)
            MoodMusic.MYSTERY -> floatArrayOf(196.00f, 233.08f, 293.66f, 349.23f)
            MoodMusic.EPIC -> floatArrayOf(174.61f, 220.00f, 261.63f, 349.23f)
            MoodMusic.COMEDY -> floatArrayOf(293.66f, 369.99f, 440.00f, 587.33f)
        }
        val beatSamples = (sampleRate * 0.45f).toInt()
        for (i in 0 until sampleCount) {
            val t = i.toFloat() / sampleRate
            val noteIdx = (i / beatSamples) % freqs.size
            val beatPos = (i % beatSamples).toFloat() / beatSamples
            val env = kotlin.math.exp(-beatPos * 3.2f)
            val f = freqs[noteIdx]
            val pad = sin(2.0 * PI * (f * 0.5) * t).toFloat() * 0.35f
            val pluck = sin(2.0 * PI * f * t).toFloat() * env * 0.55f
            out[i] = pad + pluck
        }
        return out
    }

    private fun generateSfx(cue: SfxCue, sampleRate: Int): FloatArray {
        val len = (sampleRate * 0.55f).toInt()
        val out = FloatArray(len)
        when (cue) {
            SfxCue.SPARKLE -> {
                val notes = floatArrayOf(880f, 1174f, 1568f, 2093f)
                for (i in 0 until len) {
                    val t = i.toFloat() / sampleRate
                    val f = notes[(i / (len / 4)).coerceIn(0, 3)]
                    val env = 1f - (i.toFloat() / len)
                    out[i] = sin(2.0 * PI * f * t).toFloat() * env
                }
            }
            SfxCue.WHOOSH -> {
                for (i in 0 until len) {
                    val p = i.toFloat() / len
                    val f = 140f + 420f * sin(PI * p).toFloat()
                    val env = sin(PI * p).toFloat()
                    out[i] = sin(2.0 * PI * f * (i.toFloat() / sampleRate)).toFloat() * env
                }
            }
            SfxCue.POP -> {
                val popLen = (sampleRate * 0.14f).toInt()
                for (i in 0 until popLen) {
                    val p = i.toFloat() / popLen
                    val f = 320f + 500f * p
                    out[i] = sin(2.0 * PI * f * (i.toFloat() / sampleRate)).toFloat() * (1f - p)
                }
            }
            SfxCue.THUNDER -> {
                for (i in 0 until len) {
                    val p = i.toFloat() / len
                    val f = 55f + 25f * sin(12.0 * p).toFloat()
                    out[i] = sin(2.0 * PI * f * (i.toFloat() / sampleRate)).toFloat() * (1f - p)
                }
            }
            SfxCue.FOOTSTEPS -> {
                for (step in 0..2) {
                    val start = step * (len / 3)
                    for (j in 0 until (sampleRate * 0.06f).toInt()) {
                        val idx = start + j
                        if (idx < len) {
                            val env = 1f - (j.toFloat() / (sampleRate * 0.06f))
                            out[idx] = sin(2.0 * PI * 110.0 * (j.toFloat() / sampleRate)).toFloat() * env
                        }
                    }
                }
            }
            SfxCue.NONE -> Unit
        }
        return out
    }
}
