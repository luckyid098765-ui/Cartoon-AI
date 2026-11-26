package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CartoonLanguage
import com.example.data.model.CartoonStyle
import com.example.data.model.GenerationPipelineState
import com.example.data.model.PipelinePhase
import com.example.data.model.SavedCharacter
import com.example.data.model.SceneStatus
import com.example.data.model.VideoAspectRatio
import com.example.data.model.VideoDurationOption
import com.example.data.model.VideoResolution
import com.example.ui.components.GlassStudioCard
import com.example.ui.components.GradientPrimaryButton
import com.example.ui.theme.CyanSpark
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ErrorCoral
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceCard
import com.example.ui.theme.SuccessMint
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.GeneratorFormState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GeneratorStudioScreen(
    formState: GeneratorFormState,
    pipelineState: GenerationPipelineState,
    savedCharacters: List<SavedCharacter>,
    onPromptChange: (String) -> Unit,
    onClearPrompt: () -> Unit,
    onRandomStory: () -> Unit,
    onGenerateStory: () -> Unit,
    onSelectStyle: (CartoonStyle) -> Unit,
    onSelectDuration: (VideoDurationOption) -> Unit,
    onSelectLanguage: (CartoonLanguage) -> Unit,
    onSelectAspectRatio: (VideoAspectRatio) -> Unit,
    onSelectResolution: (VideoResolution) -> Unit,
    onToggleCharacter: (String) -> Unit,
    onGenerateCartoon: () -> Unit,
    onCancelGeneration: () -> Unit,
    onOpenVideoPlayer: (String) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 860.dp

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "AI Cartoon Generator",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Write or generate a story, choose style, duration, language & video format.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                                onGenerateStory = onGenerateStory
                            )
                            StyleAndLanguageCard(
                                formState = formState,
                                onSelectStyle = onSelectStyle,
                                onSelectLanguage = onSelectLanguage
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            DurationAndVideoSettingsCard(
                                formState = formState,
                                onSelectDuration = onSelectDuration,
                                onSelectAspectRatio = onSelectAspectRatio,
                                onSelectResolution = onSelectResolution
                            )
                            CastConsistencyCard(
                                savedCharacters = savedCharacters,
                                selectedIds = formState.selectedCharacterIds,
                                onToggleCharacter = onToggleCharacter
                            )
                            GenerateActionAndPipelineCard(
                                formState = formState,
                                pipelineState = pipelineState,
                                onGenerateCartoon = onGenerateCartoon,
                                onCancelGeneration = onCancelGeneration,
                                onOpenVideoPlayer = onOpenVideoPlayer
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
                        onGenerateStory = onGenerateStory
                    )
                }
                item {
                    StyleAndLanguageCard(
                        formState = formState,
                        onSelectStyle = onSelectStyle,
                        onSelectLanguage = onSelectLanguage
                    )
                }
                item {
                    DurationAndVideoSettingsCard(
                        formState = formState,
                        onSelectDuration = onSelectDuration,
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
                item {
                    GenerateActionAndPipelineCard(
                        formState = formState,
                        pipelineState = pipelineState,
                        onGenerateCartoon = onGenerateCartoon,
                        onCancelGeneration = onCancelGeneration,
                        onOpenVideoPlayer = onOpenVideoPlayer
                    )
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
    onGenerateStory: () -> Unit
) {
    GlassStudioCard(highlighted = true, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Story Prompt",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = formState.prompt,
            onValueChange = onPromptChange,
            placeholder = {
                Text(
                    text = stringResource(R.string.prompt_placeholder),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
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
                onClick = onGenerateStory,
                enabled = !formState.isGeneratingStoryIdea,
                border = BorderStroke(1.dp, ElectricViolet),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricViolet),
                modifier = Modifier.testTag("generate_story_button")
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
                modifier = Modifier.testTag("random_story_button")
            ) {
                Icon(Icons.Filled.Casino, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.btn_random_story))
            }

            OutlinedButton(
                onClick = onClearPrompt,
                enabled = formState.prompt.isNotEmpty(),
                border = BorderStroke(1.dp, StudioOutline),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier.testTag("clear_prompt_button")
            ) {
                Icon(Icons.Filled.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.btn_clear_prompt))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StyleAndLanguageCard(
    formState: GeneratorFormState,
    onSelectStyle: (CartoonStyle) -> Unit,
    onSelectLanguage: (CartoonLanguage) -> Unit
) {
    GlassStudioCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Cartoon Style",
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
    onSelectDuration: (VideoDurationOption) -> Unit,
    onSelectAspectRatio: (VideoAspectRatio) -> Unit,
    onSelectResolution: (VideoResolution) -> Unit
) {
    GlassStudioCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Video Duration & Connected Scenes",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            VideoDurationOption.entries.forEach { dur ->
                val selected = formState.duration == dur
                FilterChip(
                    selected = selected,
                    onClick = { onSelectDuration(dur) },
                    label = { Text("${dur.label} (${dur.sceneCount} scenes)") },
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
        if (savedCharacters.isEmpty()) {
            Text(
                text = "No saved characters yet — AI will design consistent characters automatically, or create yours in the Characters tab.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
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
}

@Composable
private fun GenerateActionAndPipelineCard(
    formState: GeneratorFormState,
    pipelineState: GenerationPipelineState,
    onGenerateCartoon: () -> Unit,
    onCancelGeneration: () -> Unit,
    onOpenVideoPlayer: (String) -> Unit
) {
    GlassStudioCard(highlighted = pipelineState.isRunning, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Production Cost",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${formState.duration.creditCost} credits • ${formState.duration.sceneCount} AI scenes • ${formState.resolution.label}",
                    style = MaterialTheme.typography.titleSmall,
                    color = WarningAmber
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
                    .testTag("generate_cartoon_button")
            )
        } else {
            OutlinedButton(
                onClick = onCancelGeneration,
                border = BorderStroke(1.dp, ErrorCoral),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorCoral),
                modifier = Modifier.fillMaxWidth()
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
                    Text(
                        text = "${pipelineState.progressPercent}%",
                        style = MaterialTheme.typography.labelSmall,
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
                    color = if (pipelineState.phase == PipelinePhase.FAILED) ErrorCoral else ElectricViolet,
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
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
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

                if (pipelineState.phase == PipelinePhase.COMPLETED && !pipelineState.activeProjectId.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    GradientPrimaryButton(
                        text = "Watch Cartoon Movie",
                        onClick = { onOpenVideoPlayer(pipelineState.activeProjectId) },
                        icon = {
                            Icon(
                                Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("watch_generated_movie_button")
                    )
                }
            }
        }
    }
}
