package com.filmax.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxColors
import com.filmax.core.designsystem.FilmaxTheme
import com.filmax.core.designsystem.FilmaxType

/**
 * Пустое состояние: иконка в плитке оттенка палитры, заголовок и подпись. [action] — кнопка под
 * текстом («Открыть каталог»), когда пустоту есть чем заполнить.
 *
 * Минимальное использование: `FilmaxEmptyState(Icons.Default.Favorite, "Пока пусто")`.
 */
@Composable
fun FilmaxEmptyState(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    val palette = FilmaxTheme.palette
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(TileShape)
                .background(palette.accent.copy(alpha = FILL_ALPHA))
                .border(1.dp, palette.accent.copy(alpha = BORDER_ALPHA), TileShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = palette.accentText, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text(title, style = FilmaxType.sectionTitle, color = FilmaxColors.Text, textAlign = TextAlign.Center)
        if (!subtitle.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                subtitle,
                style = FilmaxType.meta,
                color = FilmaxColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }
        action?.let {
            Spacer(Modifier.height(20.dp))
            it()
        }
    }
}

private val TileShape = RoundedCornerShape(26.dp)
private const val FILL_ALPHA = 0.16f
private const val BORDER_ALPHA = 0.28f
