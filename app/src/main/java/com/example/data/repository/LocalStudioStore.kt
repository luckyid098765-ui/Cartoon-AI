package com.example.data.repository

import com.example.data.model.CartoonLanguage
import com.example.data.model.CartoonProject
import com.example.data.model.CartoonStyle
import com.example.data.model.DialogueLine
import com.example.data.model.GenerationHistoryEntry
import com.example.data.model.SavedCharacter
import com.example.data.model.SceneRecord
import com.example.data.model.UserProfile
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class LocalStudioStore(private val filesDir: File) {

    private val storageFile = File(filesDir, "local_cartoon_studio.json")
    private val writeMutex = Mutex()

    private val _profile = MutableStateFlow(
        UserProfile(
            userId = "local_guest",
            displayName = "Cartoon Creator",
            creditsRemaining = 999L,
            totalVideosGenerated = 0L,
            totalScenesRendered = 0L
        )
    )
    val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    private val _projects = MutableStateFlow<List<CartoonProject>>(emptyList())
    val projects: StateFlow<List<CartoonProject>> = _projects.asStateFlow()

    private val _scenesByProject = MutableStateFlow<Map<String, List<SceneRecord>>>(emptyMap())

    private val _characters = MutableStateFlow<List<SavedCharacter>>(emptyList())
    val characters: StateFlow<List<SavedCharacter>> = _characters.asStateFlow()

    private val _history = MutableStateFlow<List<GenerationHistoryEntry>>(emptyList())
    val history: StateFlow<List<GenerationHistoryEntry>> = _history.asStateFlow()

    init {
        loadFromDisk()
    }

    fun observeScenes(projectId: String): Flow<List<SceneRecord>> =
        _scenesByProject.map { map -> map[projectId].orEmpty().sortedBy { it.sceneIndex } }

    suspend fun createProject(project: CartoonProject): Result<String> = runCatching {
        val id = "proj_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val now = Timestamp.now()
        val created = project.copy(
            id = id,
            userId = "local_guest",
            createdAt = now,
            updatedAt = now
        )
        _projects.update { listOf(created) + it }
        persistToDisk()
        id
    }

    suspend fun updateProjectProgress(
        projectId: String,
        title: String? = null,
        prompt: String? = null,
        status: String,
        progressPercent: Int,
        sceneCount: Int? = null,
        completedScenes: Int,
        durationSeconds: Int? = null,
        scriptSummary: String? = null,
        localVideoPath: String? = null,
        thumbnailBase64: String? = null,
        errorMessage: String? = null
    ): Result<Unit> = runCatching {
        val now = Timestamp.now()
        _projects.update { current ->
            current.map { proj ->
                if (proj.id != projectId) {
                    proj
                } else {
                    proj.copy(
                        title = title ?: proj.title,
                        prompt = prompt ?: proj.prompt,
                        status = status,
                        progressPercent = progressPercent.toLong(),
                        sceneCount = sceneCount?.toLong() ?: proj.sceneCount,
                        completedScenes = completedScenes.toLong(),
                        durationSeconds = durationSeconds?.toLong() ?: proj.durationSeconds,
                        scriptSummary = scriptSummary ?: proj.scriptSummary,
                        localVideoPath = localVideoPath ?: proj.localVideoPath,
                        thumbnailBase64 = thumbnailBase64 ?: proj.thumbnailBase64,
                        errorMessage = errorMessage,
                        updatedAt = now
                    )
                }
            }.sortedByDescending { it.updatedAt?.seconds ?: 0L }
        }
        persistToDisk()
    }

    suspend fun deleteProject(projectId: String): Result<Unit> = runCatching {
        _projects.update { list -> list.filterNot { it.id == projectId } }
        _scenesByProject.update { map -> map - projectId }
        persistToDisk()
    }

    suspend fun saveScenesBatch(projectId: String, scenes: List<SceneRecord>): Result<List<String>> = runCatching {
        val now = Timestamp.now()
        val normalized = scenes.map { sc ->
            sc.copy(
                id = "scene_${sc.sceneIndex}",
                userId = "local_guest",
                projectId = projectId,
                createdAt = now,
                updatedAt = now
            )
        }
        _scenesByProject.update { it + (projectId to normalized) }
        persistToDisk()
        normalized.map { it.id }
    }

    suspend fun updateSceneStatus(
        projectId: String,
        sceneId: String,
        status: String,
        retryCount: Long,
        localImagePath: String? = null,
        errorMessage: String? = null
    ): Result<Unit> = runCatching {
        val now = Timestamp.now()
        _scenesByProject.update { map ->
            val current = map[projectId].orEmpty()
            val updated = current.map { sc ->
                if (sc.id != sceneId) {
                    sc
                } else {
                    sc.copy(
                        status = status,
                        retryCount = retryCount,
                        localImagePath = localImagePath ?: sc.localImagePath,
                        errorMessage = errorMessage,
                        updatedAt = now
                    )
                }
            }
            map + (projectId to updated)
        }
        persistToDisk()
    }

    suspend fun saveCharacter(character: SavedCharacter): Result<String> = runCatching {
        val id = character.id.ifBlank { "char_${UUID.randomUUID().toString().replace("-", "").take(12)}" }
        val now = Timestamp.now()
        val saved = character.copy(
            id = id,
            userId = "local_guest",
            createdAt = character.createdAt ?: now,
            updatedAt = now
        )
        _characters.update { list ->
            val filtered = list.filterNot { it.id == id }
            listOf(saved) + filtered
        }
        persistToDisk()
        id
    }

    suspend fun deleteCharacter(characterId: String): Result<Unit> = runCatching {
        _characters.update { list -> list.filterNot { it.id == characterId } }
        persistToDisk()
    }

    suspend fun addHistoryEntry(entry: GenerationHistoryEntry): Result<Unit> = runCatching {
        val id = "hist_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val saved = entry.copy(
            id = id,
            userId = "local_guest",
            createdAt = Timestamp.now()
        )
        _history.update { listOf(saved) + it.take(99) }
        persistToDisk()
    }

    suspend fun deleteHistoryEntry(historyId: String): Result<Unit> = runCatching {
        _history.update { list -> list.filterNot { it.id == historyId } }
        persistToDisk()
    }

    suspend fun clearHistory(): Result<Unit> = runCatching {
        _history.value = emptyList()
        persistToDisk()
    }

    suspend fun recordStats(videosIncrement: Long, scenesIncrement: Long): Result<Unit> = runCatching {
        _profile.update { p ->
            p.copy(
                totalVideosGenerated = p.totalVideosGenerated + videosIncrement,
                totalScenesRendered = p.totalScenesRendered + scenesIncrement,
                updatedAt = Timestamp.now()
            )
        }
        persistToDisk()
    }

    suspend fun updatePreferences(style: CartoonStyle, language: CartoonLanguage): Result<Unit> = runCatching {
        _profile.update { p ->
            p.copy(
                preferredStyle = style.key,
                preferredLanguage = language.key,
                updatedAt = Timestamp.now()
            )
        }
        persistToDisk()
    }

    private suspend fun persistToDisk() = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            val prof = _profile.value
            val rootMap = linkedMapOf<String, Any?>(
                "profile" to linkedMapOf<String, Any?>(
                    "displayName" to prof.displayName,
                    "totalVideosGenerated" to prof.totalVideosGenerated,
                    "totalScenesRendered" to prof.totalScenesRendered,
                    "preferredStyle" to prof.preferredStyle,
                    "preferredLanguage" to prof.preferredLanguage
                ),
                "projects" to _projects.value.map { p ->
                    linkedMapOf<String, Any?>(
                        "id" to p.id,
                        "title" to p.title,
                        "prompt" to p.prompt,
                        "style" to p.style,
                        "durationOption" to p.durationOption,
                        "language" to p.language,
                        "aspectRatio" to p.aspectRatio,
                        "resolution" to p.resolution,
                        "status" to p.status,
                        "progressPercent" to p.progressPercent,
                        "sceneCount" to p.sceneCount,
                        "completedScenes" to p.completedScenes,
                        "durationSeconds" to p.durationSeconds,
                        "scriptSummary" to p.scriptSummary,
                        "localVideoPath" to p.localVideoPath,
                        "thumbnailBase64" to p.thumbnailBase64,
                        "errorMessage" to p.errorMessage,
                        "updatedSec" to (p.updatedAt?.seconds ?: (System.currentTimeMillis() / 1000L))
                    )
                },
                "scenes" to _scenesByProject.value.mapValues { (_, list) ->
                    list.map { sc ->
                        linkedMapOf<String, Any?>(
                            "id" to sc.id,
                            "projectId" to sc.projectId,
                            "sceneIndex" to sc.sceneIndex,
                            "title" to sc.title,
                            "visualPrompt" to sc.visualPrompt,
                            "cameraMotion" to sc.cameraMotion,
                            "moodMusic" to sc.moodMusic,
                            "sfxCue" to sc.sfxCue,
                            "characterNames" to sc.characterNames,
                            "dialogues" to sc.parsedDialogues().map { d ->
                                linkedMapOf<String, Any?>(
                                    "speaker" to d.speaker,
                                    "gender" to d.gender,
                                    "expression" to d.expression,
                                    "line" to d.line,
                                    "durationSec" to d.durationSec
                                )
                            },
                            "status" to sc.status,
                            "retryCount" to sc.retryCount,
                            "localImagePath" to sc.localImagePath,
                            "errorMessage" to sc.errorMessage
                        )
                    }
                },
                "characters" to _characters.value.map { ch ->
                    linkedMapOf<String, Any?>(
                        "id" to ch.id,
                        "name" to ch.name,
                        "role" to ch.role,
                        "appearance" to ch.appearance,
                        "outfit" to ch.outfit,
                        "personality" to ch.personality,
                        "voiceGender" to ch.voiceGender,
                        "voicePitch" to ch.voicePitch,
                        "defaultExpression" to ch.defaultExpression,
                        "style" to ch.style,
                        "localPortraitPath" to ch.localPortraitPath,
                        "portraitBase64" to ch.portraitBase64
                    )
                },
                "history" to _history.value.map { h ->
                    linkedMapOf<String, Any?>(
                        "id" to h.id,
                        "projectId" to h.projectId,
                        "projectTitle" to h.projectTitle,
                        "actionType" to h.actionType,
                        "creditsUsed" to h.creditsUsed,
                        "status" to h.status,
                        "durationLabel" to h.durationLabel,
                        "language" to h.language,
                        "style" to h.style,
                        "details" to h.details
                    )
                }
            )

            storageFile.parentFile?.mkdirs()
            val tmp = File(storageFile.parentFile, "${storageFile.name}.tmp")
            tmp.writeText(SimpleJson.encode(rootMap))
            if (storageFile.exists()) {
                storageFile.delete()
            }
            if (!tmp.renameTo(storageFile)) {
                tmp.copyTo(storageFile, overwrite = true)
                tmp.delete()
            }
        }
    }

    private fun loadFromDisk() {
        if (!storageFile.exists()) return
        runCatching {
            val root = SimpleJson.decodeObject(storageFile.readText()) ?: return@runCatching
            root.obj("profile")?.let { prof ->
                _profile.value = _profile.value.copy(
                    displayName = prof.str("displayName", "Cartoon Creator"),
                    totalVideosGenerated = prof.lng("totalVideosGenerated", 0L),
                    totalScenesRendered = prof.lng("totalScenesRendered", 0L),
                    preferredStyle = prof.str("preferredStyle", CartoonStyle.CARTOON_3D.key),
                    preferredLanguage = prof.str("preferredLanguage", CartoonLanguage.ENGLISH.key)
                )
            }

            _projects.value = root.objList("projects").map { o ->
                val sec = o.lng("updatedSec", 0L)
                CartoonProject(
                    id = o.str("id"),
                    userId = "local_guest",
                    title = o.str("title"),
                    prompt = o.str("prompt"),
                    style = o.str("style", CartoonStyle.CARTOON_3D.key),
                    durationOption = o.str("durationOption", "SEC_30"),
                    language = o.str("language", CartoonLanguage.ENGLISH.key),
                    aspectRatio = o.str("aspectRatio", "RATIO_16_9"),
                    resolution = o.str("resolution", "RES_1080P"),
                    status = o.str("status", "DRAFT"),
                    progressPercent = o.lng("progressPercent", 0L),
                    sceneCount = o.lng("sceneCount", 3L),
                    completedScenes = o.lng("completedScenes", 0L),
                    durationSeconds = o.lng("durationSeconds", 30L),
                    scriptSummary = o.optStr("scriptSummary"),
                    localVideoPath = o.optStr("localVideoPath"),
                    thumbnailBase64 = o.optStr("thumbnailBase64"),
                    errorMessage = o.optStr("errorMessage"),
                    updatedAt = if (sec > 0L) Timestamp(sec, 0) else null
                )
            }

            val sceneMap = mutableMapOf<String, List<SceneRecord>>()
            root.obj("scenes")?.forEach { (pid, rawList) ->
                @Suppress("UNCHECKED_CAST")
                val arr = (rawList as? List<*>)?.mapNotNull { it as? Map<String, Any?> }.orEmpty()
                sceneMap[pid] = arr.mapIndexed { i, o ->
                    val charNames = o.strList("characterNames")
                    val dlgs = o.objList("dialogues").map { d ->
                        DialogueLine(
                            speaker = d.str("speaker"),
                            gender = d.str("gender", "MALE"),
                            expression = d.str("expression", "HAPPY"),
                            line = d.str("line"),
                            durationSec = d.lng("durationSec", 4L).toInt()
                        ).toFirestoreMap()
                    }
                    SceneRecord(
                        id = o.str("id"),
                        userId = "local_guest",
                        projectId = pid,
                        sceneIndex = o.lng("sceneIndex", i.toLong()),
                        title = o.str("title"),
                        visualPrompt = o.str("visualPrompt"),
                        cameraMotion = o.str("cameraMotion", "ZOOM_IN"),
                        moodMusic = o.str("moodMusic", "ADVENTURE"),
                        sfxCue = o.str("sfxCue", "SPARKLE"),
                        characterNames = charNames,
                        dialogues = dlgs,
                        status = o.str("status", "PENDING"),
                        retryCount = o.lng("retryCount", 0L),
                        localImagePath = o.optStr("localImagePath"),
                        errorMessage = o.optStr("errorMessage")
                    )
                }
            }
            _scenesByProject.value = sceneMap

            _characters.value = root.objList("characters").map { o ->
                SavedCharacter(
                    id = o.str("id"),
                    userId = "local_guest",
                    name = o.str("name"),
                    role = o.str("role"),
                    appearance = o.str("appearance"),
                    outfit = o.str("outfit"),
                    personality = o.optStr("personality"),
                    voiceGender = o.str("voiceGender", "MALE"),
                    voicePitch = o.dbl("voicePitch", 1.0),
                    defaultExpression = o.str("defaultExpression", "HAPPY"),
                    style = o.str("style", CartoonStyle.CARTOON_3D.key),
                    localPortraitPath = o.optStr("localPortraitPath"),
                    portraitBase64 = o.optStr("portraitBase64")
                )
            }

            _history.value = root.objList("history").map { o ->
                GenerationHistoryEntry(
                    id = o.str("id"),
                    userId = "local_guest",
                    projectId = o.str("projectId"),
                    projectTitle = o.str("projectTitle"),
                    actionType = o.str("actionType", "FULL_MOVIE"),
                    creditsUsed = o.lng("creditsUsed", 0L),
                    status = o.str("status", "COMPLETED"),
                    durationLabel = o.str("durationLabel", "30s"),
                    language = o.str("language", CartoonLanguage.ENGLISH.label),
                    style = o.str("style", CartoonStyle.CARTOON_3D.label),
                    details = o.optStr("details")
                )
            }
        }
    }
}

private fun Map<String, Any?>.str(key: String, default: String = ""): String =
    (this[key] as? String) ?: default

private fun Map<String, Any?>.optStr(key: String): String? =
    (this[key] as? String)?.takeIf { it.isNotBlank() }

private fun Map<String, Any?>.lng(key: String, default: Long = 0L): Long =
    (this[key] as? Number)?.toLong() ?: default

private fun Map<String, Any?>.dbl(key: String, default: Double = 0.0): Double =
    (this[key] as? Number)?.toDouble() ?: default

@Suppress("UNCHECKED_CAST")
private fun Map<String, Any?>.obj(key: String): Map<String, Any?>? =
    this[key] as? Map<String, Any?>

@Suppress("UNCHECKED_CAST")
private fun Map<String, Any?>.objList(key: String): List<Map<String, Any?>> =
    (this[key] as? List<*>)?.mapNotNull { it as? Map<String, Any?> }.orEmpty()

private fun Map<String, Any?>.strList(key: String): List<String> =
    (this[key] as? List<*>)?.mapNotNull { it as? String }.orEmpty()

private object SimpleJson {
    fun encode(value: Any?): String = buildString { appendValue(value) }

    private fun StringBuilder.appendValue(value: Any?) {
        when (value) {
            null -> append("null")
            is Boolean -> append(value)
            is Number -> append(value.toString())
            is String -> appendString(value)
            is Map<*, *> -> {
                append('{')
                var first = true
                value.forEach { (k, v) ->
                    if (k is String) {
                        if (!first) append(',')
                        first = false
                        appendString(k)
                        append(':')
                        appendValue(v)
                    }
                }
                append('}')
            }
            is Iterable<*> -> {
                append('[')
                var first = true
                value.forEach { item ->
                    if (!first) append(',')
                    first = false
                    appendValue(item)
                }
                append(']')
            }
            else -> appendString(value.toString())
        }
    }

    private fun StringBuilder.appendString(text: String) {
        append('"')
        for (ch in text) {
            when (ch) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (ch.code < 0x20) {
                    append("\\u")
                    append(ch.code.toString(16).padStart(4, '0'))
                } else {
                    append(ch)
                }
            }
        }
        append('"')
    }

    @Suppress("UNCHECKED_CAST")
    fun decodeObject(json: String): Map<String, Any?>? =
        Parser(json).parseRoot() as? Map<String, Any?>

    private class Parser(private val src: String) {
        private var pos = 0

        fun parseRoot(): Any? {
            skipWs()
            if (pos >= src.length) return null
            return parseValue()
        }

        private fun parseValue(): Any? {
            skipWs()
            if (pos >= src.length) error("Unexpected EOF")
            return when (val c = src[pos]) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> parseString()
                't', 'f' -> parseBoolean()
                'n' -> parseNull()
                '-', in '0'..'9' -> parseNumber()
                else -> error("Unexpected char '$c' at $pos")
            }
        }

        private fun parseObject(): Map<String, Any?> {
            pos++ // '{'
            val map = linkedMapOf<String, Any?>()
            skipWs()
            if (pos < src.length && src[pos] == '}') {
                pos++
                return map
            }
            while (pos < src.length) {
                skipWs()
                val key = parseString()
                skipWs()
                if (src[pos] != ':') error("Expected ':' at $pos")
                pos++
                val value = parseValue()
                map[key] = value
                skipWs()
                when (src.getOrNull(pos)) {
                    ',' -> pos++
                    '}' -> {
                        pos++
                        return map
                    }
                    else -> error("Expected ',' or '}' at $pos")
                }
            }
            return map
        }

        private fun parseArray(): List<Any?> {
            pos++ // '['
            val list = mutableListOf<Any?>()
            skipWs()
            if (pos < src.length && src[pos] == ']') {
                pos++
                return list
            }
            while (pos < src.length) {
                list.add(parseValue())
                skipWs()
                when (src.getOrNull(pos)) {
                    ',' -> pos++
                    ']' -> {
                        pos++
                        return list
                    }
                    else -> error("Expected ',' or ']' at $pos")
                }
            }
            return list
        }

        private fun parseString(): String {
            if (src[pos] != '"') error("Expected '\"' at $pos")
            pos++
            val sb = StringBuilder()
            while (pos < src.length) {
                val c = src[pos++]
                when (c) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        if (pos >= src.length) error("Unterminated escape")
                        when (val esc = src[pos++]) {
                            '"' -> sb.append('"')
                            '\\' -> sb.append('\\')
                            '/' -> sb.append('/')
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                val hex = src.substring(pos, pos + 4)
                                pos += 4
                                sb.append(hex.toInt(16).toChar())
                            }
                            else -> sb.append(esc)
                        }
                    }
                    else -> sb.append(c)
                }
            }
            error("Unterminated string")
        }

        private fun parseBoolean(): Boolean {
            return if (src.startsWith("true", pos)) {
                pos += 4
                true
            } else {
                pos += 5
                false
            }
        }

        private fun parseNull(): Any? {
            pos += 4
            return null
        }

        private fun parseNumber(): Number {
            val start = pos
            if (src[pos] == '-') pos++
            while (pos < src.length && src[pos].isDigit()) pos++
            var isDouble = false
            if (pos < src.length && src[pos] == '.') {
                isDouble = true
                pos++
                while (pos < src.length && src[pos].isDigit()) pos++
            }
            if (pos < src.length && (src[pos] == 'e' || src[pos] == 'E')) {
                isDouble = true
                pos++
                if (pos < src.length && (src[pos] == '+' || src[pos] == '-')) pos++
                while (pos < src.length && src[pos].isDigit()) pos++
            }
            val token = src.substring(start, pos)
            return if (isDouble) token.toDouble() else token.toLong()
        }

        private fun skipWs() {
            while (pos < src.length && src[pos].isWhitespace()) pos++
        }
    }
}
