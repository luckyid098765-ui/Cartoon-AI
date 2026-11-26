package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CartoonLanguage
import com.example.data.model.CartoonProject
import com.example.data.model.CartoonStyle
import com.example.data.model.GenerationPipelineState
import com.example.data.model.SceneRecord
import com.example.data.model.VideoResolution
import com.example.ui.components.Base64OrPathImage
import com.example.ui.components.GlassStudioCard
import com.example.ui.components.VideoPlayerAndDownloadCard
import com.example.ui.theme.CyanSpark
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.StudioSurfaceCard
import com.example.ui.theme.SuccessMint

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
    onVideoDownloaded: (CartoonProject, com.example.media.VideoDownloadResult) -> Unit = { _, _ -> },
    onShowMessage: (String) -> Unit
) {
    BackHandler(onBack = onBack)

    if (project == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = ElectricViolet)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Loading cartoon project...", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    var showEditStoryDialog by remember { mutableStateOf(false) }
    var savedToProjectsConfirmed by remember(project.id) { mutableStateOf(false) }

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
            // 4. Save to My Projects + Edit Story
            // 5. REGENERATE Button
            item {
                VideoPlayerAndDownloadCard(
                    project = project,
                    isGenerationRunning = pipelineState.isRunning,
                    defaultMovieNumber = defaultMovieNumber,
                    onRegenerate = onRegenerateCartoon,
                    onShowMessage = onShowMessage,
                    onVideoDownloaded = onVideoDownloaded
                ) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
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
                                .weight(1.2f)
                                .minimumInteractiveComponentSize()
                                .height(50.dp)
                                .testTag("save_to_projects_button")
                        ) {
                            Icon(
                                imageVector = if (savedToProjectsConfirmed) Icons.Filled.BookmarkAdded else Icons.Filled.FolderSpecial,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (savedToProjectsConfirmed) {
                                    stringResource(R.string.btn_saved_to_projects)
                                } else {
                                    stringResource(R.string.btn_save_to_projects)
                                },
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
                                .weight(0.8f)
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
                    enabled = editedPrompt.isNotBlank()
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
