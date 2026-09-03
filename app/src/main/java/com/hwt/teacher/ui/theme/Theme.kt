package com.hwt.teacher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme

private val LightColors = lightColorScheme(
    primary = MdPrimary,
    onPrimary = MdOnPrimary,
    primaryContainer = MdPrimaryContainer,
    onPrimaryContainer = MdOnPrimaryContainer,
    secondaryContainer = MdSecondaryContainer,
    onSecondaryContainer = MdOnSecondaryContainer,
    surface = MdSurface,
    onSurface = MdOnSurface,
    onSurfaceVariant = MdOnSurfaceVariant,
    outline = MdOutline,
    outlineVariant = MdOutlineVariant,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = MdSurfaceContainerLow,
    surfaceContainer = MdSurfaceContainer,
    surfaceContainerHigh = MdSurfaceContainerHigh,
    surfaceContainerHighest = MdSurfaceContainerHigh,
    background = MdSurface,
    onBackground = MdOnSurface
)

private val DarkColors = darkColorScheme(
    primary = DkPrimary,
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4A4458),
    onPrimaryContainer = MdPrimaryContainer,
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = MdSecondaryContainer,
    surface = DkSurface,
    onSurface = DkOnSurface,
    onSurfaceVariant = DkOnSurfaceVariant,
    outline = DkOutline,
    outlineVariant = Color(0xFF49454F),
    surfaceContainerLowest = Color(0xFF0F0D13),
    surfaceContainerLow = Color(0xFF1D1B20),
    surfaceContainer = DkSurfaceContainer,
    surfaceContainerHigh = Color(0xFF2B2930),
    surfaceContainerHighest = Color(0xFF36343B),
    background = DkSurface,
    onBackground = DkOnSurface
)

@Composable
fun HomeworkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography(),
        content = content
    )
}
