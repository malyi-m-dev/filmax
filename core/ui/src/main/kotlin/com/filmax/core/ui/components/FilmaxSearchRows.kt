package com.filmax.core.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NorthWest
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxColors
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.FilmaxTheme
import com.filmax.core.designsystem.FilmaxType
import com.filmax.core.designsystem.ShapePoster

/**
 * Результат поиска: постер, название с подсвеченным совпадением [query], рейтинг и мета. Стрелка
 * справа ([onInsert]) переносит название в поле — уточнить запрос, не набирая его заново.
 */
// Компонент дизайн-системы: параметры — его публичный API (см. FilmaxButton).
@Suppress("LongParameterList")
@Composable
fun FilmaxSearchResultRow(
    title: String,
    query: String,
    posterUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    rating: String? = null,
    meta: String? = null,
    onInsert: (() -> Unit)? = null,
) {
    val accent = FilmaxTheme.palette.accentText
    Row(
        modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = FilmaxMetrics.ScreenPadding, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PosterImage(
            url = posterUrl,
            contentDescription = null,
            shape = ResultPosterShape,
            modifier = Modifier.size(width = 56.dp, height = 84.dp),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                highlighted(title, query, SpanStyle(color = accent)),
                style = FilmaxType.button,
                color = FilmaxColors.Text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                rating?.let { FilmaxRatingPill(it) }
                meta?.let {
                    Text(
                        it,
                        style = FilmaxType.meta,
                        color = FilmaxColors.TextDim,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        onInsert?.let {
            FilmaxIconButton(
                Icons.Filled.NorthWest,
                contentDescription = "Подставить в поиск",
                onClick = it,
                style = FilmaxIconButtonStyle.Plain,
                tint = FilmaxColors.TextDim,
                size = FilmaxMetrics.IconButtonSmall,
            )
        }
    }
}

/** Вид подсказки: из истории запросов или из того, что ищут часто. */
enum class SuggestionKind { Recent, Trending }

/** Подсказка пустого поиска: иконка вида и сам запрос. Тап — искать его. */
@Composable
fun FilmaxSuggestionRow(text: String, kind: SuggestionKind, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = FilmaxMetrics.ScreenPadding),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (kind == SuggestionKind.Recent) Icons.Filled.History else Icons.AutoMirrored.Filled.TrendingUp,
            contentDescription = null,
            tint = FilmaxColors.TextDim,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text,
            style = FilmaxType.bodyLarge,
            color = FilmaxColors.Text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Заглушка постера, пока грузится сетка: та же форма и подпись, что у карточки, и мягкая пульсация.
 * Сетка не прыгает, когда приходят настоящие постеры.
 */
@Composable
fun FilmaxPosterSkeleton(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val pulse = transition.animateFloat(
        initialValue = SKELETON_LOW,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(SKELETON_MILLIS), RepeatMode.Reverse),
        label = "skeletonPulse",
    )
    Column(modifier.graphicsLayer { alpha = pulse.value }, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(POSTER_ASPECT)
                .clip(ShapePoster)
                .background(FilmaxColors.Field),
        )
        Box(Modifier.fillMaxWidth(LINE_LONG).height(10.dp).clip(LineShape).background(FilmaxColors.Field))
        Box(Modifier.fillMaxWidth(LINE_SHORT).height(8.dp).clip(LineShape).background(FilmaxColors.Card))
    }
}

/** Название с подсветкой первого вхождения запроса без учёта регистра. */
private fun highlighted(title: String, query: String, style: SpanStyle) = buildAnnotatedString {
    val needle = query.trim()
    val start = if (needle.isEmpty()) -1 else title.indexOf(needle, ignoreCase = true)
    if (start < 0) {
        append(title)
        return@buildAnnotatedString
    }
    append(title.substring(0, start))
    withStyle(style) { append(title.substring(start, start + needle.length)) }
    append(title.substring(start + needle.length))
}

private val ResultPosterShape = RoundedCornerShape(10.dp)
private val LineShape = RoundedCornerShape(4.dp)
private const val SKELETON_LOW = 0.45f
private const val SKELETON_MILLIS = 900
private const val LINE_LONG = 0.8f
private const val LINE_SHORT = 0.5f
