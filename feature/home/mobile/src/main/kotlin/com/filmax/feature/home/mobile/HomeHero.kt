// Hero Главной: обложка со скримом, надзаголовок, название, мета и кнопка «Смотреть».
// Лента и переходы — в HomeScreen.

package com.filmax.feature.home.mobile

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.ShapeButton
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.ui.components.PosterImage
import com.filmax.core.ui.components.ratingLabel

@Composable
internal fun HomeHero(item: Item, onPlay: () -> Unit, onOpenItem: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(FilmaxMetrics.HomeHeroHeight)
            .clickable(onClick = onOpenItem),
    ) {
        PosterImage(
            // Широкий кадр — если он есть: вертикальную обложку на 412dp во всю ширину режет по центру.
            url = item.posters.wide?.takeIf { it.isNotBlank() } ?: item.posters.big,
            contentDescription = item.title,
            modifier = Modifier.matchParentSize(),
            shape = RectangleShape,
            accentColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        )
        Box(Modifier.matchParentSize().background(heroScrim()))
        HeroContent(item = item, onPlay = onPlay, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

/**
 * Скрим hero: снизу плотный — на нём лежит текст, сверху лёгкий — постер должен читаться.
 *
 * Плотнее макета в нижней трети. В макете кадр Дюны тёмный сам по себе, и лёгкого затемнения
 * хватало; на реальном каталоге постер бывает каким угодно — на светлом мультике надзаголовок
 * и мета пропадали. Скрим обязан держать текст на ЛЮБОЙ обложке, а не на удачной.
 *
 * Прозрачность набирается из surface, а не из [androidx.compose.ui.graphics.Color.Transparent]:
 * у последнего нулевой RGB, и переход к нему уводит градиент через чёрный, давая грязь.
 */
@Composable
private fun heroScrim(): Brush {
    val base = MaterialTheme.colorScheme.surface
    return Brush.verticalGradient(
        colorStops = arrayOf(
            0f to base.copy(alpha = 0.55f),
            0.45f to base.copy(alpha = 0.35f),
            0.62f to base.copy(alpha = 0.72f),
            0.82f to base.copy(alpha = 0.95f),
            1f to base,
        ),
    )
}

@Composable
private fun HeroContent(item: Item, onPlay: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "ВЫБОР РЕДАКЦИИ",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Text(
            item.title,
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        HeroMeta(item = item)
        // Кнопки «Буду смотреть» рядом нет намеренно: HomeEvent не умеет переключать watchlist,
        // а рисовать кнопку, которая ничего не делает, — хуже, чем не рисовать её вовсе.
        HeroPlayButton(onClick = onPlay)
    }
}

/** Мета hero: «8.3 КП · Фантастика · Драма · 2024» — оценка весом, остальное вторичным. */
@Composable
private fun HeroMeta(item: Item) {
    val rating = ratingLabel(item.rating.kinopoisk)
    val genres = item.genres.take(MAX_HERO_GENRES).joinToString(" · ") { it.title }
    Row(
        modifier = Modifier.padding(top = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (rating != null) {
            Text(
                "$rating КП",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (genres.isNotBlank()) {
            Text(
                genres,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                // Длинные жанры ужимаются с многоточием, а не выдавливают год за край экрана.
                modifier = Modifier.weight(1f, fill = false),
            )
        }
        if (item.year > 0) {
            Text(
                item.year.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Главное действие экрана — единственная белая заливка в монохроме. */
@Composable
private fun HeroPlayButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(top = 15.dp)
            .fillMaxWidth()
            .height(HeroButtonHeight)
            .clip(ShapeButton)
            .background(MaterialTheme.colorScheme.primary)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(20.dp),
        )
        Text(
            "Смотреть",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

/** Высота кнопки hero — из макета; общий [FilmaxMetrics.PrimaryButtonHeight] выше на 2dp. */
private val HeroButtonHeight = 48.dp

/** Больше двух жанров мета-строка hero не вмещает: на 360dp рядом ещё оценка и год. */
private const val MAX_HERO_GENRES = 2
