package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.Base64
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.CartoonLanguage
import com.example.data.model.CartoonProject
import com.example.data.model.CartoonStyle
import com.example.data.model.GenerationHistoryEntry
import com.example.data.model.ProjectStatus
import com.example.data.model.SavedCharacter
import com.example.data.model.UserProfile
import com.example.ui.components.GlassStudioCard
import com.example.ui.theme.CyanSpark
import com.example.ui.theme.DeepViolet
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ErrorCoral
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceCard
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.SuccessMint
import com.example.ui.theme.WarningAmber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ProjectsAndAccountScreen(
    userProfile: UserProfile?,
    projects: List<CartoonProject>,
    characters: List<SavedCharacter>,
    history: List<GenerationHistoryEntry>,
    onOpenProject: (CartoonProject) -> Unit,
    onDeleteProject: (String) -> Unit,
    onRefillCredits: () -> Unit,
    onSignOut: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var isGalleryView by remember { mutableStateOf(true) }
    var selectedStyleFilter by remember { mutableStateOf<String?>(null) }
    var projectPendingDelete by remember { mutableStateOf<CartoonProject?>(null) }

    val availableStyleKeys = remember(projects) {
        projects.map { it.style }.distinct()
    }
    val filteredProjects = remember(projects, selectedStyleFilter) {
        if (selectedStyleFilter == null) {
            projects
        } else {
            projects.filter { it.style == selectedStyleFilter }
        }
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        val galleryColumns = when {
            maxWidth >= 900.dp -> 4
            maxWidth >= 560.dp -> 3
            else -> 2
        }
        val projectRows = remember(filteredProjects, galleryColumns) {
            filteredProjects.chunked(galleryColumns)
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 1040.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // User Profile & Credits Card
            item {
                GlassStudioCard(highlighted = true, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val userPhotoUrl = userProfile?.photoUrl
                        val userEmail = userProfile?.email
                        if (!userPhotoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = userPhotoUrl,
                                contentDescription = "Profile avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(58.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, CyanSpark, CircleShape)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = "Profile avatar",
                                tint = CyanSpark,
                                modifier = Modifier
                                    .size(58.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, CyanSpark, CircleShape)
                                    .padding(12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = userProfile?.displayName ?: "Cartoon Creator",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!userEmail.isNullOrBlank()) {
                                Text(
                                    text = userEmail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${projects.size} projects • ${characters.size} characters • ${userProfile?.totalVideosGenerated ?: 0} movies generated",
                                style = MaterialTheme.typography.labelMedium,
                                color = ElectricBlue
                            )
                        }

                        IconButton(
                            onClick = onSignOut,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("sign_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Sign out",
                                tint = ErrorCoral
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Bolt,
                                contentDescription = null,
                                tint = WarningAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${userProfile?.creditsRemaining ?: 50} Generation Credits Available",
                                style = MaterialTheme.typography.titleSmall,
                                color = WarningAmber
                            )
                        }

                        if ((userProfile?.creditsRemaining ?: 50L) < 10L) {
                            OutlinedButton(
                                onClick = onRefillCredits,
                                border = BorderStroke(1.dp, WarningAmber),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = WarningAmber)
                            ) {
                                Text("Refill 50")
                            }
                        }
                    }
                }
            }

            // Projects vs History Switcher
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = StudioSurface,
                    contentColor = ElectricViolet,
                    modifier = Modifier.clip(RoundedCornerShape(14.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("My Projects (${projects.size})") },
                        icon = { Icon(Icons.Filled.Movie, contentDescription = null) },
                        modifier = Modifier.testTag("tab_my_projects")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("History (${history.size})") },
                        icon = { Icon(Icons.Filled.History, contentDescription = null) },
                        modifier = Modifier.testTag("tab_history")
                    )
                }
            }

            if (selectedTab == 0) {
                if (projects.isEmpty()) {
                    item {
                        GlassStudioCard(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Movie,
                                    contentDescription = null,
                                    tint = ElectricViolet,
                                    modifier = Modifier.size(34.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "No saved cartoons in your gallery yet",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Create your first AI cartoon movie in Studio to see visual thumbnail previews and export MP4s here.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Gallery Header + View Mode Toggle (Gallery / List)
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = stringResource(R.string.projects_gallery_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = "${filteredProjects.size} ${if (filteredProjects.size == 1) "cartoon" else "cartoons"} ready to preview",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CyanSpark
                                    )
                                }

                                // Gallery vs List view switcher
                                Surface(
                                    color = StudioSurface,
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, StudioOutline.copy(alpha = 0.6f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        ViewModeChip(
                                            selected = isGalleryView,
                                            icon = Icons.Filled.GridView,
                                            contentDescription = stringResource(R.string.view_mode_gallery),
                                            onClick = { isGalleryView = true },
                                            modifier = Modifier.testTag("projects_gallery_toggle")
                                        )
                                        ViewModeChip(
                                            selected = !isGalleryView,
                                            icon = Icons.AutoMirrored.Filled.ViewList,
                                            contentDescription = stringResource(R.string.view_mode_list),
                                            onClick = { isGalleryView = false },
                                            modifier = Modifier.testTag("projects_list_toggle")
                                        )
                                    }
                                }
                            }

                            if (availableStyleKeys.size > 1) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    item {
                                        FilterChip(
                                            selected = selectedStyleFilter == null,
                                            onClick = { selectedStyleFilter = null },
                                            label = { Text("All (${projects.size})") },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = ElectricViolet.copy(alpha = 0.3f),
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                    }
                                    items(availableStyleKeys, key = { it }) { styleKey ->
                                        val styleLabel = CartoonStyle.fromKey(styleKey).label
                                        FilterChip(
                                            selected = selectedStyleFilter == styleKey,
                                            onClick = {
                                                selectedStyleFilter = if (selectedStyleFilter == styleKey) null else styleKey
                                            },
                                            label = { Text(styleLabel) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = ElectricViolet.copy(alpha = 0.3f),
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (isGalleryView) {
                        // Responsive Thumbnail Gallery Grid inside LazyColumn
                        items(
                            items = projectRows,
                            key = { row -> row.joinToString("_") { it.id } }
                        ) { rowProjects ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("projects_gallery_row"),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowProjects.forEach { project ->
                                    ProjectGalleryCard(
                                        project = project,
                                        onClick = { onOpenProject(project) },
                                        onRequestDelete = { projectPendingDelete = project },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                repeat(galleryColumns - rowProjects.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    } else {
                        // Compact List View with Preview Thumbnails
                        items(filteredProjects, key = { it.id }) { project ->
                            GlassStudioCard(
                                onClick = { onOpenProject(project) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("project_list_card_${project.id}")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    ProjectPreviewThumbnail(
                                        project = project,
                                        modifier = Modifier
                                            .size(86.dp, 64.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = project.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${CartoonStyle.fromKey(project.style).label} • ${CartoonLanguage.fromKey(project.language).label}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${project.completedScenes}/${project.sceneCount} scenes • ${formatDurationCompact(project.durationSeconds)} • ${project.status}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (project.status == ProjectStatus.COMPLETED.key) SuccessMint else ElectricBlue
                                        )
                                    }
                                    IconButton(
                                        onClick = { onOpenProject(project) },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Icon(Icons.Filled.PlayCircle, contentDescription = "Open player", tint = ElectricViolet)
                                    }
                                    IconButton(
                                        onClick = { projectPendingDelete = project },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete project", tint = ErrorCoral)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                if (history.isEmpty()) {
                    item {
                        GlassStudioCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "No generation history yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Every cartoon generation and scene re-render appears here with credit usage.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(history, key = { it.id }) { entry ->
                        GlassStudioCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = entry.projectTitle,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = "${entry.actionType} • ${entry.style} • ${entry.language} • ${entry.durationLabel}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (!entry.details.isNullOrBlank()) {
                                        Text(
                                            text = entry.details,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CyanSpark
                                        )
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = entry.status,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (entry.status == "COMPLETED") SuccessMint else ErrorCoral
                                    )
                                    Text(
                                        text = "-${entry.creditsUsed} cr",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = WarningAmber
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    val pendingDelete = projectPendingDelete
    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = { projectPendingDelete = null },
            title = { Text("Delete Cartoon Project?") },
            text = {
                Text("Remove \"${pendingDelete.title}\" and its saved scenes from My Projects?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteProject(pendingDelete.id)
                        projectPendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ErrorCoral,
                        contentColor = Color.White
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { projectPendingDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ViewModeChip(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) ElectricViolet.copy(alpha = 0.28f) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (selected) CyanSpark else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ProjectGalleryCard(
    project: CartoonProject,
    onClick: () -> Unit,
    onRequestDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCompleted = project.status == ProjectStatus.COMPLETED.key
    val style = CartoonStyle.fromKey(project.style)
    val shape = RoundedCornerShape(18.dp)

    Surface(
        modifier = modifier
            .clip(shape)
            .border(
                BorderStroke(
                    1.dp,
                    Brush.linearGradient(
                        if (isCompleted) {
                            listOf(ElectricViolet.copy(alpha = 0.7f), ElectricBlue.copy(alpha = 0.5f))
                        } else {
                            listOf(StudioOutline.copy(alpha = 0.7f), StudioOutline.copy(alpha = 0.3f))
                        }
                    )
                ),
                shape
            )
            .clickable(onClick = onClick)
            .testTag("project_gallery_card_${project.id}"),
        color = StudioSurfaceElevated.copy(alpha = 0.92f),
        tonalElevation = 4.dp,
        shape = shape
    ) {
        Column {
            // Visual Preview Thumbnail Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
            ) {
                ProjectPreviewThumbnail(
                    project = project,
                    modifier = Modifier.fillMaxSize()
                )

                // Bottom contrast gradient scrim
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x5505040D),
                                    Color.Transparent,
                                    Color(0xCC060512)
                                )
                            )
                        )
                )

                // Style pill (top-left)
                Surface(
                    color = Color(0xB3090717),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = style.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanSpark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                // Quick delete button (top-right, 48dp accessible touch area)
                IconButton(
                    onClick = onRequestDelete,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .minimumInteractiveComponentSize()
                        .testTag("delete_project_${project.id}")
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0x99090717)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Delete ${project.title}",
                            tint = ErrorCoral,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Centered play badge overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0x99120D2A))
                        .border(1.dp, Color.White.copy(alpha = 0.45f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Play ${project.title}",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Bottom status + duration row inside thumbnail
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color(0xB3090717),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isCompleted) "MP4 Ready" else "${project.progressPercent}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isCompleted) SuccessMint else WarningAmber,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        color = Color(0xCC090717),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = formatDurationCompact(project.durationSeconds),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Card Metadata Footer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 10.dp)
            ) {
                Text(
                    text = project.title.ifBlank { "Untitled Cartoon" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${CartoonLanguage.fromKey(project.language).label} • ${project.completedScenes}/${project.sceneCount} scenes",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ProjectPreviewThumbnail(
    project: CartoonProject,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(
        initialValue = null,
        key1 = project.id,
        key2 = project.thumbnailBase64,
        key3 = project.localVideoPath
    ) {
        value = withContext(Dispatchers.IO) {
            // 1. Decode synced Firestore thumbnail Base64
            project.thumbnailBase64?.takeIf { it.isNotBlank() }?.let { b64 ->
                runCatching {
                    val bytes = Base64.decode(b64, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }.getOrNull()
            }
                // 2. Fallback to first generated scene frame on disk
                ?: File(context.filesDir, "frames/${project.id}/scene_0.jpg")
                    .takeIf { it.exists() && it.length() > 0L }
                    ?.let { BitmapFactory.decodeFile(it.absolutePath) }
                // 3. Fallback to extracting first video frame from the rendered MP4
                ?: project.localVideoPath
                    ?.let { File(it) }
                    ?.takeIf { it.exists() && it.length() > 1024L }
                    ?.let { videoFile ->
                        val retriever = MediaMetadataRetriever()
                        runCatching {
                            retriever.setDataSource(videoFile.absolutePath)
                            retriever.getFrameAtTime(500_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                                ?: retriever.frameAtTime
                        }.getOrNull().also {
                            runCatching { retriever.release() }
                        }
                    }
        }
    }

    val loadedBitmap = bitmap
    if (loadedBitmap != null) {
        Image(
            bitmap = loadedBitmap.asImageBitmap(),
            contentDescription = "${project.title} preview thumbnail",
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier.background(
                Brush.linearGradient(
                    listOf(DeepViolet, StudioSurfaceCard, RoyalBlue)
                )
            ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Movie,
                contentDescription = "${project.title} preview placeholder",
                tint = CyanSpark.copy(alpha = 0.75f),
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

private fun formatDurationCompact(seconds: Long): String {
    val safeSec = seconds.coerceAtLeast(0L)
    val min = safeSec / 60L
    val sec = safeSec % 60L
    return "%d:%02d".format(min, sec)
}
