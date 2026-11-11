package com.filmax.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ImageNotSupported
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.filmax.core.designsystem.FilmaxColors
import com.filmax.core.designsystem.ShapePoster

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
    shape: Shape = ShapePoster,
    // Дефолт берём из темы, а не из константы. Раньше здесь был зашит розовый #B4305A: любой
    // вызов без явного accentColor красил плейсхолдер мимо схемы — цветное пятно там, где во
    // всём приложении цвет только у постеров.
    accentColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
) {
    val placeholder = remember(accentColor) { posterPlaceholderBrush(accentColor) }
    // Битую ссылку помечаем знаком, а не оставляем пустую плашку: kino.pub отдаёт адрес постера
    // всегда, даже когда файла нет (у подборки 967 это честный 404), и голый градиент читается
    // как вечная загрузка. Ключ — url: при переиспользовании карточки в ленте флаг сбрасывается.
    var failed by remember(url) { mutableStateOf(false) }
    Box(modifier.clip(shape).background(placeholder), contentAlignment = Alignment.Center) {
        AsyncImage(
            model = url,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            onState = { state -> failed = state is AsyncImagePainter.State.Error },
        )
        if (failed) {
            Icon(
                Icons.Outlined.ImageNotSupported,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(BrokenPosterIconSize),
            )
        }
    }
}

/** Знак «постера нет» — заметен на карточке в ряду и не спорит с обложками соседей. */
private val BrokenPosterIconSize = 28.dp

/** Нижний (тёмный) цвет градиента-заглушки постера. */
private val PlaceholderBottomColor = FilmaxColors.PosterPlaceholder

/** Конечная точка линейного градиента-заглушки (диагональ сверху-слева вниз-вправо). */
private const val PLACEHOLDER_GRADIENT_END_X = 200f
private const val PLACEHOLDER_GRADIENT_END_Y = 600f

/** Тёмный градиент-заглушка постера (под обложкой и при ошибке загрузки). */
private fun posterPlaceholderBrush(accentColor: Color): Brush =
    Brush.linearGradient(
        colors = listOf(accentColor.copy(alpha = 0.7f), PlaceholderBottomColor),
        start = Offset(0f, 0f),
        end = Offset(PLACEHOLDER_GRADIENT_END_X, PLACEHOLDER_GRADIENT_END_Y),
    )

/** Тот же градиент как самостоятельный composable — для превью дизайн-системы. */
@Composable
fun GradientPosterPlaceholder(accentColor: Color, modifier: Modifier = Modifier) {
    val placeholder = remember(accentColor) { posterPlaceholderBrush(accentColor) }
    Box(modifier = modifier.background(placeholder))
}
