package com.example.ui.viewmodel

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.ai.CartoonAiService
import com.example.data.model.CameraMotion
import com.example.data.model.CartoonLanguage
import com.example.data.model.CartoonProject
import com.example.data.model.CartoonStyle
import com.example.data.model.CharacterExpression
import com.example.data.model.GeneratedCharacterSpec
import com.example.data.model.GeneratedSceneSpec
import com.example.data.model.GenerationHistoryEntry
import com.example.data.model.GenerationPipelineState
import com.example.data.model.MoodMusic
import com.example.data.model.PipelinePhase
import com.example.data.model.ProjectStatus
import com.example.data.model.SavedCharacter
import com.example.data.model.SceneJobProgress
import com.example.data.model.SceneRecord
import com.example.data.model.SceneStatus
import com.example.data.model.SfxCue
import com.example.data.model.UserProfile
import com.example.data.model.VideoAspectRatio
import com.example.data.model.VideoDurationOption
import com.example.data.model.VideoResolution
import com.example.data.model.VoiceGender
import com.example.data.repository.CartoonRepository
import com.example.data.repository.LocalStudioStore
import com.example.media.CartoonVideoRenderer
import com.example.media.SceneRenderInput
import com.example.media.VoiceAndAudioEngine
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

private const val TAG = "CartoonStudioVM"

sealed interface DataUiState<out T> {
    data object Loading : DataUiState<Nothing>
    data class Success<T>(val data: T) : DataUiState<T>
    data class Error(val message: String) : DataUiState<Nothing>
}

data class GeneratorFormState(
    val prompt: String = "",
    val style: CartoonStyle = CartoonStyle.CARTOON_3D,
    val duration: VideoDurationOption = VideoDurationOption.SEC_30,
    val language: CartoonLanguage = CartoonLanguage.ENGLISH,
    val aspectRatio: VideoAspectRatio = VideoAspectRatio.RATIO_16_9,
    val resolution: VideoResolution = VideoResolution.RES_1080P,
    val selectedCharacterIds: Set<String> = emptySet(),
    val isGeneratingStoryIdea: Boolean = false,
    val storyIdeaError: String? = null
)

private data class FailedGenerationRequest(
    val existingProjectId: String?,
    val prompt: String,
    val style: CartoonStyle,
    val durationOption: VideoDurationOption,
    val language: CartoonLanguage,
    val aspectRatio: VideoAspectRatio,
    val resolution: VideoResolution,
    val lockedCharacters: List<SavedCharacter>,
    val titleOverride: String?
)

class CartoonStudioViewModel(
    private val repository: CartoonRepository?,
    private val localStore: LocalStudioStore,
    private val aiService: CartoonAiService,
    private val audioEngine: VoiceAndAudioEngine,
    private val videoRenderer: CartoonVideoRenderer,
    private val filesDir: File,
    private val currentUserId: String?
) : ViewModel() {

    private val ownerId: String = currentUserId ?: "local_guest"

    private fun unwrapFirestoreError(error: Throwable, fallbackMessage: String): DataUiState.Error {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, fallbackMessage, rootError)
        return DataUiState.Error(rootError.message ?: fallbackMessage)
    }

    val userProfileState: StateFlow<DataUiState<UserProfile?>> =
        if (repository != null && !currentUserId.isNullOrBlank()) {
            repository.observeUserProfile(currentUserId)
                .map<UserProfile?, DataUiState<UserProfile?>> { DataUiState.Success(it) }
                .catch { emit(unwrapFirestoreError(it, "Failed to load profile")) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataUiState.Loading)
        } else {
            localStore.profile
                .map<UserProfile?, DataUiState<UserProfile?>> { DataUiState.Success(it) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataUiState.Success(localStore.profile.value))
        }

    val projectsState: StateFlow<DataUiState<List<CartoonProject>>> =
        if (repository != null && !currentUserId.isNullOrBlank()) {
            repository.observeProjects(currentUserId)
                .map<List<CartoonProject>, DataUiState<List<CartoonProject>>> { DataUiState.Success(it) }
                .catch { emit(unwrapFirestoreError(it, "Failed to load projects")) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataUiState.Loading)
        } else {
            localStore.projects
                .map<List<CartoonProject>, DataUiState<List<CartoonProject>>> { DataUiState.Success(it) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataUiState.Success(localStore.projects.value))
        }

    val charactersState: StateFlow<DataUiState<List<SavedCharacter>>> =
        if (repository != null && !currentUserId.isNullOrBlank()) {
            repository.observeCharacters(currentUserId)
                .map<List<SavedCharacter>, DataUiState<List<SavedCharacter>>> { DataUiState.Success(it) }
                .catch { emit(unwrapFirestoreError(it, "Failed to load characters")) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataUiState.Loading)
        } else {
            localStore.characters
                .map<List<SavedCharacter>, DataUiState<List<SavedCharacter>>> { DataUiState.Success(it) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataUiState.Success(localStore.characters.value))
        }

    val historyState: StateFlow<DataUiState<List<GenerationHistoryEntry>>> =
        if (repository != null && !currentUserId.isNullOrBlank()) {
            repository.observeHistory(currentUserId)
                .map<List<GenerationHistoryEntry>, DataUiState<List<GenerationHistoryEntry>>> { DataUiState.Success(it) }
                .catch { emit(unwrapFirestoreError(it, "Failed to load history")) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataUiState.Loading)
        } else {
            localStore.history
                .map<List<GenerationHistoryEntry>, DataUiState<List<GenerationHistoryEntry>>> { DataUiState.Success(it) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataUiState.Success(localStore.history.value))
        }

    private val _selectedProjectId = MutableStateFlow<String?>(null)
    val selectedProjectId: StateFlow<String?> = _selectedProjectId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedProjectScenesState: StateFlow<DataUiState<List<SceneRecord>>> =
        _selectedProjectId.flatMapLatest { projectId ->
            if (projectId.isNullOrBlank()) {
                flowOf(DataUiState.Success(emptyList()))
            } else if (repository != null && !currentUserId.isNullOrBlank()) {
                repository.observeScenes(currentUserId, projectId)
                    .map<List<SceneRecord>, DataUiState<List<SceneRecord>>> { DataUiState.Success(it) }
                    .catch { emit(unwrapFirestoreError(it, "Failed to load scenes")) }
            } else {
                localStore.observeScenes(projectId)
                    .map<List<SceneRecord>, DataUiState<List<SceneRecord>>> { DataUiState.Success(it) }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataUiState.Loading)

    private val _formState = MutableStateFlow(GeneratorFormState())
    val formState: StateFlow<GeneratorFormState> = _formState.asStateFlow()

    private val _pipelineState = MutableStateFlow(GenerationPipelineState())
    val pipelineState: StateFlow<GenerationPipelineState> = _pipelineState.asStateFlow()

    private val _characterGenBusy = MutableStateFlow(false)
    val characterGenBusy: StateFlow<Boolean> = _characterGenBusy.asStateFlow()

    private val _uiNotice = MutableStateFlow<String?>(null)
    val uiNotice: StateFlow<String?> = _uiNotice.asStateFlow()

    private var activeGenerationJob: Job? = null
    private var lastFailedRequest: FailedGenerationRequest? = null

    private val randomStories = listOf(
        "A fearless little robot fox named Bolt and a wise owl named Luna discover a floating clockwork island in the clouds. Together they must repair the giant crystal compass before the sky lanterns go out forever.",
        "Chintu, a cheerful monkey wearing a red cap, accidentally launches his grandmother's spicy samosa cart into outer space, turning a grumpy alien king into his biggest food fan.",
        "In a glowing underwater kingdom, a shy seahorse painter named Coral uses a magical rainbow brush to bring frozen coral reefs back to life and outsmart a giant grumpy crab.",
        "A young detective girl named Tara and her talking blue dachshund follow glowing footprints through a neon midnight city to find a missing musical dragon."
    )

    fun clearNotice() {
        _uiNotice.value = null
    }

    fun showNotice(message: String) {
        _uiNotice.value = message
    }

    fun selectProject(projectId: String?) {
        _selectedProjectId.value = projectId
    }

    fun updatePrompt(value: String) {
        _formState.update { it.copy(prompt = value, storyIdeaError = null) }
    }

    fun clearPrompt() {
        _formState.update { it.copy(prompt = "", storyIdeaError = null) }
    }

    fun pickRandomStory() {
        val current = _formState.value.prompt
        val next = randomStories.filter { it != current }.randomOrNull() ?: randomStories.first()
        _formState.update { it.copy(prompt = next, storyIdeaError = null) }
    }

    fun applyQuickTheme(style: CartoonStyle, samplePrompt: String? = null) {
        _formState.update { state ->
            state.copy(
                style = style,
                prompt = if (state.prompt.isBlank() && !samplePrompt.isNullOrBlank()) samplePrompt else state.prompt,
                storyIdeaError = null
            )
        }
    }

    fun selectStyle(style: CartoonStyle) {
        _formState.update { it.copy(style = style) }
    }

    fun selectDuration(duration: VideoDurationOption) {
        _formState.update { it.copy(duration = duration) }
    }

    fun selectLanguage(language: CartoonLanguage) {
        _formState.update { it.copy(language = language) }
    }

    fun selectAspectRatio(ratio: VideoAspectRatio) {
        _formState.update { it.copy(aspectRatio = ratio) }
    }

    fun selectResolution(resolution: VideoResolution) {
        _formState.update { it.copy(resolution = resolution) }
    }

    fun toggleCharacterSelection(characterId: String) {
        _formState.update { state ->
            val updated = if (characterId in state.selectedCharacterIds) {
                state.selectedCharacterIds - characterId
            } else {
                state.selectedCharacterIds + characterId
            }
            state.copy(selectedCharacterIds = updated)
        }
    }

    fun generateAiStoryIdea() {
        val form = _formState.value
        if (form.isGeneratingStoryIdea) return
        viewModelScope.launch {
            _formState.update { it.copy(isGeneratingStoryIdea = true, storyIdeaError = null) }
            try {
                val generated = aiService.generateStoryIdea(
                    seedPrompt = form.prompt,
                    style = form.style,
                    language = form.language
                )
                _formState.update {
                    it.copy(
                        prompt = generated,
                        isGeneratingStoryIdea = false,
                        storyIdeaError = null
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _formState.update {
                    it.copy(
                        isGeneratingStoryIdea = false,
                        storyIdeaError = e.localizedMessage ?: "Failed to generate story."
                    )
                }
            }
        }
    }

    fun cancelGeneration() {
        activeGenerationJob?.cancel()
        activeGenerationJob = null
        _pipelineState.update {
            it.copy(
                isRunning = false,
                phase = PipelinePhase.IDLE,
                statusDetail = "Generation cancelled"
            )
        }
    }

    // Unlimited generation without daily/monthly quotas, credits, or arbitrary prompt-length limits
    fun generateCartoonMovie(onOpenPlayer: (String) -> Unit = {}) {
        val form = _formState.value
        val trimmedPrompt = form.prompt.trim()
        if (trimmedPrompt.isBlank()) {
            _uiNotice.value = "Please paste or enter a cartoon prompt."
            return
        }
        if (_pipelineState.value.isRunning) {
            _uiNotice.value = "A cartoon video is already generating."
            return
        }

        val savedChars = (charactersState.value as? DataUiState.Success)?.data.orEmpty()
        val lockedChars = savedChars.filter { it.id in form.selectedCharacterIds }

        activeGenerationJob = viewModelScope.launch {
            runFullPipeline(
                existingProjectId = null,
                prompt = trimmedPrompt,
                style = form.style,
                durationOption = form.duration,
                language = form.language,
                aspectRatio = form.aspectRatio,
                resolution = form.resolution,
                lockedCharacters = lockedChars,
                onOpenPlayer = onOpenPlayer
            )
        }
    }

    fun retryFailedGeneration(onOpenPlayer: (String) -> Unit = {}) {
        if (_pipelineState.value.isRunning) return
        val last = lastFailedRequest
        if (last != null) {
            activeGenerationJob = viewModelScope.launch {
                runFullPipeline(
                    existingProjectId = last.existingProjectId,
                    prompt = last.prompt,
                    style = last.style,
                    durationOption = last.durationOption,
                    language = last.language,
                    aspectRatio = last.aspectRatio,
                    resolution = last.resolution,
                    lockedCharacters = last.lockedCharacters,
                    onOpenPlayer = onOpenPlayer,
                    titleOverride = last.titleOverride
                )
            }
        } else {
            generateCartoonMovie(onOpenPlayer)
        }
    }

    fun editStoryAndRegenerate(
        project: CartoonProject,
        newTitle: String,
        newPrompt: String,
        onOpenPlayer: (String) -> Unit = {}
    ) {
        if (_pipelineState.value.isRunning) {
            _uiNotice.value = "Wait for current generation to finish first."
            return
        }
        val savedChars = (charactersState.value as? DataUiState.Success)?.data.orEmpty()
        activeGenerationJob = viewModelScope.launch {
            updateProject(
                projectId = project.id,
                title = newTitle,
                prompt = newPrompt,
                status = ProjectStatus.QUEUED.key,
                progressPercent = 2,
                completedScenes = 0
            )
            runFullPipeline(
                existingProjectId = project.id,
                prompt = newPrompt,
                style = CartoonStyle.fromKey(project.style),
                durationOption = VideoDurationOption.fromKey(project.durationOption),
                language = CartoonLanguage.fromKey(project.language),
                aspectRatio = VideoAspectRatio.fromKey(project.aspectRatio),
                resolution = VideoResolution.fromKey(project.resolution),
                lockedCharacters = savedChars.take(2),
                onOpenPlayer = onOpenPlayer,
                titleOverride = newTitle
            )
        }
    }

    fun regenerateCartoon(
        project: CartoonProject,
        onOpenPlayer: (String) -> Unit = {}
    ) {
        editStoryAndRegenerate(
            project = project,
            newTitle = project.title,
            newPrompt = project.prompt,
            onOpenPlayer = onOpenPlayer
        )
    }

    fun saveProjectToMyProjects(project: CartoonProject) {
        viewModelScope.launch {
            val result = updateProject(
                projectId = project.id,
                title = project.title,
                prompt = project.prompt,
                status = project.status,
                progressPercent = project.progressPercent.toInt(),
                sceneCount = project.sceneCount.toInt(),
                completedScenes = project.completedScenes.toInt(),
                durationSeconds = project.durationSeconds.toInt(),
                scriptSummary = project.scriptSummary,
                localVideoPath = project.localVideoPath,
                thumbnailBase64 = project.thumbnailBase64
            )
            result.onSuccess {
                _uiNotice.value = "Saved \"${project.title}\" to My Projects!"
            }.onFailure { err ->
                _uiNotice.value = err.localizedMessage ?: "Failed to save to My Projects."
            }
        }
    }

    private suspend fun runFullPipeline(
        existingProjectId: String?,
        prompt: String,
        style: CartoonStyle,
        durationOption: VideoDurationOption,
        language: CartoonLanguage,
        aspectRatio: VideoAspectRatio,
        resolution: VideoResolution,
        lockedCharacters: List<SavedCharacter>,
        onOpenPlayer: (String) -> Unit,
        titleOverride: String? = null
    ) {
        var projectId = existingProjectId
        lastFailedRequest = FailedGenerationRequest(
            existingProjectId = existingProjectId,
            prompt = prompt,
            style = style,
            durationOption = durationOption,
            language = language,
            aspectRatio = aspectRatio,
            resolution = resolution,
            lockedCharacters = lockedCharacters,
            titleOverride = titleOverride
        )

        try {
            _pipelineState.value = GenerationPipelineState(
                isRunning = true,
                activeProjectId = projectId,
                activeTitle = titleOverride ?: "Preparing Cartoon...",
                phase = PipelinePhase.WRITING_SCRIPT,
                progressPercent = 5,
                statusDetail = "Converting story into ${durationOption.sceneCount}-scene ${language.label} script..."
            )

            // 1. Generate complete script & scene breakdown via Gemini AI
            val script = aiService.generateFullScript(
                prompt = prompt,
                style = style,
                durationOption = durationOption,
                language = language,
                lockedCharacters = lockedCharacters
            )
            val movieTitle = titleOverride?.takeIf { it.isNotBlank() } ?: script.title

            // 2. Create or update Project & Scenes
            if (projectId == null) {
                val created = createProjectRecord(
                    CartoonProject(
                        title = movieTitle,
                        prompt = prompt,
                        style = style.key,
                        durationOption = durationOption.key,
                        language = language.key,
                        aspectRatio = aspectRatio.key,
                        resolution = resolution.key,
                        status = ProjectStatus.GENERATING.key,
                        progressPercent = 12,
                        sceneCount = script.scenes.size.toLong(),
                        completedScenes = 0,
                        durationSeconds = durationOption.targetSeconds.toLong(),
                        scriptSummary = script.logline
                    )
                )
                projectId = created.getOrThrow()
                lastFailedRequest = lastFailedRequest?.copy(existingProjectId = projectId)
            } else {
                updateProject(
                    projectId = projectId,
                    title = movieTitle,
                    prompt = prompt,
                    status = ProjectStatus.GENERATING.key,
                    progressPercent = 12,
                    sceneCount = script.scenes.size,
                    completedScenes = 0,
                    scriptSummary = script.logline
                )
            }
            _selectedProjectId.value = projectId

            val initialSceneRecords = script.scenes.map { sc ->
                SceneRecord(
                    id = "scene_${sc.sceneIndex}",
                    userId = ownerId,
                    projectId = projectId,
                    sceneIndex = sc.sceneIndex.toLong(),
                    title = sc.title,
                    visualPrompt = sc.visualPrompt,
                    cameraMotion = sc.cameraMotion.key,
                    moodMusic = sc.moodMusic.key,
                    sfxCue = sc.sfxCue.key,
                    characterNames = sc.characterNames,
                    dialogues = sc.dialogues.map { it.toFirestoreMap() },
                    status = SceneStatus.PENDING.key,
                    retryCount = 0L
                )
            }
            saveScenes(projectId, initialSceneRecords).getOrThrow()

            val jobs = initialSceneRecords.map {
                SceneJobProgress(
                    sceneId = it.id,
                    sceneIndex = it.sceneIndex.toInt(),
                    title = it.title,
                    status = SceneStatus.PENDING
                )
            }.toMutableList()

            _pipelineState.update {
                it.copy(
                    activeProjectId = projectId,
                    activeTitle = movieTitle,
                    phase = PipelinePhase.DESIGNING_CHARACTERS,
                    progressPercent = 18,
                    statusDetail = "Generating consistent character model sheets (${script.characters.size} characters)...",
                    sceneJobs = jobs.toList()
                )
            }

            // 3. Build character portrait reference map
            val referencePortraits = mutableMapOf<String, Bitmap>()
            for (locked in lockedCharacters) {
                loadSavedCharacterBitmap(locked)?.let { referencePortraits[locked.name] = it }
            }
            for (charSpec in script.characters.take(3)) {
                if (referencePortraits.keys.none { it.equals(charSpec.name, ignoreCase = true) }) {
                    val portrait = retryOperation(maxAttempts = 2) {
                        aiService.generateCharacterPortrait(charSpec, style)
                    }
                    referencePortraits[charSpec.name] = portrait
                    saveCharacterSheetFile(projectId, charSpec.name, portrait)
                }
            }

            // 4. Generate every scene frame in job queue with retries & progress updates
            _pipelineState.update {
                it.copy(
                    phase = PipelinePhase.RENDERING_SCENES,
                    progressPercent = 25,
                    statusDetail = "Rendering animated scenes with consistent characters..."
                )
            }

            val sceneBitmaps = mutableListOf<Bitmap>()
            val totalScenes = script.scenes.size
            for ((index, sceneSpec) in script.scenes.withIndex()) {
                val sceneId = "scene_$index"
                jobs[index] = jobs[index].copy(status = SceneStatus.GENERATING, attempt = 1)
                _pipelineState.update {
                    it.copy(
                        sceneJobs = jobs.toList(),
                        statusDetail = "Generating Scene ${index + 1}/$totalScenes: ${sceneSpec.title}"
                    )
                }
                updateScene(
                    projectId = projectId,
                    sceneId = sceneId,
                    status = SceneStatus.GENERATING.key,
                    retryCount = 0
                )

                var attemptsUsed = 0
                val bitmap = retryOperation(maxAttempts = 2, onRetry = { attempt ->
                    attemptsUsed = attempt
                    jobs[index] = jobs[index].copy(attempt = attempt + 1)
                    _pipelineState.update { it.copy(sceneJobs = jobs.toList()) }
                }) {
                    aiService.generateSceneArtwork(
                        scene = sceneSpec,
                        characters = script.characters,
                        referencePortraits = referencePortraits,
                        style = style,
                        aspectRatio = aspectRatio
                    )
                }

                val sceneFile = saveSceneFrameFile(projectId, index, bitmap)
                sceneBitmaps.add(bitmap)
                jobs[index] = jobs[index].copy(status = SceneStatus.COMPLETED)
                val sceneProgress = 25 + (((index + 1).toFloat() / totalScenes) * 45).toInt()

                _pipelineState.update {
                    it.copy(
                        progressPercent = sceneProgress,
                        sceneJobs = jobs.toList()
                    )
                }
                updateScene(
                    projectId = projectId,
                    sceneId = sceneId,
                    status = SceneStatus.COMPLETED.key,
                    retryCount = attemptsUsed.toLong(),
                    localImagePath = sceneFile.absolutePath
                )
                updateProject(
                    projectId = projectId,
                    status = ProjectStatus.GENERATING.key,
                    progressPercent = sceneProgress,
                    completedScenes = index + 1
                )
            }

            // 5. Synthesize voices, dialogues, BGM, SFX & lip-sync envelopes
            _pipelineState.update {
                it.copy(
                    phase = PipelinePhase.SYNTHESIZING_VOICES,
                    progressPercent = 74,
                    statusDetail = "Synthesizing ${language.label} voices, lip-sync & soundtrack..."
                )
            }
            val pitchMap = script.characters.associate { it.name to it.voicePitch }
            val minPerSceneSec = (durationOption.targetSeconds / totalScenes).coerceIn(3, 15)
            val sceneInputs = mutableListOf<SceneRenderInput>()

            for ((index, sceneSpec) in script.scenes.withIndex()) {
                val audio = audioEngine.synthesizeSceneAudio(
                    sceneIndex = index,
                    dialogues = sceneSpec.dialogues,
                    language = language,
                    moodMusic = sceneSpec.moodMusic,
                    sfxCue = sceneSpec.sfxCue,
                    targetMinSeconds = minPerSceneSec,
                    characterPitches = pitchMap
                )
                sceneInputs.add(
                    SceneRenderInput(
                        scene = initialSceneRecords[index],
                        bitmap = sceneBitmaps[index],
                        audio = audio
                    )
                )
            }

            // 6. Encode combined scenes + audio into final MP4 movie
            _pipelineState.update {
                it.copy(
                    phase = PipelinePhase.ENCODING_VIDEO,
                    progressPercent = 84,
                    statusDetail = "Rendering final animated MP4 video..."
                )
            }
            val renderResult = videoRenderer.renderMovieMp4(
                projectId = projectId,
                movieTitle = movieTitle,
                aspectRatio = aspectRatio,
                resolution = resolution,
                sceneInputs = sceneInputs
            ) { pct, detail ->
                val overall = 84 + (pct * 0.15f).toInt()
                _pipelineState.update {
                    it.copy(
                        progressPercent = overall.coerceIn(84, 99),
                        statusDetail = detail
                    )
                }
            }

            val thumbBase64 = sceneBitmaps.firstOrNull()?.let { encodeThumbnailBase64(it) }

            updateProject(
                projectId = projectId,
                status = ProjectStatus.COMPLETED.key,
                progressPercent = 100,
                completedScenes = totalScenes,
                durationSeconds = renderResult.durationSeconds,
                localVideoPath = renderResult.outputFile.absolutePath,
                thumbnailBase64 = thumbBase64
            )
            recordStats(videosIncrement = 1L, scenesIncrement = totalScenes.toLong())
            recordHistoryEntry(
                GenerationHistoryEntry(
                    userId = ownerId,
                    projectId = projectId,
                    projectTitle = movieTitle,
                    actionType = "FULL_MOVIE",
                    creditsUsed = 0L,
                    status = "COMPLETED",
                    durationLabel = durationOption.shortLabel,
                    language = language.label,
                    style = style.label,
                    details = "$totalScenes scenes • ${renderResult.actualWidth}x${renderResult.actualHeight} MP4"
                )
            )
            updatePreferences(style, language)

            lastFailedRequest = null
            _pipelineState.update {
                it.copy(
                    isRunning = false,
                    phase = PipelinePhase.COMPLETED,
                    progressPercent = 100,
                    statusDetail = "Cartoon video ready! (${renderResult.actualWidth}x${renderResult.actualHeight} MP4)",
                    completedVideoPath = renderResult.outputFile.absolutePath,
                    errorMessage = null
                )
            }
            onOpenPlayer(projectId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Cartoon generation pipeline failed", e)
            val errMsg = e.localizedMessage ?: "AI video service failed or is temporarily busy. Tap Retry to try again."
            projectId?.let { pid ->
                updateProject(
                    projectId = pid,
                    status = ProjectStatus.FAILED.key,
                    progressPercent = _pipelineState.value.progressPercent,
                    completedScenes = _pipelineState.value.sceneJobs.count { it.status == SceneStatus.COMPLETED },
                    errorMessage = errMsg
                )
                recordHistoryEntry(
                    GenerationHistoryEntry(
                        userId = ownerId,
                        projectId = pid,
                        projectTitle = _pipelineState.value.activeTitle.ifBlank { "Cartoon Video" },
                        actionType = "FULL_MOVIE",
                        creditsUsed = 0L,
                        status = "FAILED",
                        durationLabel = durationOption.shortLabel,
                        language = language.label,
                        style = style.label,
                        details = errMsg.take(280)
                    )
                )
            }
            _pipelineState.update {
                it.copy(
                    isRunning = false,
                    phase = PipelinePhase.FAILED,
                    statusDetail = "Generation failed",
                    errorMessage = errMsg
                )
            }
        }
    }

    fun regenerateSingleScene(project: CartoonProject, scene: SceneRecord, allScenes: List<SceneRecord>) {
        if (_pipelineState.value.isRunning) {
            _uiNotice.value = "Please wait for active rendering to complete."
            return
        }
        viewModelScope.launch {
            val style = CartoonStyle.fromKey(project.style)
            val ratio = VideoAspectRatio.fromKey(project.aspectRatio)
            val lang = CartoonLanguage.fromKey(project.language)
            val res = VideoResolution.fromKey(project.resolution)

            _pipelineState.value = GenerationPipelineState(
                isRunning = true,
                activeProjectId = project.id,
                activeTitle = project.title,
                phase = PipelinePhase.RENDERING_SCENES,
                progressPercent = 20,
                statusDetail = "Regenerating Scene ${scene.sceneIndex + 1}: ${scene.title}..."
            )
            try {
                updateScene(
                    projectId = project.id,
                    sceneId = scene.id,
                    status = SceneStatus.GENERATING.key,
                    retryCount = scene.retryCount + 1
                )

                val savedChars = (charactersState.value as? DataUiState.Success)?.data.orEmpty()
                val refMap = mutableMapOf<String, Bitmap>()
                savedChars.forEach { ch ->
                    loadSavedCharacterBitmap(ch)?.let { refMap[ch.name] = it }
                }

                val spec = GeneratedSceneSpec(
                    sceneIndex = scene.sceneIndex.toInt(),
                    title = scene.title,
                    visualPrompt = scene.visualPrompt,
                    cameraMotion = CameraMotion.fromKey(scene.cameraMotion),
                    moodMusic = MoodMusic.fromKey(scene.moodMusic),
                    sfxCue = SfxCue.fromKey(scene.sfxCue),
                    characterNames = scene.characterNames,
                    dialogues = scene.parsedDialogues()
                )
                val charSpecs = savedChars.map {
                    GeneratedCharacterSpec(
                        name = it.name,
                        role = it.role,
                        appearance = it.appearance,
                        outfit = it.outfit,
                        voiceGender = VoiceGender.fromKey(it.voiceGender),
                        voicePitch = it.voicePitch.toFloat()
                    )
                }
                val newBitmap = retryOperation(maxAttempts = 2) {
                    aiService.generateSceneArtwork(spec, charSpecs, refMap, style, ratio)
                }
                val file = saveSceneFrameFile(project.id, scene.sceneIndex.toInt(), newBitmap)

                updateScene(
                    projectId = project.id,
                    sceneId = scene.id,
                    status = SceneStatus.COMPLETED.key,
                    retryCount = scene.retryCount + 1,
                    localImagePath = file.absolutePath
                )

                _pipelineState.update {
                    it.copy(
                        phase = PipelinePhase.ENCODING_VIDEO,
                        progressPercent = 65,
                        statusDetail = "Rebuilding MP4 video with updated scene..."
                    )
                }
                val sorted = allScenes.sortedBy { it.sceneIndex }
                val inputs = sorted.map { sc ->
                    val bmp = if (sc.id == scene.id) {
                        newBitmap
                    } else {
                        sc.localImagePath?.let { BitmapFactory.decodeFile(it) } ?: newBitmap
                    }
                    val audio = audioEngine.synthesizeSceneAudio(
                        sceneIndex = sc.sceneIndex.toInt(),
                        dialogues = sc.parsedDialogues(),
                        language = lang,
                        moodMusic = MoodMusic.fromKey(sc.moodMusic),
                        sfxCue = SfxCue.fromKey(sc.sfxCue),
                        targetMinSeconds = 5,
                        characterPitches = emptyMap()
                    )
                    SceneRenderInput(sc, bmp, audio)
                }
                val renderResult = videoRenderer.renderMovieMp4(
                    projectId = project.id,
                    movieTitle = project.title,
                    aspectRatio = ratio,
                    resolution = res,
                    sceneInputs = inputs
                ) { pct, msg ->
                    _pipelineState.update { it.copy(progressPercent = 65 + (pct * 0.34f).toInt(), statusDetail = msg) }
                }

                val updatedThumb = if (scene.sceneIndex == 0L) encodeThumbnailBase64(newBitmap) else null
                updateProject(
                    projectId = project.id,
                    status = ProjectStatus.COMPLETED.key,
                    progressPercent = 100,
                    completedScenes = sorted.size,
                    durationSeconds = renderResult.durationSeconds,
                    localVideoPath = renderResult.outputFile.absolutePath,
                    thumbnailBase64 = updatedThumb
                )
                recordHistoryEntry(
                    GenerationHistoryEntry(
                        userId = ownerId,
                        projectId = project.id,
                        projectTitle = project.title,
                        actionType = "SCENE_REGEN",
                        creditsUsed = 0L,
                        status = "COMPLETED",
                        durationLabel = "${renderResult.durationSeconds}s",
                        language = lang.label,
                        style = style.label,
                        details = "Regenerated Scene ${scene.sceneIndex + 1}"
                    )
                )
                _pipelineState.update {
                    it.copy(
                        isRunning = false,
                        phase = PipelinePhase.COMPLETED,
                        progressPercent = 100,
                        statusDetail = "Scene ${scene.sceneIndex + 1} updated in MP4!",
                        completedVideoPath = renderResult.outputFile.absolutePath
                    )
                }
                _uiNotice.value = "Scene ${scene.sceneIndex + 1} regenerated and merged into MP4!"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val err = e.localizedMessage ?: "Failed to regenerate scene."
                updateScene(
                    projectId = project.id,
                    sceneId = scene.id,
                    status = SceneStatus.FAILED.key,
                    retryCount = scene.retryCount + 1,
                    errorMessage = err
                )
                _pipelineState.update {
                    it.copy(isRunning = false, phase = PipelinePhase.FAILED, errorMessage = err)
                }
                _uiNotice.value = err
            }
        }
    }

    fun createCharacter(
        name: String,
        role: String,
        appearance: String,
        outfit: String,
        personality: String,
        voiceGender: VoiceGender,
        voicePitch: Float,
        expression: CharacterExpression,
        style: CartoonStyle,
        generateAiPortrait: Boolean,
        onComplete: () -> Unit
    ) {
        if (name.isBlank() || appearance.isBlank() || outfit.isBlank()) {
            _uiNotice.value = "Please enter character name, appearance, and outfit."
            return
        }
        viewModelScope.launch {
            _characterGenBusy.value = true
            try {
                var localPath: String? = null
                var base64Thumb: String? = null
                if (generateAiPortrait) {
                    val bmp = aiService.generateCharacterPortrait(
                        GeneratedCharacterSpec(
                            name = name.trim(),
                            role = role.trim().ifBlank { "Hero" },
                            appearance = appearance.trim(),
                            outfit = outfit.trim(),
                            voiceGender = voiceGender,
                            voicePitch = voicePitch
                        ),
                        style
                    )
                    val file = saveCharacterSheetFile("custom", "${name.trim()}_${System.currentTimeMillis()}", bmp)
                    localPath = file.absolutePath
                    base64Thumb = encodeThumbnailBase64(bmp)
                }

                val character = SavedCharacter(
                    userId = ownerId,
                    name = name.trim(),
                    role = role.trim().ifBlank { "Lead Character" },
                    appearance = appearance.trim(),
                    outfit = outfit.trim(),
                    personality = personality.trim().takeIf { it.isNotEmpty() },
                    voiceGender = voiceGender.key,
                    voicePitch = voicePitch.toDouble(),
                    defaultExpression = expression.key,
                    style = style.key,
                    localPortraitPath = localPath,
                    portraitBase64 = base64Thumb
                )

                val result = if (repository != null && !currentUserId.isNullOrBlank()) {
                    repository.saveCharacter(character)
                } else {
                    localStore.saveCharacter(character)
                }

                result.onSuccess {
                    _uiNotice.value = "Saved character \"$name\"!"
                    onComplete()
                }.onFailure {
                    _uiNotice.value = it.localizedMessage ?: "Failed to save character."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiNotice.value = e.localizedMessage ?: "Failed to create character portrait."
            } finally {
                _characterGenBusy.value = false
            }
        }
    }

    fun deleteCharacter(characterId: String) {
        viewModelScope.launch {
            val result = if (repository != null && !currentUserId.isNullOrBlank()) {
                repository.deleteCharacter(characterId)
            } else {
                localStore.deleteCharacter(characterId)
            }
            result.onFailure { _uiNotice.value = it.localizedMessage ?: "Could not delete character." }
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            if (_selectedProjectId.value == projectId) _selectedProjectId.value = null
            val result = if (repository != null && !currentUserId.isNullOrBlank()) {
                repository.deleteProject(projectId)
            } else {
                localStore.deleteProject(projectId)
            }
            result.onFailure { _uiNotice.value = it.localizedMessage ?: "Could not delete project." }
        }
    }

    fun deleteHistoryEntry(historyId: String) {
        viewModelScope.launch {
            val result = if (repository != null && !currentUserId.isNullOrBlank()) {
                repository.deleteHistoryEntry(historyId)
            } else {
                localStore.deleteHistoryEntry(historyId)
            }
            result.onSuccess {
                _uiNotice.value = "History item deleted."
            }.onFailure {
                _uiNotice.value = it.localizedMessage ?: "Could not delete history item."
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            val result = if (repository != null && !currentUserId.isNullOrBlank()) {
                repository.clearHistory()
            } else {
                localStore.clearHistory()
            }
            result.onSuccess {
                _uiNotice.value = "Video history cleared."
            }.onFailure {
                _uiNotice.value = it.localizedMessage ?: "Could not clear history."
            }
        }
    }

    fun recordVideoDownload(
        project: CartoonProject,
        fileName: String,
        displayLocation: String,
        bytesCopied: Long
    ) {
        viewModelScope.launch {
            val sizeText = com.example.media.VideoExportHelper.formatByteSize(bytesCopied)
            recordHistoryEntry(
                GenerationHistoryEntry(
                    userId = ownerId,
                    projectId = project.id.ifBlank { "local_movie" },
                    projectTitle = project.title.ifBlank { fileName },
                    actionType = "DOWNLOAD_MP4",
                    creditsUsed = 0L,
                    status = "COMPLETED",
                    durationLabel = "${project.durationSeconds}s",
                    language = CartoonLanguage.fromKey(project.language).label,
                    style = CartoonStyle.fromKey(project.style).label,
                    details = "Saved $fileName ($sizeText) to $displayLocation"
                )
            )
        }
    }

    // Dual-mode storage helpers
    private suspend fun createProjectRecord(project: CartoonProject): Result<String> =
        if (repository != null && !currentUserId.isNullOrBlank()) {
            repository.createProject(project)
        } else {
            localStore.createProject(project)
        }

    private suspend fun updateProject(
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
    ): Result<Unit> =
        if (repository != null && !currentUserId.isNullOrBlank()) {
            repository.updateProjectProgress(
                projectId = projectId,
                title = title,
                prompt = prompt,
                status = status,
                progressPercent = progressPercent,
                sceneCount = sceneCount,
                completedScenes = completedScenes,
                durationSeconds = durationSeconds,
                scriptSummary = scriptSummary,
                localVideoPath = localVideoPath,
                thumbnailBase64 = thumbnailBase64,
                errorMessage = errorMessage
            )
        } else {
            localStore.updateProjectProgress(
                projectId = projectId,
                title = title,
                prompt = prompt,
                status = status,
                progressPercent = progressPercent,
                sceneCount = sceneCount,
                completedScenes = completedScenes,
                durationSeconds = durationSeconds,
                scriptSummary = scriptSummary,
                localVideoPath = localVideoPath,
                thumbnailBase64 = thumbnailBase64,
                errorMessage = errorMessage
            )
        }

    private suspend fun saveScenes(projectId: String, scenes: List<SceneRecord>): Result<List<String>> =
        if (repository != null && !currentUserId.isNullOrBlank()) {
            repository.saveScenesBatch(projectId, scenes)
        } else {
            localStore.saveScenesBatch(projectId, scenes)
        }

    private suspend fun updateScene(
        projectId: String,
        sceneId: String,
        status: String,
        retryCount: Long,
        localImagePath: String? = null,
        errorMessage: String? = null
    ): Result<Unit> =
        if (repository != null && !currentUserId.isNullOrBlank()) {
            repository.updateSceneStatus(
                projectId = projectId,
                sceneId = sceneId,
                status = status,
                retryCount = retryCount,
                localImagePath = localImagePath,
                errorMessage = errorMessage
            )
        } else {
            localStore.updateSceneStatus(
                projectId = projectId,
                sceneId = sceneId,
                status = status,
                retryCount = retryCount,
                localImagePath = localImagePath,
                errorMessage = errorMessage
            )
        }

    private suspend fun recordStats(videosIncrement: Long, scenesIncrement: Long) {
        if (repository != null && !currentUserId.isNullOrBlank()) {
            repository.deductCreditsAndRecordStats(0L, videosIncrement, scenesIncrement)
        } else {
            localStore.recordStats(videosIncrement, scenesIncrement)
        }
    }

    private suspend fun recordHistoryEntry(entry: GenerationHistoryEntry) {
        if (repository != null && !currentUserId.isNullOrBlank()) {
            repository.addHistoryEntry(entry)
        } else {
            localStore.addHistoryEntry(entry)
        }
    }

    private suspend fun updatePreferences(style: CartoonStyle, language: CartoonLanguage) {
        if (repository != null && !currentUserId.isNullOrBlank()) {
            repository.updateUserPreferences(style, language)
        } else {
            localStore.updatePreferences(style, language)
        }
    }

    private suspend fun <T> retryOperation(
        maxAttempts: Int,
        onRetry: (Int) -> Unit = {},
        block: suspend () -> T
    ): T {
        var lastError: Throwable? = null
        for (attempt in 1..maxAttempts) {
            try {
                return block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastError = e
                if (attempt < maxAttempts) {
                    onRetry(attempt)
                    delay(1200L * attempt)
                }
            }
        }
        throw lastError ?: IllegalStateException("Operation failed after $maxAttempts attempts")
    }

    private suspend fun saveSceneFrameFile(projectId: String, sceneIdx: Int, bitmap: Bitmap): File =
        withContext(Dispatchers.IO) {
            val dir = File(filesDir, "frames/$projectId").apply { mkdirs() }
            val file = File(dir, "scene_${sceneIdx}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            file
        }

    private suspend fun saveCharacterSheetFile(scopeKey: String, name: String, bitmap: Bitmap): File =
        withContext(Dispatchers.IO) {
            val safe = name.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            val dir = File(filesDir, "characters/$scopeKey").apply { mkdirs() }
            val file = File(dir, "$safe.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            file
        }

    private fun loadSavedCharacterBitmap(ch: SavedCharacter): Bitmap? {
        ch.localPortraitPath?.let { path ->
            val f = File(path)
            if (f.exists()) return BitmapFactory.decodeFile(path)
        }
        ch.portraitBase64?.let { b64 ->
            return runCatching {
                val bytes = Base64.decode(b64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }.getOrNull()
        }
        return null
    }

    private suspend fun encodeThumbnailBase64(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        val scaled = Bitmap.createScaledBitmap(bitmap, 280, (280f * bitmap.height / bitmap.width).toInt().coerceAtLeast(160), true)
        val baos = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 72, baos)
        Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
    }

    companion object {
        fun provideFactory(
            repository: CartoonRepository?,
            localStore: LocalStudioStore,
            aiService: CartoonAiService,
            audioEngine: VoiceAndAudioEngine,
            videoRenderer: CartoonVideoRenderer,
            filesDir: File,
            currentUserId: String?
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                CartoonStudioViewModel(
                    repository = repository,
                    localStore = localStore,
                    aiService = aiService,
                    audioEngine = audioEngine,
                    videoRenderer = videoRenderer,
                    filesDir = filesDir,
                    currentUserId = currentUserId
                )
            }
        }
    }
}
