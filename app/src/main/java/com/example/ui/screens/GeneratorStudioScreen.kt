package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.BgmTrackOption
import com.example.data.model.CartoonLanguage
import com.example.data.model.CartoonProject
import com.example.data.model.CartoonStyle
import com.example.data.model.GenerationPipelineState
import com.example.data.model.PipelinePhase
import com.example.data.model.SavedCharacter
import com.example.data.model.SceneStatus
import com.example.data.model.StoryGenre
import com.example.data.model.VideoAspectRatio
import com.example.data.model.VideoDurationOption
import com.example.data.model.VideoResolution
import com.example.ui.components.GlassStudioCard
import com.example.ui.components.GradientPrimaryButton
import com.example.ui.components.VideoPlayerAndDownloadCard
import com.example.ui.theme.CyanSpark
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ErrorCoral
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.SuccessMint
import com.example.ui.viewmodel.GeneratorFormState

private data class CartoonCategoryShortcut(
    val label: String,
    val style: CartoonStyle,
    val starterPrompt: String
)

private val categoryShortcuts = listOf(
    CartoonCategoryShortcut(
        label = "Funny Cartoons",
        style = CartoonStyle.FUNNY,
        starterPrompt = "A clumsy penguin chef tries to bake a giant rainbow birthday cake in an icy kitchen while two mischievous seals keep sneaking bites."
    ),
    CartoonCategoryShortcut(
        label = "3D Cartoons",
        style = CartoonStyle.CARTOON_3D,
        starterPrompt = "A tiny glowing star-sprite and a brave cardboard robot cross a neon toy city at midnight to find the missing music box key."
    ),
    CartoonCategoryShortcut(
        label = "Animated Stories",
        style = CartoonStyle.CINEMATIC,
        starterPrompt = "Two siblings sail a paper lantern boat across a starry cloud sea to reunite a lost baby comet with its constellation family."
    ),
    CartoonCategoryShortcut(
        label = "Kids Cartoons",
        style = CartoonStyle.CARTOON_2D,
        starterPrompt = "Sunny the singing school bus takes a class of cheerful animal friends on a magical rainbow trip to visit the friendly moon."
    ),
    CartoonCategoryShortcut(
        label = "Animal Cartoons",
        style = CartoonStyle.ADVENTURE,
        starterPrompt = "A clever red fox and a giant gentle bear follow an ancient acorn map through emerald canyon waterfalls to find the Golden Treehouse."
    ),
    CartoonCategoryShortcut(
        label = "Fantasy Cartoons",
        style = CartoonStyle.FANTASY,
        starterPrompt = "A young apprentice wizard with a glowing crystal owl brews a potion of dancing fireflies to wake the sleeping crystal forest."
    )
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GeneratorStudioScreen(
    formState: GeneratorFormState,
    pipelineState: GenerationPipelineState,
    completedProject: CartoonProject?,
    totalProjectsCount: Int,
    savedCharacters: List<SavedCharacter>,
    onPromptChange: (String) -> Unit,
    onClearPrompt: () -> Unit,
    onRandomStory: () -> Unit,
    onGenerateStory: () -> Unit,
    onApplyCategoryTheme: (CartoonStyle, String) -> Unit,
    onSelectStyle: (CartoonStyle) -> Unit,
    onSelectGenre: (StoryGenre) -> Unit,
    onSelectDurationMinutes: (Int) -> Unit,
    onSelectBgmTrack: (BgmTrackOption) -> Unit,
    onSelectLanguage: (CartoonLanguage) -> Unit,
    onSelectAspectRatio: (VideoAspectRatio) -> Unit,
    onSelectResolution: (VideoResolution) -> Unit,
    onToggleCharacter: (String) -> Unit,
    onGenerateCartoon: () -> Unit,
    onRetryGeneration: () -> Unit,
    onRegenerateProject: (CartoonProject) -> Unit,
    onCancelGeneration: () -> Unit,
    onOpenVideoPlayer: (String) -> Unit,
    onVideoDownloaded: (CartoonProject, com.example.media.VideoDownloadResult) -> Unit = { _, _ -> },
    onShowMessage: (String) -> Unit
) {
    val listState = rememberLazyListState()

    // Scroll to the generated video player section right below when rendering finishes
    LaunchedEffect(pipelineState.phase, completedProject?.localVideoPath) {
        if (pipelineState.phase == PipelinePhase.COMPLETED && completedProject?.localVideoPath != null) {
            val lastIndex = (listState.layoutInfo.totalItemsCount - 2).coerceAtLeast(0)
            listState.animateScrollToItem(lastIndex)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 860.dp

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "AI Cartoon Video Generator",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Paste any cartoon prompt and generate unlimited animated MP4 videos — no login or quota limits.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Quick category pills: Funny, 3D, Animated Stories, Kids, Animal, Fantasy
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categoryShortcuts, key = { it.label }) { shortcut ->
                        val active = formState.style == shortcut.style
                        FilterChip(
                            selected = active,
                            onClick = { onApplyCategoryTheme(shortcut.style, shortcut.starterPrompt) },
                            label = { Text(shortcut.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricViolet.copy(alpha = 0.34f),
                                selectedLabelColor = Color.White,
                                containerColor = StudioSurface,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = active,
                                borderColor = StudioOutline,
                                selectedBorderColor = CyanSpark
                            )
                        )
                    }
                }
            }

            if (isWide) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1.2f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            PromptEditorCard(
                                formState = formState,
                                onPromptChange = onPromptChange,
                                onClearPrompt = onClearPrompt,
                                onRandomStory = onRandomStory,
                                onGenerateStory = onGenerateStory,
                                onShowMessage = onShowMessage
                            )
                            StyleAndLanguageCard(
                                formState = formState,
                                onSelectStyle = onSelectStyle,
                                onSelectGenre = onSelectGenre,
                                onSelectLanguage = onSelectLanguage
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            DurationAndVideoSettingsCard(
                                formState = formState,
                                onSelectDurationMinutes = onSelectDurationMinutes,
                                onSelectBgmTrack = onSelectBgmTrack,
                                onSelectAspectRatio = onSelectAspectRatio,
                                onSelectResolution = onSelectResolution
                            )
                            if (savedCharacters.isNotEmpty()) {
                                CastConsistencyCard(
                                    savedCharacters = savedCharacters,
                                    selectedIds = formState.selectedCharacterIds,
                                    onToggleCharacter = onToggleCharacter
                                )
                            }
                            GenerateActionAndPipelineCard(
                                formState = formState,
                                pipelineState = pipelineState,
                                onGenerateCartoon = onGenerateCartoon,
                                onRetryGeneration = onRetryGeneration,
                                onCancelGeneration = onCancelGeneration
                            )
                        }
                    }
                }
            } else {
                item {
                    PromptEditorCard(
                        formState = formState,
                        onPromptChange = onPromptChange,
                        onClearPrompt = onClearPrompt,
                        onRandomStory = onRandomStory,
                        onGenerateStory = onGenerateStory,
                        onShowMessage = onShowMessage
                    )
                }
                item {
                    GenerateActionAndPipelineCard(
                        formState = formState,
                        pipelineState = pipelineState,
                        onGenerateCartoon = onGenerateCartoon,
                        onRetryGeneration = onRetryGeneration,
                        onCancelGeneration = onCancelGeneration
                    )
                }
                item {
                    StyleAndLanguageCard(
                        formState = formState,
                        onSelectStyle = onSelectStyle,
                        onSelectGenre = onSelectGenre,
                        onSelectLanguage = onSelectLanguage
                    )
                }
                item {
                    DurationAndVideoSettingsCard(
                        formState = formState,
                        onSelectDurationMinutes = onSelectDurationMinutes,
                        onSelectBgmTrack = onSelectBgmTrack,
                        onSelectAspectRatio = onSelectAspectRatio,
                        onSelectResolution = onSelectResolution
                    )
                }
                if (savedCharacters.isNotEmpty()) {
                    item {
                        CastConsistencyCard(
                            savedCharacters = savedCharacters,
                            selectedIds = formState.selectedCharacterIds,
                            onToggleCharacter = onToggleCharacter
                        )
                    }
                }
            }

            // 4. Video Download Section — automatically displayed below after successful generation
            if (completedProject != null &&
                completedProject.localVideoPath != null &&
                (pipelineState.phase == PipelinePhase.COMPLETED || pipelineState.activeProjectId == completedProject.id)
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("generated_video_section"),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Generated Cartoon Video",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = SuccessMint
                                )
                                Text(
                                    text = completedProject.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = CyanSpark,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            OutlinedButton(
                                onClick = { onOpenVideoPlayer(completedProject.id) },
                                border = BorderStroke(1.dp, ElectricBlue),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricBlue),
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Scenes")
                            }
                        }

                        VideoPlayerAndDownloadCard(
                            project = completedProject,
                            isGenerationRunning = pipelineState.isRunning,
                            defaultMovieNumber = totalProjectsCount.coerceAtLeast(1),
                            onRegenerate = onRegenerateProject,
                            onShowMessage = onShowMessage,
                            onVideoDownloaded = onVideoDownloaded
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PromptEditorCard(
    formState: GeneratorFormState,
    onPromptChange: (String) -> Unit,
    onClearPrompt: () -> Unit,
    onRandomStory: () -> Unit,
    onGenerateStory: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current

    GlassStudioCard(highlighted = true, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Cartoon Video Prompt",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "${formState.prompt.length} chars",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = formState.prompt,
            onValueChange = onPromptChange,
            placeholder = {
                Text(
                    text = stringResource(R.string.prompt_placeholder),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                )
            },
            minLines = 5,
            maxLines = 9,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = StudioSurface,
                unfocusedContainerColor = StudioSurface,
                focusedBorderColor = ElectricViolet,
                unfocusedBorderColor = StudioOutline,
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("story_prompt_input")
        )

        if (!formState.storyIdeaError.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = formState.storyIdeaError,
                style = MaterialTheme.typography.bodySmall,
                color = ErrorCoral
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onClearPrompt,
                enabled = formState.prompt.isNotEmpty(),
                border = BorderStroke(1.dp, StudioOutline),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("clear_prompt_button")
            ) {
                Icon(Icons.Filled.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.btn_clear_prompt))
            }

            OutlinedButton(
                onClick = {
                    val clipText = clipboard.getText()?.text
                    if (!clipText.isNullOrBlank()) {
                        onPromptChange(clipText)
                        onShowMessage("Prompt pasted!")
                    } else {
                        onShowMessage("Clipboard is empty.")
                    }
                },
                border = BorderStroke(1.dp, CyanSpark.copy(alpha = 0.7f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanSpark),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("paste_prompt_button")
            ) {
                Icon(Icons.Filled.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.btn_paste_prompt))
            }

            OutlinedButton(
                onClick = {
                    if (formState.prompt.isNotBlank()) {
                        clipboard.setText(AnnotatedString(formState.prompt))
                        onShowMessage("Prompt copied!")
                    }
                },
                enabled = formState.prompt.isNotBlank(),
                border = BorderStroke(1.dp, StudioOutline),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("copy_prompt_button")
            ) {
                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.btn_copy_prompt))
            }

            OutlinedButton(
                onClick = onGenerateStory,
                enabled = !formState.isGeneratingStoryIdea,
                border = BorderStroke(1.dp, ElectricViolet),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricViolet),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("generate_story_button")
            ) {
                if (formState.isGeneratingStoryIdea) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.btn_generate_story))
            }

            OutlinedButton(
                onClick = onRandomStory,
                border = BorderStroke(1.dp, ElectricBlue),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricBlue),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("random_story_button")
            ) {
                Icon(Icons.Filled.Casino, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.btn_random_story))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StyleAndLanguageCard(
    formState: GeneratorFormState,
    onSelectStyle: (CartoonStyle) -> Unit,
    onSelectGenre: (StoryGenre) -> Unit,
    onSelectLanguage: (CartoonLanguage) -> Unit
) {
    GlassStudioCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Cartoon Style (2D & 3D)",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CartoonStyle.entries.forEach { style ->
                val selected = formState.style == style
                FilterChip(
                    selected = selected,
                    onClick = { onSelectStyle(style) },
                    label = { Text(style.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricViolet.copy(alpha = 0.28f),
                        selectedLabelColor = Color.White,
                        containerColor = StudioSurface,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = StudioOutline,
                        selectedBorderColor = ElectricViolet
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Story Genre",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            StoryGenre.entries.forEach { genre ->
                val selected = formState.storyGenre == genre
                FilterChip(
                    selected = selected,
                    onClick = { onSelectGenre(genre) },
                    label = { Text(genre.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyanSpark.copy(alpha = 0.24f),
                        selectedLabelColor = Color.White,
                        containerColor = StudioSurface,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = StudioOutline,
                        selectedBorderColor = CyanSpark
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Voice & Dialogue Language",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CartoonLanguage.entries.forEach { lang ->
                val selected = formState.language == lang
                FilterChip(
                    selected = selected,
                    onClick = { onSelectLanguage(lang) },
                    label = { Text(lang.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricBlue.copy(alpha = 0.3f),
                        selectedLabelColor = Color.White,
                        containerColor = StudioSurface,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = StudioOutline,
                        selectedBorderColor = ElectricBlue
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DurationAndVideoSettingsCard(
    formState: GeneratorFormState,
    onSelectDurationMinutes: (Int) -> Unit,
    onSelectBgmTrack: (BgmTrackOption) -> Unit,
    onSelectAspectRatio: (VideoAspectRatio) -> Unit,
    onSelectResolution: (VideoResolution) -> Unit
) {
    GlassStudioCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Story Duration (1–30 Minutes)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "${formState.durationMinutes} min • ${formState.plannedSceneCount} scenes",
                style = MaterialTheme.typography.labelLarge,
                color = CyanSpark
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Maximum 30 minutes (strict limit — final video never exceeds selected minutes).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Slider(
            value = formState.durationMinutes.toFloat(),
            onValueChange = { onSelectDurationMinutes(it.toInt().coerceIn(1, 30)) },
            valueRange = 1f..30f,
            steps = 28,
            colors = SliderDefaults.colors(
                thumbColor = CyanSpark,
                activeTrackColor = ElectricViolet,
                inactiveTrackColor = StudioOutline
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("studio_duration_slider")
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(1, 2, 5, 10, 15, 20, 30).forEach { mins ->
                val selected = formState.durationMinutes == mins
                FilterChip(
                    selected = selected,
                    onClick = { onSelectDurationMinutes(mins) },
                    label = {
                        val scenes = VideoDurationOption.sceneCountForMinutes(mins)
                        Text(if (mins == 30) "30m Max ($scenes scenes)" else "${mins}m ($scenes scenes)")
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricViolet.copy(alpha = 0.28f),
                        selectedLabelColor = Color.White,
                        containerColor = StudioSurface,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = StudioOutline,
                        selectedBorderColor = ElectricViolet
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Custom Background Music",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            BgmTrackOption.entries.forEach { track ->
                val selected = formState.bgmTrack == track
                FilterChip(
                    selected = selected,
                    onClick = { onSelectBgmTrack(track) },
                    label = { Text(track.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricViolet.copy(alpha = 0.28f),
                        selectedLabelColor = Color.White,
                        containerColor = StudioSurface,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = StudioOutline,
                        selectedBorderColor = ElectricViolet
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Aspect Ratio & Resolution",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            VideoAspectRatio.entries.forEach { ratio ->
                val selected = formState.aspectRatio == ratio
                FilterChip(
                    selected = selected,
                    onClick = { onSelectAspectRatio(ratio) },
                    label = { Text(ratio.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyanSpark.copy(alpha = 0.22f),
                        selectedLabelColor = Color.White,
                        containerColor = StudioSurface,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = StudioOutline,
                        selectedBorderColor = CyanSpark
                    )
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            VideoResolution.entries.forEach { res ->
                val selected = formState.resolution == res
                FilterChip(
                    selected = selected,
                    onClick = { onSelectResolution(res) },
                    label = { Text(res.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricBlue.copy(alpha = 0.28f),
                        selectedLabelColor = Color.White,
                        containerColor = StudioSurface,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = StudioOutline,
                        selectedBorderColor = ElectricBlue
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CastConsistencyCard(
    savedCharacters: List<SavedCharacter>,
    selectedIds: Set<String>,
    onToggleCharacter: (String) -> Unit
) {
    GlassStudioCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Face, contentDescription = null, tint = CyanSpark, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Cast Saved Characters (Optional)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Selected characters keep their exact face, clothes, and voice across every scene.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            savedCharacters.forEach { ch ->
                val selected = ch.id in selectedIds
                FilterChip(
                    selected = selected,
                    onClick = { onToggleCharacter(ch.id) },
                    label = { Text("${ch.name} (${ch.voiceGender.lowercase()})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricViolet.copy(alpha = 0.35f),
                        selectedLabelColor = Color.White,
                        containerColor = StudioSurface,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

@Composable
private fun GenerateActionAndPipelineCard(
    formState: GeneratorFormState,
    pipelineState: GenerationPipelineState,
    onGenerateCartoon: () -> Unit,
    onRetryGeneration: () -> Unit,
    onCancelGeneration: () -> Unit
) {
    GlassStudioCard(highlighted = pipelineState.isRunning, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.AllInclusive,
                contentDescription = "Unlimited generation",
                tint = SuccessMint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Unlimited AI Video Studio",
                    style = MaterialTheme.typography.labelMedium,
                    color = SuccessMint
                )
                Text(
                    text = "${formState.style.label} • ${formState.durationMinutes} min (${formState.plannedSceneCount} scenes) • ${formState.resolution.label}",
                    style = MaterialTheme.typography.titleSmall,
                    color = CyanSpark
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (!pipelineState.isRunning) {
            GradientPrimaryButton(
                text = stringResource(R.string.btn_generate_cartoon),
                onClick = onGenerateCartoon,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("generate_cartoon_button")
            )
        } else {
            OutlinedButton(
                onClick = onCancelGeneration,
                border = BorderStroke(1.dp, ErrorCoral),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorCoral),
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .height(50.dp)
            ) {
                Icon(Icons.Filled.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Cancel Generation")
            }
        }

        AnimatedVisibility(visible = pipelineState.phase != PipelinePhase.IDLE) {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (pipelineState.isRunning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.2.dp,
                                color = CyanSpark
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = pipelineState.phase.label,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = when (pipelineState.phase) {
                                PipelinePhase.COMPLETED -> SuccessMint
                                PipelinePhase.FAILED -> ErrorCoral
                                else -> CyanSpark
                            }
                        )
                    }
                    Text(
                        text = "${pipelineState.progressPercent}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (pipelineState.progressPercent / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50)),
                    color = when (pipelineState.phase) {
                        PipelinePhase.COMPLETED -> SuccessMint
                        PipelinePhase.FAILED -> ErrorCoral
                        else -> ElectricViolet
                    },
                    trackColor = StudioSurface
                )

                if (pipelineState.statusDetail.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = pipelineState.statusDetail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!pipelineState.errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = Icons.Filled.ErrorOutline,
                                    contentDescription = "Error",
                                    tint = ErrorCoral,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = pipelineState.errorMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = onRetryGeneration,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ErrorCoral,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .testTag("retry_generation_button")
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.btn_resume_generation))
                            }
                        }
                    }
                }

                if (pipelineState.sceneJobs.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Scene Queue",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    pipelineState.sceneJobs.forEach { job ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            when (job.status) {
                                SceneStatus.COMPLETED -> Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = "Done",
                                    tint = SuccessMint,
                                    modifier = Modifier.size(16.dp)
                                )
                                SceneStatus.GENERATING -> CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = CyanSpark
                                )
                                SceneStatus.FAILED -> Icon(
                                    Icons.Filled.ErrorOutline,
                                    contentDescription = "Failed",
                                    tint = ErrorCoral,
                                    modifier = Modifier.size(16.dp)
                                )
                                SceneStatus.PENDING -> Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(StudioOutline)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Scene ${job.sceneIndex + 1}: ${job.title}" +
                                    if (job.attempt > 1) " (retry #${job.attempt - 1})" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
