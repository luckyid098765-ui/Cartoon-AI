package com.example.ui.screens

import android.os.Build
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.data.model.CartoonLanguage
import com.example.data.model.CartoonProject
import com.example.data.model.CartoonStyle
import com.example.data.model.GenerationPipelineState
import com.example.data.model.SceneRecord
import com.example.data.model.VideoAspectRatio
import com.example.data.model.VideoResolution
import com.example.media.VideoExportHelper
import com.example.ui.components.Base64OrPathImage
import com.example.ui.components.GlassStudioCard
import com.example.ui.theme.CyanSpark
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ErrorCoral
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceCard
import com.example.ui.theme.SuccessMint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

private sealed interface DownloadUiState {
    data object Idle : DownloadUiState
    data class Downloading(
        val progress: Float,
        val bytesCopied: Long,
        val totalBytes: Long,
        val fileName: String
    ) : DownloadUiState

    data class Completed(
        val fileName: String,
        val displayLocation: String,
        val bytesCopied: Long
    ) : DownloadUiState

    data class Failed(
        val message: String,
        val fileName: String
    ) : DownloadUiState
}

@Composable
fun VideoPlayerScreen(
    project: CartoonProject?,
    scenes: List<SceneRecord>,
    pipelineState: GenerationPipelineState,
    defaultMovieNumber: Int = 1,
    onBack: () -> Unit,
    onSaveToMyProjects: (CartoonProject) -> Unit,
    onRegenerateCartoon: (CartoonProject) -> Unit,
    onRegenerateScene: (CartoonProject, SceneRecord, List<SceneRecord>) -> Unit,
    onEditStoryAndRegenerate: (CartoonProject, String, String) -> Unit,
    onShowMessage: (String) -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    if (project == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = ElectricViolet)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Loading movie project...", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    val videoFile = remember(project.localVideoPath) {
        project.localVideoPath
            ?.let { File(it) }
            ?.takeIf { it.exists() && it.isFile && it.length() > 1024L && it.extension.equals("mp4", true) }
    }
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var durationMs by remember(project.localVideoPath) {
        mutableIntStateOf((project.durationSeconds * 1000).toInt().coerceAtLeast(1000))
    }
    var currentPosMs by remember(project.localVideoPath) { mutableIntStateOf(0) }
    var sliderFraction by remember(project.localVideoPath) { mutableFloatStateOf(0f) }
    var showEditStoryDialog by remember { mutableStateOf(false) }

    var downloadState by remember(project.id, project.localVideoPath) {
        mutableStateOf<DownloadUiState>(DownloadUiState.Idle)
    }
    var pendingSafFileName by remember(project.id) {
        mutableStateOf(VideoExportHelper.formatMovieFilename(defaultMovieNumber))
    }
    var savedToProjectsConfirmed by remember(project.id) { mutableStateOf(false) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("video/mp4")
    ) { uri ->
        val file = videoFile
        if (uri == null || file == null) {
            if (downloadState is DownloadUiState.Downloading) {
                downloadState = DownloadUiState.Idle
            }
            return@rememberLauncherForActivityResult
        }
        val targetName = pendingSafFileName
        scope.launch {
            downloadState = DownloadUiState.Downloading(
                progress = 0f,
                bytesCopied = 0L,
                totalBytes = file.length(),
                fileName = targetName
            )
            VideoExportHelper.copyMp4ToUri(
                context = context,
                sourceFile = file,
                destinationUri = uri,
                fileName = targetName
            ) { progress, copied, total ->
                downloadState = DownloadUiState.Downloading(
                    progress = progress,
                    bytesCopied = copied,
                    totalBytes = total,
                    fileName = targetName
                )
            }.onSuccess { result ->
                downloadState = DownloadUiState.Completed(
                    fileName = result.fileName,
                    displayLocation = result.displayLocation,
                    bytesCopied = result.bytesCopied
                )
                onShowMessage("Download Complete: ${result.fileName}")
            }.onFailure { err ->
                val msg = err.localizedMessage ?: "Video download failed."
                downloadState = DownloadUiState.Failed(message = msg, fileName = targetName)
                onShowMessage(msg)
            }
        }
    }

    val startDownload: () -> Unit = start@{
        val file = videoFile
        if (file == null || pipelineState.isRunning) {
            onShowMessage("Generated MP4 video is not ready yet.")
            return@start
        }
        if (downloadState is DownloadUiState.Downloading) return@start

        val movieFileName = VideoExportHelper.nextMovieFilename(context, fallbackNumber = defaultMovieNumber)
        pendingSafFileName = movieFileName

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            scope.launch {
                downloadState = DownloadUiState.Downloading(
                    progress = 0f,
                    bytesCopied = 0L,
                    totalBytes = file.length(),
                    fileName = movieFileName
                )
                VideoExportHelper.saveMp4ToGallery(
                    context = context,
                    sourceFile = file,
                    fileName = movieFileName
                ) { progress, copied, total ->
                    downloadState = DownloadUiState.Downloading(
                        progress = progress,
                        bytesCopied = copied,
                        totalBytes = total,
                        fileName = movieFileName
                    )
                }.onSuccess { result ->
                    downloadState = DownloadUiState.Completed(
                        fileName = result.fileName,
                        displayLocation = result.displayLocation,
                        bytesCopied = result.bytesCopied
                    )
                    onShowMessage("Download Complete: ${result.fileName}")
                }.onFailure { err ->
                    val msg = err.localizedMessage ?: "Unable to download MP4 video."
                    downloadState = DownloadUiState.Failed(message = msg, fileName = movieFileName)
                    onShowMessage(msg)
                }
            }
        } else {
            createDocumentLauncher.launch(movieFileName)
        }
    }

    // Poll playback position smoothly while playing
    LaunchedEffect(isPlaying, videoViewRef) {
        while (isPlaying) {
            val vv = videoViewRef
            if (vv != null && vv.isPlaying) {
                currentPosMs = vv.currentPosition
                val d = vv.duration.takeIf { it > 0 } ?: durationMs
                durationMs = d
                sliderFraction = (currentPosMs.toFloat() / d).coerceIn(0f, 1f)
            }
            delay(250L)
        }
    }

    DisposableEffect(videoFile?.absolutePath) {
        onDispose {
            videoViewRef?.stopPlayback()
            isPlaying = false
        }
    }

    val aspectRatioFloat = when (VideoAspectRatio.fromKey(project.aspectRatio)) {
        VideoAspectRatio.RATIO_16_9 -> 16f / 9f
        VideoAspectRatio.RATIO_9_16 -> 9f / 16f
        VideoAspectRatio.RATIO_1_1 -> 1f
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 780.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = project.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${CartoonStyle.fromKey(project.style).label} • ${CartoonLanguage.fromKey(project.language).label} • ${VideoResolution.fromKey(project.resolution).label}",
                            style = MaterialTheme.typography.bodySmall,
                            color = CyanSpark
                        )
                    }
                }
            }

            // FINAL VIDEO LAYOUT:
            // 1. Generated Cartoon Video Player
            // 2. Play / Pause Controls
            // 3. GREEN DOWNLOAD VIDEO BUTTON (+ Progress / Download Complete / Retry Download)
            // 4. Save to My Projects
            // 5. Regenerate Cartoon Button (+ Edit Story)
            item {
                GlassStudioCard(highlighted = true, modifier = Modifier.fillMaxWidth()) {
                    // 1. Generated Cartoon Video Player
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(aspectRatioFloat.coerceIn(0.65f, 1.85f))
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black)
                            .border(1.dp, StudioOutline, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (videoFile != null) {
                            key(videoFile.absolutePath) {
                                AndroidView(
                                    factory = { ctx ->
                                        VideoView(ctx).apply {
                                            setVideoPath(videoFile.absolutePath)
                                            setOnPreparedListener { mp ->
                                                durationMs = mp.duration.coerceAtLeast(1000)
                                                mp.isLooping = false
                                            }
                                            setOnCompletionListener {
                                                isPlaying = false
                                                currentPosMs = 0
                                                sliderFraction = 0f
                                            }
                                            videoViewRef = this
                                        }
                                    },
                                    update = { vv ->
                                        videoViewRef = vv
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Movie,
                                    contentDescription = "Video preview unavailable",
                                    tint = ElectricViolet,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (pipelineState.isRunning) {
                                        "Rendering MP4 (${pipelineState.progressPercent}%)..."
                                    } else {
                                        project.errorMessage ?: "MP4 not rendered yet."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 2. Play / Pause Controls & Seekbar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                val vv = videoViewRef
                                if (vv != null && videoFile != null) {
                                    if (vv.isPlaying) {
                                        vv.pause()
                                        isPlaying = false
                                    } else {
                                        vv.start()
                                        isPlaying = true
                                    }
                                } else {
                                    onShowMessage("Video file is not ready yet.")
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(ElectricViolet, ElectricBlue)))
                                .testTag("video_play_pause_button")
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Slider(
                                value = sliderFraction,
                                onValueChange = { frac ->
                                    sliderFraction = frac
                                    val targetMs = (frac * durationMs).toInt()
                                    currentPosMs = targetMs
                                    videoViewRef?.seekTo(targetMs)
                                },
                                colors = SliderDefaults.colors(
                                    thumbColor = CyanSpark,
                                    activeTrackColor = ElectricViolet,
                                    inactiveTrackColor = StudioSurface
                                )
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = formatMs(currentPosMs),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatMs(durationMs),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3. LARGE GREEN DOWNLOAD VIDEO BUTTON
                    val isVideoReady = videoFile != null && !pipelineState.isRunning
                    val isDownloading = downloadState is DownloadUiState.Downloading
                    GreenDownloadVideoButton(
                        text = when (val st = downloadState) {
                            is DownloadUiState.Downloading -> "DOWNLOADING ${(st.progress * 100).toInt()}%"
                            else -> stringResource(R.string.btn_download_mp4).uppercase()
                        },
                        enabled = isVideoReady && !isDownloading,
                        isDownloading = isDownloading,
                        onClick = startDownload,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("download_mp4_button")
                    )

                    if (!isVideoReady) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (pipelineState.isRunning) {
                                "Download button unlocks automatically once MP4 rendering completes."
                            } else {
                                "Generate or complete the MP4 cartoon video to enable download."
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Download Progress Indicator
                    AnimatedVisibility(visible = downloadState is DownloadUiState.Downloading) {
                        val st = downloadState as? DownloadUiState.Downloading
                        if (st != null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF0A2817))
                                    .border(1.dp, Color(0xFF22C55E).copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Downloading ${st.fileName}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF86EFAC)
                                    )
                                    Text(
                                        text = "${(st.progress * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4ADE80)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { st.progress.coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(50)),
                                    color = Color(0xFF22C55E),
                                    trackColor = Color(0xFF14532D)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${VideoExportHelper.formatByteSize(st.bytesCopied)} / ${VideoExportHelper.formatByteSize(st.totalBytes)} MP4",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFBBF7D0)
                                )
                            }
                        }
                    }

                    // Download Complete Message
                    AnimatedVisibility(visible = downloadState is DownloadUiState.Completed) {
                        val st = downloadState as? DownloadUiState.Completed
                        if (st != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF092A19))
                                    .border(1.dp, Color(0xFF22C55E), RoundedCornerShape(14.dp))
                                    .padding(12.dp)
                                    .testTag("download_complete_banner"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Download Complete",
                                    tint = Color(0xFF4ADE80),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.download_complete_title),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF86EFAC)
                                    )
                                    Text(
                                        text = "${st.displayLocation} (${VideoExportHelper.formatByteSize(st.bytesCopied)})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFDCFCE7)
                                    )
                                }
                            }
                        }
                    }

                    // Download Failed + Retry Download Button
                    AnimatedVisibility(visible = downloadState is DownloadUiState.Failed) {
                        val st = downloadState as? DownloadUiState.Failed
                        if (st != null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF2C0F18))
                                    .border(1.dp, ErrorCoral.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.ErrorOutline,
                                        contentDescription = "Download failed",
                                        tint = ErrorCoral,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = st.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFFFD4DF)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = startDownload,
                                    enabled = isVideoReady,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ErrorCoral,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .minimumInteractiveComponentSize()
                                        .testTag("retry_download_button")
                                ) {
                                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.btn_retry_download), fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 4. Save to My Projects
                    OutlinedButton(
                        onClick = {
                            savedToProjectsConfirmed = true
                            onSaveToMyProjects(project)
                        },
                        enabled = !pipelineState.isRunning,
                        border = BorderStroke(
                            1.dp,
                            if (savedToProjectsConfirmed) SuccessMint else CyanSpark.copy(alpha = 0.7f)
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (savedToProjectsConfirmed) SuccessMint.copy(alpha = 0.12f) else Color.Transparent,
                            contentColor = if (savedToProjectsConfirmed) SuccessMint else CyanSpark
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .height(50.dp)
                            .testTag("save_to_projects_button")
                    ) {
                        Icon(
                            imageVector = if (savedToProjectsConfirmed) Icons.Filled.BookmarkAdded else Icons.Filled.FolderSpecial,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (savedToProjectsConfirmed) {
                                stringResource(R.string.btn_saved_to_projects)
                            } else {
                                stringResource(R.string.btn_save_to_projects)
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 5. Regenerate Cartoon Button + Edit Story
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onRegenerateCartoon(project) },
                            enabled = !pipelineState.isRunning,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricViolet,
                                contentColor = Color.White,
                                disabledContainerColor = StudioSurfaceCard,
                                disabledContentColor = Color.White.copy(alpha = 0.45f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1.3f)
                                .minimumInteractiveComponentSize()
                                .height(50.dp)
                                .testTag("regenerate_cartoon_button")
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.btn_regenerate_cartoon),
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        OutlinedButton(
                            onClick = { showEditStoryDialog = true },
                            enabled = !pipelineState.isRunning,
                            border = BorderStroke(1.dp, ElectricBlue),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricBlue),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(0.9f)
                                .minimumInteractiveComponentSize()
                                .height(50.dp)
                                .testTag("edit_story_button")
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.btn_edit_story))
                        }
                    }

                    if (pipelineState.isRunning && pipelineState.activeProjectId == project.id) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "${pipelineState.phase.label}: ${pipelineState.statusDetail} (${pipelineState.progressPercent}%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = CyanSpark
                        )
                    }
                }
            }

            // Script Summary
            if (!project.scriptSummary.isNullOrBlank()) {
                item {
                    GlassStudioCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Story & Script Overview",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = project.scriptSummary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Scene Breakdown with Regenerate Scene buttons
            item {
                Text(
                    text = "Scenes & Dialogues (${scenes.size})",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            items(scenes, key = { it.id }) { scene ->
                GlassStudioCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Base64OrPathImage(
                            localPath = scene.localImagePath,
                            base64 = null,
                            contentDescription = "Scene ${scene.sceneIndex + 1} frame",
                            modifier = Modifier
                                .size(96.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Scene ${scene.sceneIndex + 1}: ${scene.title}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Motion: ${scene.cameraMotion} • Music: ${scene.moodMusic} • SFX: ${scene.sfxCue}",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanSpark
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            scene.parsedDialogues().forEach { dlg ->
                                Text(
                                    text = "${dlg.speaker}: \"${dlg.line}\"",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = StudioSurfaceCard,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = scene.status,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (scene.status == "COMPLETED") SuccessMint else ElectricBlue,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        OutlinedButton(
                            onClick = { onRegenerateScene(project, scene, scenes) },
                            enabled = !pipelineState.isRunning,
                            border = BorderStroke(1.dp, ElectricViolet),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricViolet),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("regenerate_scene_${scene.sceneIndex}")
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.btn_regenerate_scene))
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }

    if (showEditStoryDialog) {
        var editedTitle by remember(project.id) { mutableStateOf(project.title) }
        var editedPrompt by remember(project.id) { mutableStateOf(project.prompt) }
        AlertDialog(
            onDismissRequest = { showEditStoryDialog = false },
            title = { Text("Edit Story & Regenerate") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editedTitle,
                        onValueChange = { editedTitle = it },
                        label = { Text("Movie Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editedPrompt,
                        onValueChange = { editedPrompt = it },
                        label = { Text("Cartoon Story Prompt") },
                        minLines = 4,
                        maxLines = 7,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEditStoryDialog = false
                        onEditStoryAndRegenerate(project, editedTitle, editedPrompt)
                    },
                    enabled = editedPrompt.trim().length >= 10
                ) {
                    Text("Save & Regenerate")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditStoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun GreenDownloadVideoButton(
    text: String,
    enabled: Boolean,
    isDownloading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeBrush = Brush.linearGradient(
        listOf(Color(0xFF15803D), Color(0xFF16A34A), Color(0xFF22C55E))
    )
    val disabledBrush = Brush.linearGradient(
        listOf(Color(0xFF1A2E23), Color(0xFF1D3327))
    )
    Box(
        modifier = modifier
            .height(56.dp)
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(18.dp))
            .background(if (enabled || isDownloading) activeBrush else disabledBrush)
            .border(
                width = 1.dp,
                color = if (enabled) Color(0xFF4ADE80).copy(alpha = 0.7f) else Color(0xFF2C4A3A),
                shape = RoundedCornerShape(18.dp)
            )
            .alpha(if (enabled || isDownloading) 1f else 0.55f)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isDownloading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.4.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = text,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun formatMs(ms: Int): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%02d:%02d".format(min, sec)
}
