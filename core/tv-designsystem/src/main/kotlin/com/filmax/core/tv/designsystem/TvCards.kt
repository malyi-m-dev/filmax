package com.filmax.core.tv.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.util.Locale

/**
 * Размер карточки 16:9. Оба варианта — одна и та же карточка, разной ширины: «продолжить»
 * на главной крупнее, эпизод в ряду серий компактнее.
 */
enum class TvCardSize(val width: Dp, val height: Dp) {
    Continue(TvMetrics.ContinueWidth, TvMetrics.ContinueHeight),
    Episode(TvMetrics.EpisodeWidth, TvMetrics.EpisodeHeight),
}

/**
 * Карточка-постер 2:3 — один из двух типов медиа-карточек на всё приложение. Используется
 * и в рядах, и в сетках каталога: один размер везде.
 *
 * Подпись живёт ПОД постером, а не поверх него: в монохроме постер — единственный источник
 * цвета, и накрывать его градиентом-скримом с текстом значит гасить единственное, что
 * держит экран. Заодно уходит риск бандинга на сером градиенте.
 *
 * [rating] — уже отформатированная строка (например «8.3»), null — пилюли нет.
 */
// Компонент дизайн-системы: параметры — его публичный API (Compose-конвенция: modifier прямым
// параметром, хвост — опции с дефолтами). Обёртка в data-класс сломала бы «минимальный API».
@Suppress("LongParameterList")
@Composable
fun TvPosterCard(
    title: String,
    meta: String?,
    posterUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    rating: String? = null,
    focusRequester: FocusRequester? = null,
    posterContent: @Composable (url: String, modifier: Modifier) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val dim = rememberDimAlpha(focused)

    Column(
        modifier = modifier
            .width(TvMetrics.PosterWidth)
            .onFocusChanged { focused = it.hasFocus }
            .alpha(dim),
    ) {
        TvFocusCard(
            onClick = onClick,
            shape = TvMetrics.PosterShape,
            focusRequester = focusRequester,
            modifier = Modifier.size(width = TvMetrics.PosterWidth, height = TvMetrics.PosterHeight),
        ) {
            Box(Modifier.fillMaxSize().clip(TvMetrics.PosterShape)) {
                posterContent(posterUrl, Modifier.fillMaxSize())
                if (rating != null) {
                    TvRatingPill(
                        rating = rating,
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                    )
                }
            }
        }
        TvCardCaption(title = title, meta = meta, focused = focused)
    }
}

/**
 * Карточка 16:9 с полосой прогресса — второй и последний тип медиа-карточки. «Продолжить
 * смотреть», история, эпизоды сериала.
 *
 * [progress] — доля 0..1. [meta] несёт то, ради чего карточка существует: «S2 · осталось 18 мин».
 * [size] — карточка эпизода уже, чем карточка продолжения ([TvCardSize.Episode] против
 * [TvCardSize.Continue]): в ряд эпизодов их помещается больше, а пропорция 16:9 та же.
 */
// Компонент дизайн-системы: параметры — его публичный API (см. TvPosterCard).
@Suppress("LongParameterList")
@Composable
fun TvProgressCard(
    title: String,
    meta: String?,
    posterUrl: String,
    progress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: TvCardSize = TvCardSize.Continue,
    focusRequester: FocusRequester? = null,
    posterContent: @Composable (url: String, modifier: Modifier) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val dim = rememberDimAlpha(focused)

    Column(
        modifier = modifier
            .width(size.width)
            .onFocusChanged { focused = it.hasFocus }
            .alpha(dim),
    ) {
        TvFocusCard(
            onClick = onClick,
            shape = TvMetrics.CardShape,
            focusRequester = focusRequester,
            modifier = Modifier.size(width = size.width, height = size.height),
        ) {
            Box(Modifier.fillMaxSize().clip(TvMetrics.CardShape)) {
                posterContent(posterUrl, Modifier.fillMaxSize())
                TvProgressBar(
                    progress = progress,
                    modifier = Modifier.align(Alignment.BottomStart),
                )
            }
        }
        TvCardCaption(title = title, meta = meta, focused = focused)
    }
}

/** Полоса прогресса на карточке: белая заливка по прозрачно-белому треку. */
@Composable
fun TvProgressBar(progress: Float, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(3.dp)
            .background(TvAccent.copy(alpha = 0.22f)),
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(3.dp)
                .background(TvAccent),
        )
    }
}

/**
 * Пилюля рейтинга поверх постера. Полупрозрачная тёмная подложка вместо цветной: в монохроме
 * оценку не кодируем цветом — число говорит само.
 */
@Composable
fun TvRatingPill(rating: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(TvMetrics.PosterShape)
            .background(TvSurface.copy(alpha = 0.72f))
            .padding(horizontal = 9.dp, vertical = 4.dp),
    ) {
        Text(
            rating,
            style = MaterialTheme.typography.labelSmall,
            color = TvOnSurface,
        )
    }
}

/**
 * Отступ подписи от постера. Рамка фокуса рисуется поверх увеличенной (scale 1.08) карточки
 * и опускается ниже её исходной границы на ~11dp у постера 2:3 — при меньшем отступе белая
 * рамка ложится прямо на текст.
 */
private val CaptionTopGap = 16.dp

/**
 * Подпись под карточкой: название (16sp) + мета (13sp). Мельче на TV не опускаемся.
 *
 * Длинное название не переносится и не обрезается многоточием навсегда: строка одна, а при
 * фокусе на карточке запускается бегущая строка — так виден весь заголовок без роста карточки.
 */
@Composable
private fun TvCardCaption(title: String, meta: String?, focused: Boolean) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = TvOnSurface,
        maxLines = 1,
        softWrap = false,
        overflow = if (focused) TextOverflow.Clip else TextOverflow.Ellipsis,
        modifier = Modifier
            .padding(top = CaptionTopGap)
            .then(if (focused) Modifier.basicMarquee(iterations = Int.MAX_VALUE) else Modifier),
    )
    if (!meta.isNullOrBlank()) {
        Text(
            meta,
            style = MaterialTheme.typography.bodySmall,
            color = TvOnSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

/** Собирает мету постера `тип · год`, пропуская пустые части. */
fun posterMeta(type: String?, year: Int): String? {
    val parts = buildList {
        if (!type.isNullOrBlank()) add(type)
        if (year > 0) add(year.toString())
    }
    return parts.joinToString(" · ").ifBlank { null }
}

/**
 * Оценка для пилюли и меты: в домене это строка вида «8.312», на экране нужен один знак.
 *
 * Ноль — это «оценки нет», а не «ноль баллов»: kino.pub отдаёт `0` для тайтлов без рейтинга,
 * и печатать «0.0 КП» под постером — врать зрителю. Такие карточки остаются без пилюли.
 */
fun ratingLabel(raw: String?): String? = ratingLabel(raw?.toDoubleOrNull())

/** То же для уже разобранной оценки (`rating.external` — усреднённая IMDb+КП). */
fun ratingLabel(value: Double?): String? =
    value?.takeIf { it > 0 }
        ?.let { String.format(Locale.US, "%.1f", it) }
