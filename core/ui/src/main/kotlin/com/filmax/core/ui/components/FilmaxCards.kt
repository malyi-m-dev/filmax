package com.filmax.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.ShapeCard
import com.filmax.core.designsystem.ShapePoster
import com.filmax.core.domain.catalog.model.Collection
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.watching.model.WatchProgress
import java.util.Locale

/**
 * Карточка-постер 2:3 — один из двух типов медиа-карточек. Размер задаётся снаружи: в ряду
 * новинок 120×180, в сетке каталога 98×147, в «Похожем» 112×168 — но это одна и та же карточка.
 *
 * Подпись живёт ПОД постером, а не поверх: в монохроме постер — единственный источник цвета,
 * и накрывать его градиентом-скримом значит гасить единственное, что держит экран.
 */
// Компонент дизайн-системы: параметры — его публичный API (Compose-конвенция: modifier прямым
// параметром, хвост — опции с дефолтами). Обёртка в data-класс сломала бы «минимальный API».
@Suppress("LongParameterList")
@Composable
fun FilmaxPosterCard(
    title: String,
    posterUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = FilmaxMetrics.PosterWidth,
    height: Dp = FilmaxMetrics.PosterHeight,
    rating: String? = null,
    meta: String? = null,
) {
    Column(
        modifier = modifier
            .width(width)
            .clickable(onClick = onClick),
    ) {
        Box(
            Modifier
                .size(width = width, height = height)
                .clip(ShapePoster)
                .background(MaterialTheme.colorScheme.surfaceContainer),
        ) {
            PosterImage(
                url = posterUrl,
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                shape = ShapePoster,
                accentColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            )
            if (rating != null) {
                FilmaxRatingPill(
                    rating = rating,
                    modifier = Modifier.align(Alignment.TopEnd).padding(5.dp),
                )
            }
        }
        Text(
            title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
        if (!meta.isNullOrBlank()) {
            Text(
                meta,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Карточка 16:9 с полосой прогресса — второй и последний тип. «Продолжить», история, эпизоды.
 * [meta] несёт то, ради чего карточка существует: «S2 · E5 · осталось 18 мин».
 */
// Компонент дизайн-системы: параметры — его публичный API (см. FilmaxPosterCard).
@Suppress("LongParameterList")
@Composable
fun FilmaxProgressCard(
    title: String,
    meta: String?,
    posterUrl: String,
    progress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = FilmaxMetrics.ContinueWidth,
    height: Dp = FilmaxMetrics.ContinueHeight,
) {
    Column(
        modifier = modifier
            .width(width)
            .clickable(onClick = onClick),
    ) {
        Box(
            Modifier
                .size(width = width, height = height)
                .clip(ShapeCard)
                .background(MaterialTheme.colorScheme.surfaceContainer),
        ) {
            PosterImage(
                url = posterUrl,
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                shape = ShapeCard,
                accentColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            )
            FilmaxProgressBar(
                progress = progress,
                modifier = Modifier.align(Alignment.BottomStart),
            )
        }
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 7.dp),
        )
        if (!meta.isNullOrBlank()) {
            Text(
                meta,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

/** Полоса прогресса на карточке: белая заливка по полупрозрачному треку. */
@Composable
fun FilmaxProgressBar(progress: Float, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(FilmaxMetrics.ProgressBarHeight)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(FilmaxMetrics.ProgressBarHeight)
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}

/**
 * Пилюля рейтинга поверх постера: полупрозрачная тёмная подложка, без цветового кодирования —
 * в монохроме оценку несёт число.
 */
@Composable
fun FilmaxRatingPill(rating: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(ShapePoster)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
            .padding(horizontal = 6.dp, vertical = 3.dp),
    ) {
        Text(
            rating,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * Оценка для пилюли: в домене строка вида «8.312», на экране нужен один знак.
 *
 * Ноль — это «оценки нет», а не «ноль баллов»: kino.pub отдаёт `0` для тайтлов без рейтинга,
 * и печатать «0.0» под постером — врать зрителю.
 */
fun ratingLabel(raw: String?): String? = ratingLabel(raw?.toDoubleOrNull())

/** То же для уже разобранной оценки (`rating.external` — усреднённая IMDb+КП). */
fun ratingLabel(value: Double?): String? =
    value?.takeIf { it > 0 }?.let { String.format(Locale.US, "%.1f", it) }

/** Собирает мету `тип · год`, пропуская пустые части. */
fun posterMeta(type: String?, year: Int): String? {
    val parts = buildList {
        if (!type.isNullOrBlank()) add(type)
        if (year > 0) add(year.toString())
    }
    return parts.joinToString(" · ").ifBlank { null }
}

/**
 * Тип тайтла под постером и в мете деталей. `serial`/`docuserial` из API зрителю не
 * показываем: у kino.pub это технические значения, а не то, как о тайтле говорят.
 */
fun typeLabel(type: ItemType): String = when (type) {
    ItemType.MOVIE -> "Фильм"
    ItemType.SERIES -> "Сериал"
    ItemType.ANIME -> "Аниме"
    ItemType.DOCUMENTARY -> "Документальный"
    ItemType.TV -> "ТВ"
}

/**
 * Подпись карточки «продолжить»: «S2 · осталось 18 мин». Одна на телефон и ТВ — раньше жила
 * тремя одинаковыми копиями (обе Главных и «Моё»).
 *
 * Номер эпизода макета («E5») не выводим: в [WatchProgress] его нет — `videoId` это
 * идентификатор трека, а не порядковый номер серии (`PlayerScreenModel` матчит им `MediaTrack.id`).
 */
fun continueMeta(progress: WatchProgress?): String? {
    if (progress == null) return null
    val parts = buildList {
        progress.season?.takeIf { it > 0 }?.let { add("S$it") }
        val remaining = remainingMinutes(progress)
        when {
            remaining != null -> add("осталось ${durationLabel(remaining)}")
            // Остатка нет, а прогресс есть — досмотрено: иначе карточка молчала бы о том,
            // почему она вообще в «Продолжить».
            progress.fraction > 0f -> add("просмотрено")
        }
    }
    return parts.joinToString(" · ").ifBlank { null }
}

/** Сколько минут осталось до конца трека; null — прогресса нет или уже досмотрено. */
private fun remainingMinutes(progress: WatchProgress): Int? {
    val watched = progress.timeSeconds
    val total = progress.durationSeconds?.takeIf { it > 0 }
    if (watched == null || total == null) return null
    return ((total - watched) / SECONDS_IN_MINUTE).takeIf { it > 0 }
}

/** «1 ч 20 мин» / «45 мин» — часы показываем, только когда они есть. */
fun durationLabel(totalMinutes: Int): String {
    val hours = totalMinutes / MINUTES_IN_HOUR
    val minutes = totalMinutes % MINUTES_IN_HOUR
    return when {
        hours > 0 && minutes > 0 -> "$hours ч $minutes мин"
        hours > 0 -> "$hours ч"
        else -> "$minutes мин"
    }
}

/**
 * Постер подборки: сначала средний, затем большой. null — картинки нет вовсе, такую подборку
 * ряды не показывают (в монохроме карточку держит только изображение).
 */
fun Collection.posterUrl(): String? =
    posters?.let { it.medium.ifEmpty { it.big } }?.takeIf { it.isNotBlank() }

private const val MINUTES_IN_HOUR = 60
private const val SECONDS_IN_MINUTE = 60
