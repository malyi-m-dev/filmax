package com.filmax.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Composable
fun FilmaxTheme(
    accentColor: Color = Color(0xFFB4305A),
    content: @Composable () -> Unit,
) {
    val preset = AccentPresets[accentColor] ?: AccentPresets.values.first()
    val colorScheme = FilmaxDarkColorScheme.copy(
        primary              = preset.primary,
        onPrimary            = preset.onPrimary,
        primaryContainer     = preset.primaryContainer,
        onPrimaryContainer   = preset.onPrimaryContainer,
    )
    MaterialTheme(
        colorScheme = colorScheme,
        typography  = FilmaxTypography,
        shapes      = FilmaxShapes,
        content     = content,
    )
}
