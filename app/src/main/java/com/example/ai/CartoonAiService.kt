package com.example.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.data.model.BgmTrackOption
import com.example.data.model.CameraMotion
import com.example.data.model.CartoonLanguage
import com.example.data.model.CartoonStyle
import com.example.data.model.CharacterExpression
import com.example.data.model.DialogueLine
import com.example.data.model.GeneratedCharacterSpec
import com.example.data.model.GeneratedMovieScript
import com.example.data.model.GeneratedSceneSpec
import com.example.data.model.MoodMusic
import com.example.data.model.SavedCharacter
import com.example.data.model.SfxCue
import com.example.data.model.StoryGenre
import com.example.data.model.VideoAspectRatio
import com.example.data.model.VideoDurationOption
import com.example.data.model.VoiceGender
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.AspectRatio
import com.google.firebase.ai.type.ImageConfig
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ImageSize
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "CartoonAiService"

class CartoonAiService {

    private val storyGeneratorModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash"
        )
    }

    private val scriptSchema = Schema.obj(
        properties = mapOf(
            "title" to Schema.string(description = "Catchy cartoon movie title"),
            "logline" to Schema.string(description = "One-sentence summary of the movie"),
            "characters" to Schema.array(
                description = "Main recurring characters in the cartoon",
                items = Schema.obj(
                    properties = mapOf(
                        "name" to Schema.string(description = "Character name"),
                        "role" to Schema.string(description = "Role such as Hero, Sidekick, Mentor, Villain"),
                        "appearance" to Schema.string(description = "Consistent physical traits: species/age, hair, eyes, skin/fur colors, accessories"),
                        "outfit" to Schema.string(description = "Consistent clothing and colors worn in every scene"),
                        "voiceGender" to Schema.enumeration(listOf("MALE", "FEMALE")),
                        "voicePitch" to Schema.double(description = "Voice pitch multiplier between 0.75 and 1.35")
                    )
                )
            ),
            "scenes" to Schema.array(
                description = "Ordered list of connected cartoon scenes",
                items = Schema.obj(
                    properties = mapOf(
                        "title" to Schema.string(description = "Short scene title"),
                        "visualPrompt" to Schema.string(description = "Detailed visual scene description including setting, action, lighting, and exact character appearance/clothes"),
                        "cameraMotion" to Schema.enumeration(listOf("PAN_LEFT", "PAN_RIGHT", "ZOOM_IN", "ZOOM_OUT", "STATIC")),
                        "moodMusic" to Schema.enumeration(
                            listOf("PLAYFUL", "ADVENTURE", "MYSTERY", "EPIC", "COMEDY", "EMOTIONAL", "SPOOKY", "UPBEAT_KIDS")
                        ),
                        "sfxCue" to Schema.enumeration(listOf("WHOOSH", "SPARKLE", "POP", "THUNDER", "FOOTSTEPS", "NONE")),
                        "characterNames" to Schema.array(items = Schema.string()),
                        "dialogues" to Schema.array(
                            items = Schema.obj(
                                properties = mapOf(
                                    "speaker" to Schema.string(),
                                    "gender" to Schema.enumeration(listOf("MALE", "FEMALE")),
                                    "expression" to Schema.enumeration(listOf("HAPPY", "EXCITED", "BRAVE", "FUNNY", "SURPRISED", "CALM")),
                                    "line" to Schema.string(description = "Spoken dialogue line in the requested language"),
                                    "durationSec" to Schema.integer(description = "Estimated spoken seconds between 3 and 8")
                                )
                            )
                        )
                    )
                )
            )
        )
    )

    private val scriptModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = scriptSchema
            }
        )
    }

    private fun buildImageModel(aspectRatio: VideoAspectRatio) = Firebase.ai.generativeModel(
        modelName = "gemini-nano-banana-2.1",
        generationConfig = generationConfig {
            responseModalities = listOf(ResponseModality.IMAGE)
            imageConfig = ImageConfig.builder()
                .setAspectRatio(
                    when (aspectRatio) {
                        VideoAspectRatio.RATIO_16_9 -> AspectRatio.LANDSCAPE_16x9
                        VideoAspectRatio.RATIO_9_16 -> AspectRatio.PORTRAIT_9x16
                        VideoAspectRatio.RATIO_1_1 -> AspectRatio.SQUARE_1x1
                    }
                )
                .setImageSize(ImageSize.SIZE_1K)
                .build()
        }
    )

    private val characterSheetModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-nano-banana-2.1",
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
            }
        )
    }

    suspend fun generateStoryIdea(
        seedPrompt: String,
        style: CartoonStyle,
        language: CartoonLanguage,
        genre: StoryGenre = StoryGenre.AUTO
    ): String {
        val instruction = buildString {
            appendLine("You are an award-winning cartoon screenwriter.")
            appendLine("Write a vivid, family-friendly cartoon story outline (4-6 sentences) suitable for a ${style.label} movie.")
            appendLine("Genre tone: ${genre.label} (${genre.promptGuidance})")
            appendLine("Write the story in ${language.label}.")
            appendLine("Include 2-3 memorable characters with distinct visual traits and outfits, a clear adventure/conflict, and a fun ending.")
            if (seedPrompt.isNotBlank()) {
                appendLine("Expand and enrich this starting idea: \"$seedPrompt\"")
            } else {
                appendLine("Invent an original, imaginative cartoon story.")
            }
            appendLine("Return only the plain story text, no titles or markdown bullets.")
        }

        return try {
            val response = withTimeout(45_000L) {
                storyGeneratorModel.generateContent(instruction)
            }
            val text = response.text?.trim()
            if (text.isNullOrBlank()) {
                throw IllegalStateException("AI returned an empty story. Please try again.")
            }
            text
        } catch (e: TimeoutCancellationException) {
            throw IllegalStateException("Story generation timed out. Check your connection and retry.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "generateStoryIdea failed", e)
            throw IllegalStateException(
                "AI Story service error: ${e.localizedMessage ?: "Firebase AI Logic is unavailable or not configured."}",
                e
            )
        }
    }

    suspend fun generateFullScript(
        prompt: String,
        style: CartoonStyle,
        targetSeconds: Int,
        sceneCount: Int,
        language: CartoonLanguage,
        lockedCharacters: List<SavedCharacter>,
        genre: StoryGenre = StoryGenre.AUTO,
        bgmOption: BgmTrackOption = BgmTrackOption.AUTO
    ): GeneratedMovieScript {
        val clampedSeconds = targetSeconds.coerceIn(10, VideoDurationOption.MAX_STORY_SECONDS)
        val totalScenes = sceneCount.coerceIn(2, 30)
        val durationLabel = if (clampedSeconds >= 60) "${clampedSeconds / 60} min" else "${clampedSeconds}s"
        val maxBatchSize = 10

        val castSection = if (lockedCharacters.isNotEmpty()) {
            buildString {
                appendLine("MANDATORY SAVED CHARACTERS (must appear consistently with these exact names, appearances, outfits, and voice genders):")
                lockedCharacters.forEach { ch ->
                    appendLine("- ${ch.name} (${ch.role}): Appearance=${ch.appearance}; Outfit=${ch.outfit}; Voice=${ch.voiceGender}, Pitch=${ch.voicePitch}")
                }
            }
        } else {
            "Create 2 to 3 distinct recurring cartoon characters with vivid, consistent appearances and outfits."
        }

        val allScenes = mutableListOf<GeneratedSceneSpec>()
        var movieTitle = "Cartoon AI Story"
        var movieLogline = ""
        var establishedCharacters = emptyList<GeneratedCharacterSpec>()

        var remaining = totalScenes
        var batchIndex = 0
        while (remaining > 0) {
            val batchCount = remaining.coerceAtMost(maxBatchSize)
            val startSceneNum = allScenes.size + 1
            val endSceneNum = allScenes.size + batchCount

            val scriptPrompt = buildString {
                appendLine("You are the showrunner and director of CARTOON AI.")
                appendLine("Convert the user's cartoon story into a continuous multi-scene animated movie script.")
                appendLine()
                appendLine("USER STORY PROMPT: \"$prompt\"")
                appendLine("VISUAL STYLE: ${style.label} (${style.promptAnchor})")
                appendLine("STORY GENRE: ${genre.label} — ${genre.promptGuidance}")
                if (bgmOption.fixedMood != null) {
                    appendLine("PREFERRED SOUNDTRACK MOOD: ${bgmOption.fixedMood.key} (${bgmOption.label})")
                }
                appendLine("STRICT MAXIMUM DURATION: $durationLabel (${clampedSeconds}s total across $totalScenes scenes — DO NOT exceed this pace).")
                appendLine("DIALOGUE LANGUAGE: ${language.label} — ALL spoken dialogue lines MUST be written naturally in ${language.label}.")
                appendLine("CURRENT BATCH: Generate exactly $batchCount connected scenes (Scenes $startSceneNum to $endSceneNum out of $totalScenes total scenes).")
                if (batchIndex == 0) {
                    appendLine(castSection)
                } else {
                    appendLine("CONTINUING STORY CHAPTER ${batchIndex + 1}:")
                    appendLine("Movie Title: \"$movieTitle\". Logline: $movieLogline")
                    appendLine("KEEP THESE EXACT ESTABLISHED CHARACTERS (same names, outfits, voices):")
                    establishedCharacters.forEach { ch ->
                        appendLine("- ${ch.name} (${ch.role}): Appearance=${ch.appearance}; Outfit=${ch.outfit}; Voice=${ch.voiceGender.key}, Pitch=${ch.voicePitch}")
                    }
                    val lastScene = allScenes.lastOrNull()
                    if (lastScene != null) {
                        appendLine("PREVIOUS SCENE ENDED WITH: Scene ${lastScene.sceneIndex + 1} '${lastScene.title}' — ${lastScene.visualPrompt}")
                        appendLine("Continue the exact same storyline seamlessly from Scene $startSceneNum to Scene $endSceneNum.")
                    }
                }
                appendLine()
                appendLine("CHARACTER & STORY CONTINUITY RULES:")
                appendLine("1. Every scene's visualPrompt must repeat the exact appearance and outfit of characters present in that scene so image generation stays 100% consistent.")
                appendLine("2. Each scene must include 1 to 3 spoken dialogue lines with speaker name, gender, expression, natural dialogue in ${language.label}, and spoken durationSec (3..8).")
                appendLine("3. Choose appropriate cameraMotion, moodMusic, and sfxCue per scene.")
            }

            try {
                val response = withTimeout(75_000L) {
                    scriptModel.generateContent(scriptPrompt)
                }
                val jsonText = response.text?.takeIf { it.isNotBlank() }
                    ?: throw IllegalStateException("AI script model returned an empty response.")
                val parsedBatch = parseMovieScript(jsonText, lockedCharacters, batchCount)
                if (batchIndex == 0) {
                    movieTitle = parsedBatch.title
                    movieLogline = parsedBatch.logline
                    establishedCharacters = parsedBatch.characters
                }
                val offset = allScenes.size
                parsedBatch.scenes.forEachIndexed { idx, sc ->
                    allScenes.add(sc.copy(sceneIndex = offset + idx))
                }
                remaining -= parsedBatch.scenes.size.coerceAtLeast(1)
                batchIndex++
            } catch (e: TimeoutCancellationException) {
                throw IllegalStateException("Script generation timed out. Please retry.", e)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "generateFullScript failed", e)
                throw IllegalStateException(
                    "AI Script service error: ${e.localizedMessage ?: "Firebase AI Logic is not configured or unavailable."}",
                    e
                )
            }
        }

        return GeneratedMovieScript(
            title = movieTitle,
            logline = movieLogline,
            characters = establishedCharacters,
            scenes = allScenes.take(totalScenes)
        )
    }

    private fun parseMovieScript(
        jsonString: String,
        lockedCharacters: List<SavedCharacter>,
        targetScenes: Int
    ): GeneratedMovieScript {
        val root = JSONObject(jsonString)
        val title = root.optString("title", "Cartoon AI Adventure").trim().ifBlank { "Cartoon AI Adventure" }
        val logline = root.optString("logline", "").trim()

        val parsedChars = mutableListOf<GeneratedCharacterSpec>()
        val charArray = root.optJSONArray("characters")
        if (charArray != null) {
            for (i in 0 until charArray.length()) {
                val obj = charArray.getJSONObject(i)
                val name = obj.optString("name", "Hero ${i + 1}").trim()
                val locked = lockedCharacters.firstOrNull { it.name.equals(name, ignoreCase = true) }
                parsedChars.add(
                    GeneratedCharacterSpec(
                        name = locked?.name ?: name,
                        role = locked?.role ?: obj.optString("role", "Hero"),
                        appearance = locked?.appearance ?: obj.optString("appearance", "Expressive cartoon character"),
                        outfit = locked?.outfit ?: obj.optString("outfit", "Colorful adventure outfit"),
                        voiceGender = if (locked != null) VoiceGender.fromKey(locked.voiceGender)
                        else VoiceGender.fromKey(obj.optString("voiceGender", "MALE")),
                        voicePitch = locked?.voicePitch?.toFloat()
                            ?: obj.optDouble("voicePitch", 1.0).toFloat().coerceIn(0.7f, 1.4f)
                    )
                )
            }
        }
        lockedCharacters.forEach { locked ->
            if (parsedChars.none { it.name.equals(locked.name, ignoreCase = true) }) {
                parsedChars.add(
                    GeneratedCharacterSpec(
                        name = locked.name,
                        role = locked.role,
                        appearance = locked.appearance,
                        outfit = locked.outfit,
                        voiceGender = VoiceGender.fromKey(locked.voiceGender),
                        voicePitch = locked.voicePitch.toFloat().coerceIn(0.7f, 1.4f)
                    )
                )
            }
        }

        val parsedScenes = mutableListOf<GeneratedSceneSpec>()
        val scenesArray = root.optJSONArray("scenes")
        if (scenesArray != null) {
            val count = scenesArray.length().coerceAtMost(targetScenes.coerceAtLeast(1))
            for (i in 0 until count) {
                val obj = scenesArray.getJSONObject(i)
                val namesArr = obj.optJSONArray("characterNames")
                val charNames = buildList {
                    if (namesArr != null) {
                        for (j in 0 until namesArr.length()) {
                            add(namesArr.getString(j))
                        }
                    }
                }
                val dlgArr = obj.optJSONArray("dialogues")
                val dialogues = buildList {
                    if (dlgArr != null) {
                        for (j in 0 until dlgArr.length()) {
                            val d = dlgArr.getJSONObject(j)
                            val lineText = d.optString("line", "").trim()
                            if (lineText.isNotEmpty()) {
                                add(
                                    DialogueLine(
                                        speaker = d.optString("speaker", parsedChars.firstOrNull()?.name ?: "Narrator"),
                                        gender = VoiceGender.fromKey(d.optString("gender", "MALE")).key,
                                        expression = CharacterExpression.fromKey(d.optString("expression", "HAPPY")).key,
                                        line = lineText,
                                        durationSec = d.optInt("durationSec", 4).coerceIn(3, 8)
                                    )
                                )
                            }
                        }
                    }
                }.ifEmpty {
                    listOf(
                        DialogueLine(
                            speaker = parsedChars.firstOrNull()?.name ?: "Narrator",
                            gender = parsedChars.firstOrNull()?.voiceGender?.key ?: VoiceGender.MALE.key,
                            expression = CharacterExpression.EXCITED.key,
                            line = obj.optString("title", "The adventure continues!"),
                            durationSec = 4
                        )
                    )
                }

                parsedScenes.add(
                    GeneratedSceneSpec(
                        sceneIndex = i,
                        title = obj.optString("title", "Scene ${i + 1}").trim().ifBlank { "Scene ${i + 1}" },
                        visualPrompt = obj.optString("visualPrompt", "Colorful cartoon scene"),
                        cameraMotion = CameraMotion.fromKey(obj.optString("cameraMotion")),
                        moodMusic = MoodMusic.fromKey(obj.optString("moodMusic")),
                        sfxCue = SfxCue.fromKey(obj.optString("sfxCue")),
                        characterNames = charNames,
                        dialogues = dialogues
                    )
                )
            }
        }

        if (parsedScenes.isEmpty()) {
            throw IllegalStateException("Script model did not produce any scenes.")
        }

        return GeneratedMovieScript(
            title = title,
            logline = logline,
            characters = parsedChars,
            scenes = parsedScenes
        )
    }

    suspend fun generateCharacterPortrait(
        character: GeneratedCharacterSpec,
        style: CartoonStyle
    ): Bitmap {
        val prompt = buildString {
            append("${style.promptAnchor}. ")
            append("Character model sheet portrait, centered medium close-up, front-facing expressive pose. ")
            append("Character name: ${character.name} (${character.role}). ")
            append("Appearance: ${character.appearance}. ")
            append("Outfit: ${character.outfit}. ")
            append("Clean studio dark indigo-purple backdrop, crisp silhouette, high detail, NO text or watermarks.")
        }

        return try {
            val response = withTimeout(60_000L) {
                characterSheetModel.generateContent(prompt)
            }
            val imagePart = response.candidates.firstOrNull()
                ?.content?.parts?.filterIsInstance<ImagePart>()
                ?.firstOrNull()
            imagePart?.image
                ?: throw IllegalStateException("Character portrait model returned no image.")
        } catch (e: TimeoutCancellationException) {
            throw IllegalStateException("Character portrait timed out after 60s.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "generateCharacterPortrait failed for ${character.name}", e)
            throw IllegalStateException(
                "AI Character Image error: ${e.localizedMessage ?: "Ensure Firebase Blaze plan is enabled for Gemini image generation."}",
                e
            )
        }
    }

    suspend fun generateSceneArtwork(
        scene: GeneratedSceneSpec,
        characters: List<GeneratedCharacterSpec>,
        referencePortraits: Map<String, Bitmap>,
        style: CartoonStyle,
        aspectRatio: VideoAspectRatio
    ): Bitmap {
        val model = buildImageModel(aspectRatio)
        val activeCharacters = characters.filter { ch ->
            scene.characterNames.any { it.equals(ch.name, ignoreCase = true) }
        }.ifEmpty { characters.take(2) }

        val refBitmaps = activeCharacters.mapNotNull { ch ->
            referencePortraits.entries.firstOrNull { it.key.equals(ch.name, ignoreCase = true) }?.value
        }.take(3)

        val charConsistencyBlock = activeCharacters.joinToString(" | ") { ch ->
            "${ch.name}: ${ch.appearance}, wearing ${ch.outfit}"
        }
        val mainExpression = scene.dialogues.firstOrNull()?.expression ?: "HAPPY"

        val promptText = buildString {
            append("${style.promptAnchor}. ")
            append("Cinematic cartoon movie frame for scene '${scene.title}'. ")
            append("Scene action & environment: ${scene.visualPrompt}. ")
            if (charConsistencyBlock.isNotBlank()) {
                append("Maintain EXACT character appearance and outfits from reference images: $charConsistencyBlock. ")
            }
            append("Primary character facial expression: $mainExpression. ")
            append("Rich vibrant lighting, dynamic composition, NO text, NO speech bubbles, NO captions, NO watermarks.")
        }

        return try {
            val response = withTimeout(60_000L) {
                if (refBitmaps.isNotEmpty()) {
                    model.generateContent(
                        content {
                            refBitmaps.forEach { image(it) }
                            text(promptText)
                        }
                    )
                } else {
                    model.generateContent(promptText)
                }
            }
            val imagePart = response.candidates.firstOrNull()
                ?.content?.parts?.filterIsInstance<ImagePart>()
                ?.firstOrNull()
            imagePart?.image
                ?: throw IllegalStateException("Image model returned no frame for scene ${scene.sceneIndex + 1}.")
        } catch (e: TimeoutCancellationException) {
            throw IllegalStateException("Scene ${scene.sceneIndex + 1} image generation timed out after 60s.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "generateSceneArtwork failed for scene ${scene.sceneIndex}", e)
            throw IllegalStateException(
                "AI Scene Image error: ${e.localizedMessage ?: "Ensure Firebase AI Logic & Blaze billing are active."}",
                e
            )
        }
    }
}
