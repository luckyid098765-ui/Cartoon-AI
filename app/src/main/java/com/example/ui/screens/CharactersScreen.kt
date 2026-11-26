package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CartoonStyle
import com.example.data.model.CharacterExpression
import com.example.data.model.SavedCharacter
import com.example.data.model.VoiceGender
import com.example.ui.components.Base64OrPathImage
import com.example.ui.components.GlassStudioCard
import com.example.ui.theme.CyanSpark
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ErrorCoral
import com.example.ui.theme.StudioSurfaceCard

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CharactersScreen(
    characters: List<SavedCharacter>,
    isBusy: Boolean,
    onCreateCharacter: (
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
    ) -> Unit,
    onDeleteCharacter: (String) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 92.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "Consistent Character Studio",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Create reusable characters whose faces, outfits, expressions, and voices stay identical in every scene.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (characters.isEmpty()) {
                item {
                    GlassStudioCard(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Face,
                                contentDescription = null,
                                tint = ElectricViolet,
                                modifier = Modifier.size(38.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "No saved characters yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "Tap + Create Character to design your hero, sidekick, or narrator with AI portrait & voice assignment.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                items(characters, key = { it.id }) { character ->
                    GlassStudioCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Base64OrPathImage(
                                localPath = character.localPortraitPath,
                                base64 = character.portraitBase64,
                                contentDescription = "${character.name} portrait",
                                modifier = Modifier
                                    .size(84.dp)
                                    .clip(RoundedCornerShape(16.dp))
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = character.name,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    IconButton(
                                        onClick = { onDeleteCharacter(character.id) },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.DeleteOutline,
                                            contentDescription = "Delete ${character.name}",
                                            tint = ErrorCoral
                                        )
                                    }
                                }
                                Text(
                                    text = "${character.role} • ${CartoonStyle.fromKey(character.style).label}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = CyanSpark
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Look: ${character.appearance}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Outfit: ${character.outfit}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    CharacterBadge(
                                        icon = Icons.Filled.RecordVoiceOver,
                                        text = "${VoiceGender.fromKey(character.voiceGender).label} (${String.format("%.2f", character.voicePitch)}x)"
                                    )
                                    CharacterBadge(
                                        icon = Icons.Filled.Face,
                                        text = CharacterExpression.fromKey(character.defaultExpression).label
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showCreateDialog = true },
            containerColor = ElectricViolet,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add_character_fab")
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Create character")
        }
    }

    if (showCreateDialog) {
        var name by remember { mutableStateOf("") }
        var role by remember { mutableStateOf("Hero") }
        var appearance by remember { mutableStateOf("") }
        var outfit by remember { mutableStateOf("") }
        var personality by remember { mutableStateOf("") }
        var voiceGender by remember { mutableStateOf(VoiceGender.MALE) }
        var voicePitch by remember { mutableFloatStateOf(1.0f) }
        var expression by remember { mutableStateOf(CharacterExpression.HAPPY) }
        var style by remember { mutableStateOf(CartoonStyle.CARTOON_3D) }
        var generateAiPortrait by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { if (!isBusy) showCreateDialog = false },
            title = { Text("Create Consistent Character") },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Character Name *") },
                            placeholder = { Text("e.g., Bolt, Tara, Chintu") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = role,
                            onValueChange = { role = it },
                            label = { Text("Role") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = appearance,
                            onValueChange = { appearance = it },
                            label = { Text("Face, Hair & Visual Traits *") },
                            placeholder = { Text("Small orange robot fox, glowing cyan eyes, pointed ears") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = outfit,
                            onValueChange = { outfit = it },
                            label = { Text("Consistent Outfit *") },
                            placeholder = { Text("Cobalt blue scarf, silver jetpack, red sneakers") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = personality,
                            onValueChange = { personality = it },
                            label = { Text("Personality & Body Movement") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Text("Voice Gender", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            VoiceGender.entries.forEach { g ->
                                FilterChip(
                                    selected = voiceGender == g,
                                    onClick = {
                                        voiceGender = g
                                        voicePitch = if (g == VoiceGender.FEMALE) 1.20f else 0.92f
                                    },
                                    label = { Text(g.label) }
                                )
                            }
                        }
                    }
                    item {
                        Text("Voice Pitch: ${String.format("%.2f", voicePitch)}x", style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = voicePitch,
                            onValueChange = { voicePitch = it },
                            valueRange = 0.7f..1.4f
                        )
                    }
                    item {
                        Text("Default Expression", style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            CharacterExpression.entries.forEach { exp ->
                                FilterChip(
                                    selected = expression == exp,
                                    onClick = { expression = exp },
                                    label = { Text(exp.label) }
                                )
                            }
                        }
                    }
                    item {
                        Text("Art Style", style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            CartoonStyle.entries.forEach { st ->
                                FilterChip(
                                    selected = style == st,
                                    onClick = { style = st },
                                    label = { Text(st.label) }
                                )
                            }
                        }
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Generate AI Portrait Sheet", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = generateAiPortrait,
                                onCheckedChange = { generateAiPortrait = it }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreateCharacter(
                            name, role, appearance, outfit, personality,
                            voiceGender, voicePitch, expression, style, generateAiPortrait
                        ) {
                            showCreateDialog = false
                        }
                    },
                    enabled = !isBusy && name.isNotBlank() && appearance.isNotBlank() && outfit.isNotBlank()
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Generating...")
                    } else {
                        Text("Save Character")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }, enabled = !isBusy) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun CharacterBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Surface(
        color = StudioSurfaceCard,
        border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(50)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground)
        }
    }
}
