// Верх экрана деталей: кадр с кнопкой «назад», заголовок с метой и рейтингами, кнопки действий.
// Порядок секций и состояние — в DetailsScreen.

package com.filmax.feature.details.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.ShapeButton
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemRating
import com.filmax.core.domain.catalog.model.MediaTrack
import com.filmax.core.ui.components.HeroBackdrop
import com.filmax.core.ui.components.ratingLabel
import com.filmax.core.ui.components.typeLabel
import com.filmax.feature.details.common.SeriesData
import com.filmax.feature.details.common.volumeLabel

/**
 * Отступ кнопки «назад» под статус-баром. В макете она в 44dp от верха кадра, но кадр макета
 * нарисован без статус-бара, а приложение живёт edge-to-edge — те же 44dp набираются
 * `statusBarsPadding()` плюс это поле.
 */
private val BackButtonInset = 8.dp

@Composable
internal fun DetailsHero(item: Item, height: Dp, onBack: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(height)) {
        HeroBackdrop(
            item = item,
            scrims = listOf(heroScrim()),
            modifier = Modifier.matchParentSize(),
            posterUrl = item.posters.wide ?: item.posters.big,
            // Заглушка постера — нейтральная поверхность: цвет на экране только у самого кадра.
            accentColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        )
        BackButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 20.dp, top = BackButtonInset),
        )
    }
}

/**
 * Скрим кадра. Макет: `linear-gradient(0deg, #0A0A0A 4%, rgba(10,10,10,.15) 55%, rgba(10,10,10,.5)
 * 100%)` — CSS-градус 0 считает снизу вверх, поэтому стопы здесь развёрнуты.
 *
 * Прозрачность берём как surface с нулевой альфой, а НЕ `Color.Transparent`: у последнего нулевые
 * RGB, и интерполяция тянет переход через чёрный — на градиенте появляется грязный хвост.
 */
@Composable
private fun heroScrim(): Brush {
    val surface = MaterialTheme.colorScheme.surface
    return remember(surface) {
        Brush.verticalGradient(
            0f to surface.copy(alpha = 0.5f),
            0.45f to surface.copy(alpha = 0.15f),
            0.96f to surface,
            1f to surface,
        )
    }
}

@Composable
private fun BackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(FilmaxMetrics.BackButtonSize)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Назад",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
internal fun DetailsHeader(item: Item, series: SeriesData?, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            item.title,
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        MetaRow(
            parts = remember(item, series) { metaParts(item, series) },
            modifier = Modifier.padding(top = 10.dp),
        )
        RatingsRow(rating = item.rating, modifier = Modifier.padding(top = 10.dp))
    }
}

/**
 * Мета-строка: «Фильм · 2024 · 2 ч 46 мин · США». Одним [Text], а не рядом из Text и точек: строка
 * переносится сама, а ряд на узком экране пришлось бы городить через FlowRow.
 */
@Composable
private fun MetaRow(parts: List<String>, modifier: Modifier = Modifier) {
    if (parts.isEmpty()) return
    Text(
        parts.joinToString(" · "),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/**
 * КП и IMDb показываем РАЗДЕЛЬНО: `rating.external` их усредняет, а расхождение оценок — это и есть
 * причина смотреть обе. Цветового кодирования нет: в монохроме оценку несёт число.
 */
@Composable
private fun RatingsRow(rating: ItemRating, modifier: Modifier = Modifier) {
    // ratingLabel режет «0» (у kino.pub это «оценки нет») и приводит «8.312» к одному знаку.
    val sources = remember(rating) {
        buildList {
            ratingLabel(rating.kinopoisk)?.let { add(it to "КиноПоиск") }
            ratingLabel(rating.imdb)?.let { add(it to "IMDb") }
        }
    }
    if (sources.isEmpty()) return

    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        sources.forEachIndexed { index, (value, source) ->
            if (index > 0) {
                Box(
                    Modifier
                        .size(width = 1.dp, height = 13.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                )
            }
            RatingValue(value = value, source = source)
        }
    }
}

@Composable
private fun RatingValue(value: String, source: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(source, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Действия кнопок — группой (detekt LongParameterList). */
internal data class DetailsButtonActions(
    val onPlay: () -> Unit,
    val onToggleFav: () -> Unit,
    val onShare: () -> Unit,
    /** null — у тайтла нет играбельного трейлера, кнопки нет. */
    val onTrailer: (() -> Unit)? = null,
)

@Composable
internal fun DetailsButtons(
    isFav: Boolean,
    resume: MediaTrack?,
    actions: DetailsButtonActions,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        PlayButton(text = playLabel(resume), onClick = actions.onPlay)
        Row(
            Modifier.padding(top = 11.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            WatchlistButton(
                isFav = isFav,
                onClick = actions.onToggleFav,
                modifier = Modifier.weight(1f),
            )
            actions.onTrailer?.let { onTrailer ->
                IconSquareButton(
                    icon = Icons.Filled.Movie,
                    contentDescription = "Трейлер",
                    onClick = onTrailer,
                )
            }
            IconSquareButton(
                icon = Icons.Filled.Share,
                contentDescription = "Поделиться",
                onClick = actions.onShare,
            )
        }
    }
}

@Composable
private fun PlayButton(text: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(FilmaxMetrics.PrimaryButtonHeight)
            .clip(ShapeButton)
            .background(MaterialTheme.colorScheme.primary)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(9.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(18.dp),
        )
        Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimary)
    }
}

@Composable
private fun WatchlistButton(isFav: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .height(FilmaxMetrics.SecondaryButtonHeight)
            .clip(ShapeButton)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (isFav) Icons.Filled.Check else Icons.Filled.Add,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(16.dp),
        )
        Text(
            if (isFav) "В списке" else "Буду смотреть",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Квадратная вторичная кнопка-иконка (Поделиться, Трейлер) — одна форма на оба действия. */
@Composable
private fun IconSquareButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(FilmaxMetrics.SecondaryButtonHeight)
            .clip(ShapeButton)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** Мета-строка макета: «Фильм · 2024 · 2 ч 46 мин · США» (у сериала вместо длительности — объём). */
private fun metaParts(item: Item, series: SeriesData?): List<String> = buildList {
    add(typeLabel(item.type))
    if (item.year > 0) add(item.year.toString())
    volumeLabel(item, series)?.let { add(it) }
    if (item.country.isNotBlank()) add(item.country)
}

/** «Продолжить · S1 E3» — сериал с недосмотренной серией; иначе «Смотреть». */
private fun playLabel(resume: MediaTrack?): String = when {
    resume == null -> "Смотреть"
    resume.seasonNumber > 0 -> "Продолжить · S${resume.seasonNumber} E${resume.number}"
    else -> "Продолжить · Серия ${resume.number}"
}
