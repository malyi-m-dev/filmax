package com.filmax.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Карточка подборки Filmax — акцентный градиент, иконка-плитка и заголовок.
 *
 * Минимальное использование: `FilmaxCollectionCard("Оскар 2024", accent, icon, onClick = {})`.
 */
// Компонент дизайн-системы: параметры — его публичный API (Compose-конвенция: modifier — прямой
// параметр, хвост — опции с дефолтами). Обёртка в data-класс сломала бы «минимальный API» и modifier.
@Suppress("LongParameterList")
@Composable
fun FilmaxCollectionCard(
    title: String,
    accent: Color,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    posterUrl: String? = null,
) {
    val hasPoster = !posterUrl.isNullOrBlank()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick),
    ) {
        if (hasPoster) {
            PosterImage(
                url = posterUrl,
                contentDescription = title,
                modifier = Modifier.matchParentSize(),
                shape = RoundedCornerShape(24.dp),
                accentColor = accent,
            )
        }
        // Градиент: при постере — затемнение снизу для читаемости, иначе — лёгкий акцент.
        Box(
            Modifier
                .matchParentSize()
                .background(
                    if (hasPoster) {
                        Brush.verticalGradient(
                            listOf(accent.copy(alpha = 0.35f), Color.Transparent, Color(0xF2141012))
                        )
                    } else {
                        Brush.linearGradient(
                            listOf(accent.copy(alpha = 0.28f), accent.copy(alpha = 0.08f), Color.Transparent)
                        )
                    }
                )
        )
        FilmaxCollectionCardContent(
            title = title,
            accent = accent,
            icon = icon,
            subtitle = subtitle,
            hasPoster = hasPoster,
        )
    }
}

/** Наполнение карточки поверх фона/градиента: иконка-плитка сверху, заголовок и подзаголовок снизу. */
@Composable
private fun BoxScope.FilmaxCollectionCardContent(
    title: String,
    accent: Color,
    icon: ImageVector,
    subtitle: String?,
    hasPoster: Boolean,
) {
    Column(
        modifier = Modifier
            .matchParentSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(accent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Column {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = if (hasPoster) Color.White else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (hasPoster) {
                        Color.White.copy(
                            alpha = 0.85f
                        )
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
