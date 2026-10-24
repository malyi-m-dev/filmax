package com.filmax.core.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxColors
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.FilmaxTheme
import com.filmax.core.designsystem.FilmaxType
import com.filmax.core.designsystem.ShapeCard
import com.filmax.core.designsystem.ShapePoster
import com.filmax.core.designsystem.glassFill
import com.filmax.core.domain.catalog.model.Collection
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.watching.model.WatchProgress
import java.util.Locale

/** Постер 2:3 — ширину задаёт место вызова, высота следует из пропорции. */
const val POSTER_ASPECT = 2f / 3f

/** Кадр 16:9: «Продолжить», кадры фильма, превью серии. */
const val FRAME_ASPECT = 16f / 9f

/**
 * Карточка-постер: ряд «В тренде», сетка каталога, «Похожие». Ширину задаёт [modifier]
 * (`width(128.dp)` в ряду, `weight(1f)` в сетке), высота — пропорция 2:3. Рейтинг — пилюлей в
 * углу постера, название и мета — под ним: накрывать постер скримом значит гасить кадр.
 * [onLongClick] — убрать из «Буду смотреть» или папки.
 */
// Компонент дизайн-системы: параметры — его публичный API (см. FilmaxButton).
@Suppress("LongParameterList")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FilmaxPosterCard(
    title: String,
    posterUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    rating: String? = null,
    meta: String? = null,
    onLongClick: (() -> Unit)? = null,
) {
    Column(modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(POSTER_ASPECT)
                .clip(ShapePoster),
        ) {
            PosterImage(url = posterUrl, contentDescription = title, modifier = Modifier.fillMaxSize())
            rating?.let { FilmaxRatingPill(it, Modifier.align(Alignment.TopStart).padding(8.dp)) }
        }
        CardCaption(title = title, meta = meta, titleStyle = FilmaxType.cardTitle)
    }
}

/**
 * Карточка «Продолжить»: кадр 16:9 с полосой прогресса и стеклянной кнопкой «▶» поверх затемнённого
 * низа. [meta] несёт то, ради чего карточка существует: «S2 · осталось 18 мин».
 *
 * [placeholder] рисуется вместо кадра, когда ссылки нет вовсе: у kino.pub кадр серии часто
 * пустой, и ряд одинаковых заглушек не отличить друг от друга, а крупный номер серии отличает.
 */
// Компонент дизайн-системы: параметры — его публичный API (см. FilmaxButton).
@Suppress("LongParameterList")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FilmaxContinueCard(
    title: String,
    meta: String?,
    imageUrl: String,
    progress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    onLongClick: (() -> Unit)? = null,
) {
    Column(modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(FRAME_ASPECT)
                .clip(ShapeCard)
                .background(FilmaxColors.Card),
        ) {
            if (imageUrl.isBlank() && placeholder != null) {
                Text(
                    placeholder,
                    style = FilmaxType.sectionTitle,
                    color = FilmaxColors.TextDim,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                PosterImage(
                    url = imageUrl,
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    shape = ShapeCard
                )
            }
            Box(Modifier.matchParentSize().background(FrameScrim))
            PlayMark(Modifier.align(Alignment.BottomStart).padding(start = 10.dp, bottom = 12.dp))
            FilmaxProgressBar(progress = progress, modifier = Modifier.align(Alignment.BottomStart))
        }
        CardCaption(title = title, meta = meta, titleStyle = FilmaxType.chip)
    }
}

/** Название и мета под карточкой — одна строка каждая, длинное ужимается многоточием. */
@Composable
private fun CardCaption(title: String, meta: String?, titleStyle: TextStyle) {
    Text(
        title,
        style = titleStyle,
        color = FilmaxColors.Text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(top = 8.dp),
    )
    if (!meta.isNullOrBlank()) {
        Text(
            meta,
            style = FilmaxType.metaSmall,
            color = FilmaxColors.TextDim,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

/** Стеклянный «▶» на кадре: карточка ведёт сразу в плеер, а не в детали. */
@Composable
private fun PlayMark(modifier: Modifier = Modifier) {
    Box(modifier.size(32.dp).glassFill(CircleShape), contentAlignment = Alignment.Center) {
        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

/** Полоса прогресса: заливка акцентом палитры по полупрозрачному треку. */
@Composable
fun FilmaxProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    thickness: Dp = FilmaxMetrics.ProgressBarHeight,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(thickness)
            .background(FilmaxColors.Track),
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(thickness)
                .background(FilmaxTheme.palette.accent),
        )
    }
}

/** Затемнение низа кадра под кнопкой и полосой: на светлом кадре они иначе пропадают. */
private val FrameScrim = Brush.verticalGradient(
    0.45f to Color.Transparent,
    1f to Color.Black.copy(alpha = 0.6f),
)

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

/** С какой оценки она «высокая» и выделяется цветом. */
private const val HIGH_RATING = 8.0

/** Высокая ли оценка — такую пилюля выделяет вторым акцентом. */
fun isHighRating(raw: String?): Boolean = (raw?.toDoubleOrNull() ?: 0.0) >= HIGH_RATING

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
 * ряды не показывают (карточку держит изображение).
 */
fun Collection.posterUrl(): String? =
    posters?.let { it.medium.ifEmpty { it.big } }?.takeIf { it.isNotBlank() }

private const val MINUTES_IN_HOUR = 60
private const val SECONDS_IN_MINUTE = 60
