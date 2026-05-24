package com.example.chat.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = ChatAIAccent,
    onPrimary = ChatAIBackground,
    primaryContainer = ChatAIAccent,
    onPrimaryContainer = ChatAIBackground,
    secondary = ChatAITextSecondary,
    onSecondary = ChatAIBackground,
    background = ChatAIBackground,
    onBackground = ChatAITextPrimary,
    surface = ChatAIBackground,
    onSurface = ChatAITextPrimary,
    surfaceVariant = ChatAISidebar,
    onSurfaceVariant = ChatAITextSecondary,
    outline = ChatAIBorder,
    outlineVariant = ChatAIBorder,
)

private val DarkColorScheme = darkColorScheme(
    primary = ChatAIAccent,
    onPrimary = ChatAIDarkBackground,
    primaryContainer = ChatAIAccent,
    onPrimaryContainer = ChatAIDarkBackground,
    secondary = ChatAIDarkTextSecondary,
    onSecondary = ChatAIDarkBackground,
    background = ChatAIDarkBackground,
    onBackground = ChatAIDarkTextPrimary,
    surface = ChatAIDarkBackground,
    onSurface = ChatAIDarkTextPrimary,
    surfaceVariant = ChatAIDarkSidebar,
    onSurfaceVariant = ChatAIDarkTextSecondary,
    outline = ChatAIDarkBorder,
    outlineVariant = ChatAIDarkBorder,
)

@Composable
fun ChatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
