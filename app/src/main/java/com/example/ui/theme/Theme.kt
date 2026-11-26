package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val CartoonAiDarkColorScheme = darkColorScheme(
    primary = ElectricViolet,
    onPrimary = Color(0xFF11042E),
    primaryContainer = VioletContainer,
    onPrimaryContainer = OnVioletContainer,
    secondary = ElectricBlue,
    onSecondary = Color(0xFF031733),
    secondaryContainer = BlueContainer,
    onSecondaryContainer = OnBlueContainer,
    tertiary = CyanSpark,
    onTertiary = Color(0xFF042029),
    tertiaryContainer = CyanContainer,
    onTertiaryContainer = Color(0xFFCFFAFE),
    background = StudioVoid,
    onBackground = StudioTextPrimary,
    surface = StudioDeepBg,
    onSurface = StudioTextPrimary,
    surfaceVariant = StudioSurfaceElevated,
    onSurfaceVariant = StudioTextSecondary,
    surfaceContainerLowest = StudioVoid,
    surfaceContainerLow = StudioDeepBg,
    surfaceContainer = StudioSurface,
    surfaceContainerHigh = StudioSurfaceElevated,
    surfaceContainerHighest = StudioSurfaceCard,
    outline = StudioOutline,
    outlineVariant = StudioOutlineSubtle,
    error = ErrorCoral,
    onError = Color(0xFF2B0410),
    errorContainer = ErrorContainerDark,
    onErrorContainer = Color(0xFFFFD9E0)
)

val CartoonAiShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun CartoonAiTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CartoonAiDarkColorScheme,
        typography = CartoonAiTypography,
        shapes = CartoonAiShapes,
        content = content
    )
}
