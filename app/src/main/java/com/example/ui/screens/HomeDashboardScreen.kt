package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CartoonProject
import com.example.data.model.CartoonStyle
import com.example.data.model.SavedCharacter
import com.example.data.model.UserProfile
import com.example.ui.components.Base64OrPathImage
import com.example.ui.components.GlassStudioCard
import com.example.ui.components.GradientPrimaryButton
import com.example.ui.theme.CyanSpark
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.StudioSurfaceCard
import com.example.ui.theme.SuccessMint
import com.example.ui.theme.WarningAmber

data class QuickStoryPreset(
    val title: String,
    val style: CartoonStyle,
    val prompt: String
)

private val quickPresets = listOf(
    QuickStoryPreset(
        title = "Sky Island Express",
        style = CartoonStyle.CARTOON_3D,
        prompt = "A cheerful robot fox named Bolt and a wise owl named Luna pilot a glowing sky-train across floating crystal islands to return a lost star before sunrise."
    ),
    QuickStoryPreset(
        title = "Chintu's Space Samosas",
        style = CartoonStyle.FUNNY,
        prompt = "Chintu, a witty monkey in a blue vest, accidentally launches his grandmother's flying food cart to Mars, winning over grumpy purple aliens with crispy samosas."
    ),
    QuickStoryPreset(
        title = "Neon Ronin Academy",
        style = CartoonStyle.ANIME,
        prompt = "Two young anime guardians armed with harmonic light-blades team up in a rain-soaked cyber shrine to awaken the sleeping thunder dragon."
    ),
    QuickStoryPreset(
        title = "Enchanted Forest",
        style = CartoonStyle.FANTASY,
        prompt = "A tiny firefly knight and a talking emerald turtle explore a bioluminescent mushroom forest to heal the ancient Whispering Tree."
    )
)

@Composable
fun HomeDashboardScreen(
    userProfile: UserProfile?,
    projects: List<CartoonProject>,
    characters: List<SavedCharacter>,
    onCreateNewCartoon: () -> Unit,
    onLaunchPreset: (QuickStoryPreset) -> Unit,
    onOpenProject: (CartoonProject) -> Unit,
    onOpenCharacters: () -> Unit,
    onOpenProjects: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 720.dp

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Hero Studio Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .border(
                            1.dp,
                            Brush.linearGradient(listOf(ElectricViolet, ElectricBlue, CyanSpark)),
                            RoundedCornerShape(24.dp)
                        )
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_studio_hero),
                        contentDescription = "Cartoon AI hero studio",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .matchParentSize()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xE60B081C),
                                        Color(0xC4131033),
                                        Color(0x66101B44)
                                    )
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.img_cartoon_ai_logo),
                                contentDescription = "3D Cartoon AI icon",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(1.5.dp, CyanSpark, RoundedCornerShape(16.dp))
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Welcome back, ${userProfile?.displayName ?: "Director"}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = CyanSpark
                                )
                                Text(
                                    text = "Direct Your Next AI Cartoon Movie",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Paste a story, pick a cartoon style & language (English, Hindi, Bengali), and let CARTOON AI generate scenes, consistent characters, lip-synced voices, and a full MP4 film.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFDCD6F7)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GradientPrimaryButton(
                                text = "Create Cartoon Movie",
                                onClick = onCreateNewCartoon,
                                modifier = Modifier.testTag("home_create_cartoon_btn")
                            )
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable(onClick = onOpenCharacters),
                                color = StudioSurfaceCard.copy(alpha = 0.85f),
                                border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Face,
                                        contentDescription = null,
                                        tint = ElectricBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Cast (${characters.size})",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Dashboard Metrics Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashboardStatCard(
                        icon = Icons.Filled.Movie,
                        label = "Movies",
                        value = "${projects.size}",
                        accent = ElectricViolet,
                        modifier = Modifier.weight(1f)
                    )
                    DashboardStatCard(
                        icon = Icons.Filled.VideoLibrary,
                        label = "Scenes",
                        value = "${userProfile?.totalScenesRendered ?: projects.sumOf { it.completedScenes }}",
                        accent = ElectricBlue,
                        modifier = Modifier.weight(1f)
                    )
                    DashboardStatCard(
                        icon = Icons.Filled.RecordVoiceOver,
                        label = "Credits",
                        value = "${userProfile?.creditsRemaining ?: 50}",
                        accent = WarningAmber,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Quick-Start Story Templates
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "One-Tap Story Starters",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Beginner friendly",
                            style = MaterialTheme.typography.labelMedium,
                            color = CyanSpark
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    if (isWide) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            quickPresets.forEach { preset ->
                                QuickPresetCard(
                                    preset = preset,
                                    onClick = { onLaunchPreset(preset) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(quickPresets) { preset ->
                                QuickPresetCard(
                                    preset = preset,
                                    onClick = { onLaunchPreset(preset) },
                                    modifier = Modifier.width(250.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 8 Cartoon Styles Showcase
            item {
                Column {
                    Text(
                        text = "8 Signature Animation Styles",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(CartoonStyle.entries) { style ->
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        onLaunchPreset(
                                            QuickStoryPreset(
                                                title = style.label,
                                                style = style,
                                                prompt = ""
                                            )
                                        )
                                    },
                                color = StudioSurfaceCard,
                                border = BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.35f)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .width(165.dp)
                                        .padding(14.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesome,
                                        contentDescription = null,
                                        tint = ElectricViolet,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = style.label,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = style.tagline,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Recent Projects
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Cartoon Movies",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (projects.isNotEmpty()) {
                        TextButton(onClick = onOpenProjects) {
                            Text("View All", color = ElectricBlue)
                        }
                    }
                }
            }

            if (projects.isEmpty()) {
                item {
                    GlassStudioCard(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.PlayCircle,
                                contentDescription = null,
                                tint = ElectricBlue,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "No cartoons generated yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "Tap Create Cartoon Movie above or pick a One-Tap Story Starter to direct your first film.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                items(projects.take(4), key = { it.id }) { project ->
                    GlassStudioCard(
                        onClick = { onOpenProject(project) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Base64OrPathImage(
                                localPath = null,
                                base64 = project.thumbnailBase64,
                                contentDescription = project.title,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = project.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${CartoonStyle.fromKey(project.style).label} • ${project.sceneCount} scenes • ${project.durationSeconds}s",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = project.status,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (project.status == "COMPLETED") SuccessMint else ElectricBlue
                                )
                            }
                            Icon(
                                imageVector = Icons.Filled.PlayCircle,
                                contentDescription = "Open video player",
                                tint = ElectricViolet,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun DashboardStatCard(
    icon: ImageVector,
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    GlassStudioCard(modifier = modifier) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun QuickPresetCard(
    preset: QuickStoryPreset,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassStudioCard(onClick = onClick, modifier = modifier) {
        Text(
            text = preset.style.label,
            style = MaterialTheme.typography.labelSmall,
            color = CyanSpark
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = preset.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = preset.prompt,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}
