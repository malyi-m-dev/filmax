package com.filmax.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Цветовой стиль [FilmaxBadge]. */
enum class FilmaxBadgeStyle { Primary, Neutral, Success, Warning, Error }

/**
 * Бейдж Filmax — компактная pill-метка статуса (Premium, «Активна», теги и т.п.).
 *
 * Минимальное использование: `FilmaxBadge("Premium")`.
 */
@Composable
fun FilmaxBadge(
    text: String,
    modifier: Modifier = Modifier,
    style: FilmaxBadgeStyle = FilmaxBadgeStyle.Primary,
    icon: ImageVector? = null,
) {
    val colors = MaterialTheme.colorScheme
    val background: Color
    val content: Color
    when (style) {
        FilmaxBadgeStyle.Primary -> {
            background = colors.primaryContainer
            content = colors.onPrimaryContainer
        }
        FilmaxBadgeStyle.Neutral -> {
            background = colors.surfaceContainerHighest
            content = colors.onSurfaceVariant
        }
        FilmaxBadgeStyle.Success -> {
            background = Color(0xFF1E4D40)
            content = Color(0xFF8FD6B5)
        }
        FilmaxBadgeStyle.Warning -> {
            background = Color(0xFF5A3B1A)
            content = Color(0xFFF4B792)
        }
        FilmaxBadgeStyle.Error -> {
            background = colors.errorContainer
            content = colors.onErrorContainer
        }
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text = text,
            color = content,
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 0.5.sp,
        )
    }
}
