// Hero экрана деталей: бэкдроп со скримами, название с метой, рейтинги и ряд действий.
// Порядок секций и состояние — в TvDetailsScreen.
package com.filmax.feature.details.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemRating
import com.filmax.core.domain.catalog.model.MediaTrack
import com.filmax.core.tv.designsystem.TvButton
import com.filmax.core.tv.designsystem.TvMetaRow
import com.filmax.core.tv.designsystem.TvMetrics
import com.filmax.core.tv.designsystem.TvOnSurface
import com.filmax.core.tv.designsystem.TvOnSurfaceVariant
import com.filmax.core.tv.designsystem.TvSurface
import com.filmax.core.tv.designsystem.TvSurfaceContainerHigh
import com.filmax.core.tv.designsystem.TvSurfaceContainerHighest
import com.filmax.core.ui.components.HeroBackdrop
import com.filmax.core.ui.components.ratingLabel
import com.filmax.feature.details.common.SeriesData

// ─────────────────────────────────── Hero ───────────────────────────────────

/** Фокус и действия кнопок hero — группой (detekt LongParameterList). */
internal data class HeroPlayback(
    val playModifier: Modifier,
    val onPlay: () -> Unit,
    val onToggleFav: () -> Unit,
    /** Фокус зашёл на кнопки hero или ушёл с них — экран переключает стейт полотна. */
    val onHeroFocusChanged: (Boolean) -> Unit,
    /** null — у тайтла нет играбельного трейлера, кнопки нет. */
    val onTrailer: (() -> Unit)? = null,
)

/**
 * Hero: бэкдроп во всю ширину, текстовый блок прижат к низу слева (вариант A макета).
 *
 * Высота фиксированная: hero — первый элемент единого полотна и скрывается обычной прокруткой,
 * когда фокус уходит в контент, а не сжимается поверх него. Так постер всегда либо виден
 * целиком, либо честно уезжает вверх — ничего не режется.
 */
@Composable
internal fun DetailsHero(
    item: Item,
    series: SeriesData?,
    isFav: Boolean,
    playback: HeroPlayback,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(TvMetrics.DetailsHeroHeight),
    ) {
        HeroBackdrop(
            item = item,
            scrims = heroScrims(),
            modifier = Modifier.fillMaxSize(),
            posterUrl = item.posters.wide ?: item.posters.big,
            // Заглушка постера — нейтральная поверхность: цвет на экране только у самого кадра.
            accentColor = TvSurfaceContainerHigh,
        )

        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = TvMetrics.SafeHorizontal, bottom = 22.dp)
                .width(HeroTextWidth),
        ) {
            Text(
                item.title,
                style = MaterialTheme.typography.displaySmall,
                color = TvOnSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            TvMetaRow(
                parts = remember(item, series) { metaParts(item, series) },
                modifier = Modifier.padding(top = 11.dp),
            )
            RatingsRow(rating = item.rating, modifier = Modifier.padding(top = 9.dp))
            HeroButtons(
                isFav = isFav,
                resume = series?.resume,
                playback = playback,
                modifier = Modifier.padding(top = 18.dp),
            )
        }
    }
}

/**
 * Скримы hero. Стопы длинные и с промежуточными точками: в монохроме переход серого в серый
 * на коротком отрезке полосит (бандинг), а уход в прозрачность берём как `TvSurface` с нулевой
 * альфой — интерполяция в `Color.Transparent` тянет RGB к чёрному и даёт грязный «хвост».
 */
@Composable
private fun heroScrims(): List<Brush> = remember {
    listOf(
        Brush.horizontalGradient(
            0f to TvSurface.copy(alpha = 0.95f),
            0.40f to TvSurface.copy(alpha = 0.72f),
            0.72f to TvSurface.copy(alpha = 0.20f),
            1f to TvSurface.copy(alpha = 0f),
        ),
        Brush.verticalGradient(
            0f to TvSurface.copy(alpha = 0f),
            0.22f to TvSurface.copy(alpha = 0f),
            0.60f to TvSurface.copy(alpha = 0.35f),
            1f to TvSurface.copy(alpha = 0.98f),
        ),
    )
}

@Composable
private fun HeroButtons(
    isFav: Boolean,
    resume: MediaTrack?,
    playback: HeroPlayback,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.onFocusChanged { playback.onHeroFocusChanged(it.hasFocus) },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TvButton(
            text = playLabel(resume),
            onClick = playback.onPlay,
            leadingIcon = Icons.Filled.PlayArrow,
            modifier = playback.playModifier,
        )
        TvButton(
            text = if (isFav) "В списке" else "Буду смотреть",
            onClick = playback.onToggleFav,
            primary = false,
            leadingIcon = if (isFav) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
        )
        playback.onTrailer?.let { onTrailer ->
            TvButton(
                text = "Трейлер",
                onClick = onTrailer,
                primary = false,
                leadingIcon = Icons.Filled.Movie,
            )
        }
    }
}

/**
 * КП и IMDb показываем РАЗДЕЛЬНО: `rating.external` усредняет их, а расхождение оценок — это
 * и есть причина смотреть обе. Цветового кодирования нет: в монохроме оценку несёт число.
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
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        sources.forEachIndexed { index, (value, source) ->
            if (index > 0) {
                Box(
                    Modifier
                        .size(width = 1.dp, height = 14.dp)
                        .background(TvSurfaceContainerHighest),
                )
            }
            RatingValue(value = value, source = source)
        }
    }
}

@Composable
private fun RatingValue(value: String, source: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = TvOnSurface)
        Text(source, style = MaterialTheme.typography.bodyLarge, color = TvOnSurfaceVariant)
    }
}
