package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class CartoonStyle(
    val key: String,
    val label: String,
    val promptAnchor: String,
    val tagline: String
) {
    CARTOON_2D(
        key = "2D_CARTOON",
        label = "2D Cartoon",
        promptAnchor = "vibrant classic 2D hand-drawn cartoon animation, clean bold outlines, expressive cel shading, rich saturated colors",
        tagline = "Classic hand-drawn cel animation"
    ),
    CARTOON_3D(
        key = "3D_CARTOON",
        label = "3D Cartoon",
        promptAnchor = "high-end family-friendly Disney and Pixar-style 3D CGI animated movie frame, soft global illumination, subsurface scattering, charming stylized proportions",
        tagline = "Disney/Pixar-inspired 3D animation"
    ),
    ANIME(
        key = "ANIME",
        label = "Anime",
        promptAnchor = "high-budget Japanese anime film still, Makoto Shinkai inspired lighting, crisp linework, dramatic sky and expressive anime eyes",
        tagline = "Dynamic anime cinema aesthetic"
    ),
    COMIC(
        key = "COMIC",
        label = "Comic Animation",
        promptAnchor = "stylized graphic novel comic animation, dynamic ink hatching, halftone texture accents, punchy superhero color contrast",
        tagline = "Bold ink & graphic panel look"
    ),
    FANTASY(
        key = "FANTASY",
        label = "Fantasy Cartoon",
        promptAnchor = "enchanted storybook fantasy cartoon animation, bioluminescent glow, magical particles, whimsical architecture and warm rim light",
        tagline = "Magical glowing storybook worlds"
    ),
    FUNNY(
        key = "FUNNY",
        label = "Funny Cartoon",
        promptAnchor = "zany slapstick Saturday morning cartoon style, exaggerated squash-and-stretch poses, bright playful colors and goofy expressions",
        tagline = "Playful exaggerated comedy"
    ),
    ADVENTURE(
        key = "ADVENTURE",
        label = "Adventure Cartoon",
        promptAnchor = "sweeping action-adventure animated series style, dramatic wide angles, heroic poses, atmospheric dust and sunlight rays",
        tagline = "Epic action & exploration"
    ),
    CINEMATIC(
        key = "CINEMATIC",
        label = "Realistic Cartoon",
        promptAnchor = "realistic stylized feature-film CGI cartoon animation, physically based fur/fabric shaders, anamorphic depth of field, dramatic volumetric lighting",
        tagline = "Realistic cinematic animation"
    );

    companion object {
        fun fromKey(raw: String?): CartoonStyle =
            entries.firstOrNull { it.key == raw } ?: CARTOON_3D
    }
}

enum class VideoDurationOption(
    val key: String,
    val label: String,
    val shortLabel: String,
    val targetSeconds: Int,
    val sceneCount: Int,
    val creditCost: Int
) {
    SEC_10("SEC_10", "10 Seconds", "10s", 10, 2, 0),
    SEC_30("SEC_30", "30 Seconds", "30s", 30, 3, 0),
    MIN_1("MIN_1", "1 Minute", "1m", 60, 4, 0),
    MIN_2("MIN_2", "2 Minutes", "2m", 120, 6, 0),
    MIN_5("MIN_5", "5 Minutes", "5m", 300, 10, 0),
    MIN_10("MIN_10", "10 Minutes", "10m", 600, 16, 0),
    MIN_15("MIN_15", "15 Minutes", "15m", 900, 20, 0),
    MIN_20("MIN_20", "20 Minutes", "20m", 1200, 24, 0),
    MIN_30("MIN_30", "30 Minutes (Max)", "30m", 1800, 30, 0);

    companion object {
        const val MIN_STORY_MINUTES = 1
        const val MAX_STORY_MINUTES = 30
        const val MAX_STORY_SECONDS = 1800

        fun fromKey(raw: String?): VideoDurationOption = when (raw) {
            "EXTENDED", "MIN_30" -> MIN_30
            "MIN_25" -> MIN_20
            else -> entries.firstOrNull { it.key == raw } ?: MIN_1
        }

        fun forMinutes(minutes: Int): VideoDurationOption {
            val clamped = minutes.coerceIn(MIN_STORY_MINUTES, MAX_STORY_MINUTES)
            return when {
                clamped <= 1 -> MIN_1
                clamped <= 2 -> MIN_2
                clamped <= 5 -> MIN_5
                clamped <= 10 -> MIN_10
                clamped <= 15 -> MIN_15
                clamped <= 20 -> MIN_20
                else -> MIN_30
            }
        }

        fun sceneCountForMinutes(minutes: Int): Int {
            val clamped = minutes.coerceIn(MIN_STORY_MINUTES, MAX_STORY_MINUTES)
            return when {
                clamped == 1 -> 4
                clamped == 2 -> 6
                clamped <= 5 -> clamped * 2
                clamped <= 10 -> 10 + (clamped - 5)
                clamped <= 20 -> 15 + ((clamped - 10) / 2)
                else -> (20 + (clamped - 20)).coerceAtMost(30)
            }
        }
    }
}

enum class CartoonLanguage(
    val key: String,
    val label: String,
    val localeTag: String
) {
    ENGLISH("ENGLISH", "English", "en-US"),
    HINDI("HINDI", "Hindi (हिन्दी)", "hi-IN"),
    BENGALI("BENGALI", "Bengali (বাংলা)", "bn-IN"),
    SPANISH("SPANISH", "Spanish (Español)", "es-ES"),
    FRENCH("FRENCH", "French (Français)", "fr-FR"),
    ARABIC("ARABIC", "Arabic (العربية)", "ar-SA"),
    TAMIL("TAMIL", "Tamil (தமிழ்)", "ta-IN"),
    JAPANESE("JAPANESE", "Japanese (日本語)", "ja-JP");

    companion object {
        fun fromKey(raw: String?): CartoonLanguage =
            entries.firstOrNull { it.key == raw } ?: ENGLISH
    }
}

enum class StoryGenre(
    val key: String,
    val label: String,
    val promptGuidance: String
) {
    AUTO("AUTO", "Auto Match", "Match the natural tone of the user's story prompt."),
    FUNNY("FUNNY", "Funny", "Playful comedy, witty misunderstandings, slapstick visual gags, and cheerful timing."),
    EMOTIONAL("EMOTIONAL", "Emotional", "Heartwarming emotional arc, deep friendship or family bonds, touching triumph."),
    ADVENTURE("ADVENTURE", "Adventure", "Action-packed quest, brave discoveries, teamwork, and exciting obstacles."),
    HORROR("HORROR", "Mystery / Spooky", "Kid-safe spooky haunted mystery, eerie shadows, suspenseful clues, and brave resolution."),
    FANTASY("FANTASY", "Fantasy", "Magical realms, enchanted creatures, glowing spells, and wonder."),
    EDUCATIONAL("EDUCATIONAL", "Educational", "Clear moral lesson, curiosity-driven facts (science, kindness, nature), engaging storytelling.");

    companion object {
        fun fromKey(raw: String?): StoryGenre =
            entries.firstOrNull { it.key == raw } ?: AUTO
    }
}

enum class BgmTrackOption(
    val key: String,
    val label: String,
    val fixedMood: MoodMusic?
) {
    AUTO("AUTO", "AI Auto Soundtrack", null),
    PLAYFUL("PLAYFUL", "Playful Toybox", MoodMusic.PLAYFUL),
    ADVENTURE("ADVENTURE", "Heroic Adventure", MoodMusic.ADVENTURE),
    EMOTIONAL("EMOTIONAL", "Heartfelt Piano", MoodMusic.EMOTIONAL),
    MYSTERY("MYSTERY", "Spooky / Mystery", MoodMusic.SPOOKY),
    COMEDY("COMEDY", "Bouncy Comedy", MoodMusic.COMEDY),
    EPIC("EPIC", "Cinematic Epic", MoodMusic.EPIC),
    UPBEAT_KIDS("UPBEAT_KIDS", "Upbeat Kids", MoodMusic.UPBEAT_KIDS);

    companion object {
        fun fromKey(raw: String?): BgmTrackOption =
            entries.firstOrNull { it.key == raw } ?: AUTO
    }
}

enum class VideoAspectRatio(
    val key: String,
    val label: String,
    val width480: Int,
    val height480: Int,
    val width720: Int,
    val height720: Int,
    val width1080: Int,
    val height1080: Int
) {
    RATIO_16_9("RATIO_16_9", "16:9 YouTube", 848, 480, 1280, 720, 1920, 1080),
    RATIO_9_16("RATIO_9_16", "9:16 Shorts & Reels", 480, 848, 720, 1280, 1080, 1920),
    RATIO_1_1("RATIO_1_1", "1:1 Square", 480, 480, 720, 720, 1080, 1080);

    companion object {
        fun fromKey(raw: String?): VideoAspectRatio =
            entries.firstOrNull { it.key == raw } ?: RATIO_16_9
    }
}

enum class VideoResolution(
    val key: String,
    val label: String
) {
    RES_480P("RES_480P", "480p SD"),
    RES_720P("RES_720P", "720p HD"),
    RES_1080P("RES_1080P", "1080p Full HD"),
    RES_4K("RES_4K", "4K Ultra HD");

    companion object {
        fun fromKey(raw: String?): VideoResolution =
            entries.firstOrNull { it.key == raw } ?: RES_1080P
    }
}

enum class VoiceGender(val key: String, val label: String) {
    MALE("MALE", "Male"),
    FEMALE("FEMALE", "Female");

    companion object {
        fun fromKey(raw: String?): VoiceGender =
            entries.firstOrNull { it.key == raw?.uppercase() } ?: MALE
    }
}

enum class CharacterExpression(val key: String, val label: String) {
    HAPPY("HAPPY", "Happy"),
    EXCITED("EXCITED", "Excited"),
    BRAVE("BRAVE", "Brave"),
    FUNNY("FUNNY", "Funny"),
    SURPRISED("SURPRISED", "Surprised"),
    CALM("CALM", "Calm");

    companion object {
        fun fromKey(raw: String?): CharacterExpression =
            entries.firstOrNull { it.key == raw?.uppercase() } ?: HAPPY
    }
}

enum class CameraMotion(val key: String) {
    PAN_LEFT("PAN_LEFT"),
    PAN_RIGHT("PAN_RIGHT"),
    ZOOM_IN("ZOOM_IN"),
    ZOOM_OUT("ZOOM_OUT"),
    STATIC("STATIC");

    companion object {
        fun fromKey(raw: String?): CameraMotion =
            entries.firstOrNull { it.key == raw?.uppercase() } ?: ZOOM_IN
    }
}

enum class MoodMusic(val key: String) {
    PLAYFUL("PLAYFUL"),
    ADVENTURE("ADVENTURE"),
    MYSTERY("MYSTERY"),
    EPIC("EPIC"),
    COMEDY("COMEDY"),
    EMOTIONAL("EMOTIONAL"),
    SPOOKY("SPOOKY"),
    UPBEAT_KIDS("UPBEAT_KIDS");

    companion object {
        fun fromKey(raw: String?): MoodMusic =
            entries.firstOrNull { it.key == raw?.uppercase() } ?: ADVENTURE
    }
}

enum class SfxCue(val key: String) {
    WHOOSH("WHOOSH"),
    SPARKLE("SPARKLE"),
    POP("POP"),
    THUNDER("THUNDER"),
    FOOTSTEPS("FOOTSTEPS"),
    NONE("NONE");

    companion object {
        fun fromKey(raw: String?): SfxCue =
            entries.firstOrNull { it.key == raw?.uppercase() } ?: NONE
    }
}

enum class ProjectStatus(val key: String) {
    DRAFT("DRAFT"),
    QUEUED("QUEUED"),
    GENERATING("GENERATING"),
    COMPLETED("COMPLETED"),
    FAILED("FAILED");

    companion object {
        fun fromKey(raw: String?): ProjectStatus =
            entries.firstOrNull { it.key == raw } ?: DRAFT
    }
}

enum class SceneStatus(val key: String) {
    PENDING("PENDING"),
    GENERATING("GENERATING"),
    COMPLETED("COMPLETED"),
    FAILED("FAILED");

    companion object {
        fun fromKey(raw: String?): SceneStatus =
            entries.firstOrNull { it.key == raw } ?: PENDING
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    val userId: String = "",
    val displayName: String = "Creator",
    val email: String? = null,
    val photoUrl: String? = null,
    val creditsRemaining: Long = 50L,
    val totalVideosGenerated: Long = 0L,
    val totalScenesRendered: Long = 0L,
    val preferredStyle: String = CartoonStyle.CARTOON_3D.key,
    val preferredLanguage: String = CartoonLanguage.ENGLISH.key,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CartoonProject(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val prompt: String = "",
    val style: String = CartoonStyle.CARTOON_3D.key,
    val durationOption: String = VideoDurationOption.SEC_30.key,
    val language: String = CartoonLanguage.ENGLISH.key,
    val aspectRatio: String = VideoAspectRatio.RATIO_16_9.key,
    val resolution: String = VideoResolution.RES_1080P.key,
    val status: String = ProjectStatus.DRAFT.key,
    val progressPercent: Long = 0L,
    val sceneCount: Long = 3L,
    val completedScenes: Long = 0L,
    val durationSeconds: Long = 30L,
    val scriptSummary: String? = null,
    val localVideoPath: String? = null,
    val thumbnailBase64: String? = null,
    val errorMessage: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class DialogueLine(
    val speaker: String = "",
    val gender: String = VoiceGender.MALE.key,
    val expression: String = CharacterExpression.HAPPY.key,
    val line: String = "",
    val durationSec: Int = 4
) {
    fun toFirestoreMap(): Map<String, Any> = mapOf(
        "speaker" to speaker.take(60),
        "gender" to VoiceGender.fromKey(gender).key,
        "expression" to CharacterExpression.fromKey(expression).key,
        "line" to line.take(500),
        "durationSec" to durationSec.coerceIn(2, 20)
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): DialogueLine = DialogueLine(
            speaker = (map["speaker"] as? String).orEmpty(),
            gender = VoiceGender.fromKey(map["gender"] as? String).key,
            expression = CharacterExpression.fromKey(map["expression"] as? String).key,
            line = (map["line"] as? String).orEmpty(),
            durationSec = ((map["durationSec"] as? Number)?.toInt() ?: 4).coerceIn(2, 20)
        )
    }
}

@Keep
@IgnoreExtraProperties
data class SceneRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val projectId: String = "",
    val sceneIndex: Long = 0L,
    val title: String = "",
    val visualPrompt: String = "",
    val cameraMotion: String = CameraMotion.ZOOM_IN.key,
    val moodMusic: String = MoodMusic.ADVENTURE.key,
    val sfxCue: String = SfxCue.SPARKLE.key,
    val characterNames: List<String> = emptyList(),
    val dialogues: List<Map<String, Any>> = emptyList(),
    val status: String = SceneStatus.PENDING.key,
    val retryCount: Long = 0L,
    val localImagePath: String? = null,
    val errorMessage: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun parsedDialogues(): List<DialogueLine> = dialogues.map { DialogueLine.fromMap(it) }
}

@Keep
@IgnoreExtraProperties
data class SavedCharacter(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val role: String = "",
    val appearance: String = "",
    val outfit: String = "",
    val personality: String? = null,
    val voiceGender: String = VoiceGender.MALE.key,
    val voicePitch: Double = 1.0,
    val defaultExpression: String = CharacterExpression.HAPPY.key,
    val style: String = CartoonStyle.CARTOON_3D.key,
    val localPortraitPath: String? = null,
    val portraitBase64: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class GenerationHistoryEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val projectId: String = "",
    val projectTitle: String = "",
    val actionType: String = "FULL_MOVIE",
    val creditsUsed: Long = 0L,
    val status: String = "COMPLETED",
    val durationLabel: String = "30s",
    val language: String = CartoonLanguage.ENGLISH.key,
    val style: String = CartoonStyle.CARTOON_3D.key,
    val details: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

// In-memory script & generation pipeline models
data class GeneratedCharacterSpec(
    val name: String,
    val role: String,
    val appearance: String,
    val outfit: String,
    val voiceGender: VoiceGender,
    val voicePitch: Float
)

data class GeneratedSceneSpec(
    val sceneIndex: Int,
    val title: String,
    val visualPrompt: String,
    val cameraMotion: CameraMotion,
    val moodMusic: MoodMusic,
    val sfxCue: SfxCue,
    val characterNames: List<String>,
    val dialogues: List<DialogueLine>
)

data class GeneratedMovieScript(
    val title: String,
    val logline: String,
    val characters: List<GeneratedCharacterSpec>,
    val scenes: List<GeneratedSceneSpec>
)

enum class PipelinePhase(val label: String) {
    IDLE("Ready"),
    WRITING_SCRIPT("Writing Story & Multi-Scene Script"),
    DESIGNING_CHARACTERS("Generating Consistent Characters"),
    RENDERING_SCENES("Generating Animated Scene Frames"),
    SYNTHESIZING_VOICES("Generating Voices, Music & Lip-Sync"),
    ENCODING_VIDEO("Rendering Final MP4 Movie"),
    COMPLETED("Movie Ready"),
    FAILED("Generation Failed")
}

data class SceneJobProgress(
    val sceneId: String,
    val sceneIndex: Int,
    val title: String,
    val status: SceneStatus,
    val attempt: Int = 1,
    val error: String? = null
)

data class GenerationPipelineState(
    val isRunning: Boolean = false,
    val activeProjectId: String? = null,
    val activeTitle: String = "",
    val phase: PipelinePhase = PipelinePhase.IDLE,
    val progressPercent: Int = 0,
    val statusDetail: String = "",
    val sceneJobs: List<SceneJobProgress> = emptyList(),
    val errorMessage: String? = null,
    val completedVideoPath: String? = null
)
