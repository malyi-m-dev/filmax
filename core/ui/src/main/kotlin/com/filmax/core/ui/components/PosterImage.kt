package com.filmax.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/**
 * Постер с ленивой загрузкой через Coil [AsyncImage]. Под обложкой всегда лежит статичный
 * градиент-плейсхолдер: он виден, пока постер грузится или если ссылка пустая/битая, а после
 * загрузки полностью перекрывается картинкой ([ContentScale.Crop] заполняет всю область).
 *
 * Намеренно используется лёгкий [AsyncImage], а НЕ `SubcomposeAsyncImage` с анимированным
 * shimmer: на Android TV десятки постеров в каруселях рендерятся одновременно, и subcomposition
 * на каждом + бесконечная shimmer-анимация на каждом грузящемся постере роняли FPS. Плейсхолдер
 * сделан статичным по той же причине — никакой `rememberInfiniteTransition` в hot-path списков.
 */
@Composable
fun PosterImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    accentColor: Color = Color(0xFFB4305A),
) {
    val placeholder = remember(accentColor) { posterPlaceholderBrush(accentColor) }
    Box(modifier.clip(shape).background(placeholder)) {
        AsyncImage(
            model = url,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/** Тёмный градиент-заглушка постера (под обложкой и при ошибке загрузки). */
private fun posterPlaceholderBrush(accentColor: Color): Brush =
    Brush.linearGradient(
        colors = listOf(accentColor.copy(alpha = 0.7f), Color(0xFF141012)),
        start = Offset(0f, 0f),
        end = Offset(200f, 600f),
    )

/** Тот же градиент как самостоятельный composable — для превью дизайн-системы. */
@Composable
fun GradientPosterPlaceholder(accentColor: Color, modifier: Modifier = Modifier) {
    val placeholder = remember(accentColor) { posterPlaceholderBrush(accentColor) }
    Box(modifier = modifier.background(placeholder))
}
