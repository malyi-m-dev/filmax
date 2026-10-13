package com.filmax.core.tv.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.filmax.core.ui.components.PosterImage

/** Размер постера 2:3: в рядах — компактнее, в сетке каталога — крупнее. */
enum class TvPosterSize(val width: Dp, val height: Dp) {
    Rail(TvMetrics.PosterWidth, TvMetrics.PosterHeight),
    Grid(TvMetrics.GridPosterWidth, TvMetrics.GridPosterHeight),

    /** Результаты поиска под «Лучшим совпадением»: четыре в узкой правой колонке. */
    Compact(106.dp, 160.dp),
}

/**
 * Размер карточки 16:9. «Продолжить» несёт подпись на кадре, эпизод — под кадром: в ряду серий
 * название серии читается раньше картинки.
 */
enum class TvCardSize(val width: Dp, val height: Dp) {
    Continue(TvMetrics.ContinueWidth, TvMetrics.ContinueHeight),
    Episode(TvMetrics.EpisodeWidth, TvMetrics.EpisodeHeight),

    /** Новинки: крупный кадр, подпись под ним. */
    Wide(TvMetrics.WideCardWidth, TvMetrics.WideCardHeight),

    /** Серия в нижней панели плеера: панель низкая, а над рядом серий ещё стоят сезоны. */
    Panel(PanelCardWidth, PanelCardHeight),
}

private val PanelCardWidth = 196.dp
private val PanelCardHeight = 110.dp

/** Отступ подписи от постера. */
private val CaptionGap = 9.dp

/**
 * Карточка-постер 2:3 — ряды Главной, сетки Каталога и подборок. Подпись под постером:
 * название одной строкой (в фокусе — бегущей), мета — только в фокусе, чтобы ряд не рябил.
 * [rating] — готовая строка оценки («8.3»), null — без пилюли; [ratingHighlighted] красит её вторым
 * акцентом (высокая оценка). [rank] — крупный номер в углу постера (топ-10 «В тренде»).
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
    ratingHighlighted: Boolean = false,
    rank: Int? = null,
    size: TvPosterSize = TvPosterSize.Rail,
    onLongClick: (() -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Column(modifier.width(size.width).railDim(focused)) {
        TvFocusSurface(
            onClick = onClick,
            onLongClick = onLongClick,
            modifier = Modifier.size(size.width, size.height),
            interactionSource = interaction,
        ) {
            TvCardImage(posterUrl, title, TvMetrics.PosterShape)
            if (rating != null) {
                TvRatingPill(rating, Modifier.align(Alignment.TopEnd).padding(8.dp), highlighted = ratingHighlighted)
            }
            if (rank != null) RankNumeral(rank, Modifier.align(Alignment.BottomStart))
        }
        TvCardTitle(title, focused, Modifier.padding(top = CaptionGap))
        if (focused && !meta.isNullOrBlank()) {
            Text(meta, style = FxType.meta, color = LocalFxColors.current.textSecondary, maxLines = 1)
        }
    }
}

/** Затемнение снизу кадра под подписью «продолжить»: текст читается на любом кадре. */
private val CaptionScrim = Brush.verticalGradient(0.4f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.8f))

/**
 * Карточка 16:9 с прогрессом — «Продолжить просмотр», «Моё», серии сезона. [progress] — доля
 * 0..1, полоса акцентом по нижнему краю кадра; null — без полосы (новинки). [meta] несёт то,
 * ради чего карточка существует: «S4E7 · осталось 41 мин». [badge] — плашка акцентом в углу кадра («S4E7», «Сейчас»).
 * [fallbackLabel] — крупная подпись вместо кадра, если его нет (номер серии: у kino.pub кадр
 * серии часто пустой, а пустые плитки в ряду не отличить друг от друга).
 */
// Компонент дизайн-системы: параметры — его публичный API (см. TvPosterCard).
@Suppress("LongParameterList")
@Composable
fun TvProgressCard(
    title: String,
    meta: String?,
    posterUrl: String,
    progress: Float?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: TvCardSize = TvCardSize.Continue,
    badge: String? = null,
    fallbackLabel: String? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val captionInside = size == TvCardSize.Continue
    Column(modifier.width(size.width).railDim(focused)) {
        TvFocusSurface(
            onClick = onClick,
            onLongClick = onLongClick,
            modifier = Modifier.size(size.width, size.height),
            interactionSource = interaction,
        ) {
            if (posterUrl.isBlank() && fallbackLabel != null) {
                Text(
                    fallbackLabel,
                    style = FxType.screenTitle,
                    color = LocalFxColors.current.textDim,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                TvCardImage(posterUrl, title, TvMetrics.CardShape)
            }
            if (captionInside) InsideCaption(title, meta, focused)
            if (badge != null) TvBadge(badge, Modifier.align(Alignment.TopStart).padding(8.dp))
            if (progress != null) TvProgressBar(progress, Modifier.align(Alignment.BottomStart))
        }
        if (!captionInside) BelowCaption(title, meta, focused)
    }
}

/** Подпись под кадром: название и мета, обе одной строкой. */
@Composable
private fun BelowCaption(title: String, meta: String?, focused: Boolean) {
    TvCardTitle(title, focused, Modifier.padding(top = CaptionGap))
    if (!meta.isNullOrBlank()) {
        Text(meta, style = FxType.meta, color = LocalFxColors.current.textSecondary, maxLines = 1)
    }
}

@Composable
private fun BoxScope.InsideCaption(title: String, meta: String?, focused: Boolean) {
    Box(Modifier.matchParentSize().background(CaptionScrim))
    Column(Modifier.align(Alignment.BottomStart).padding(start = 12.dp, end = 12.dp, bottom = 14.dp)) {
        TvCardTitle(title, focused, style = FxType.titleOnImage)
        if (!meta.isNullOrBlank()) {
            Text(meta, style = FxType.metaOnImage, color = LocalFxColors.current.textSecondary, maxLines = 1)
        }
    }
}

/** Картинка карточки во всю площадь; пока грузится — градиент-заглушка тона стекла. */
@Composable
private fun TvCardImage(url: String, contentDescription: String, shape: Shape) {
    PosterImage(
        url = url,
        contentDescription = contentDescription,
        modifier = Modifier.fillMaxSize(),
        shape = shape,
        accentColor = TvSurfaceContainerHigh,
    )
}

/**
 * Пилюля рейтинга поверх постера. Высокая оценка ([highlighted]) — вторым акцентом палитры:
 * заметна в ряду, не споря с главным действием экрана; обычная — тёмной подложкой.
 */
@Composable
fun TvRatingPill(rating: String, modifier: Modifier = Modifier, highlighted: Boolean = true) {
    val colors = LocalFxColors.current
    Text(
        rating,
        style = FxType.pill,
        color = if (highlighted) colors.onAccent2 else Color.White,
        modifier = modifier
            .background(if (highlighted) colors.accent2 else PillScrim, RatingPillShape)
            .padding(horizontal = 7.dp, vertical = 2.dp),
    )
}

private val PillScrim = Color.Black.copy(alpha = 0.6f)

private val RatingBadgeShape = RoundedCornerShape(9.dp)

/**
 * Оценка с источником в деталях: «7.9 КиноПоиск», «7.6 IMDb». Главный источник ([highlighted]) —
 * вторым акцентом, остальные — стеклом.
 */
@Composable
fun TvRatingBadge(value: String, source: String, modifier: Modifier = Modifier, highlighted: Boolean = false) {
    val colors = LocalFxColors.current
    Row(
        modifier
            .background(if (highlighted) colors.accent2 else colors.surfaceHigh, RatingBadgeShape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(value, style = FxType.badgeValue, color = if (highlighted) colors.onAccent2 else colors.text)
        Text(
            source,
            style = FxType.metaOnImage,
            color = if (highlighted) colors.onAccent2 else colors.textSecondary,
            modifier = Modifier.padding(bottom = 1.dp),
        )
    }
}

/** Номер в топе — крупно у нижнего левого угла постера, с тенью, чтобы читался на любом кадре. */
@Composable
private fun RankNumeral(rank: Int, modifier: Modifier) {
    Text("$rank", style = FxType.rank, color = Color.White, modifier = modifier.padding(start = 8.dp, bottom = 4.dp))
}

private val RatingPillShape = RoundedCornerShape(6.dp)

/** Плашка на кадре: серия «S4E7», «Сейчас», «Премьера» — акцентом палитры. */
@Composable
fun TvBadge(text: String, modifier: Modifier = Modifier) {
    val colors = LocalFxColors.current
    Text(
        text,
        style = FxType.pill,
        color = colors.onAccent,
        modifier = modifier
            .background(colors.accent, RatingPillShape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/** Полоса прогресса по нижнему краю кадра: акцент по полупрозрачному белому треку. */
@Composable
fun TvProgressBar(progress: Float, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(ProgressHeight)
            .background(Color.White.copy(alpha = PROGRESS_TRACK_ALPHA)),
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(ProgressHeight)
                .background(LocalFxColors.current.accent),
        )
    }
}

private val ProgressHeight = 4.dp
private const val PROGRESS_TRACK_ALPHA = 0.2f

/**
 * Фокусируемый контейнер для своей плитки (папка, круг актёра): фокус как у карточек, форма —
 * по [shape]. Для постера и кадра 16:9 есть готовые [TvPosterCard] и [TvProgressCard].
 */
@Composable
fun TvFocusCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = TvMetrics.CardShape,
    focusedScale: Float = TvMetrics.FocusScale,
    content: @Composable () -> Unit,
) {
    TvFocusSurface(
        onClick = onClick,
        modifier = modifier,
        style = TvFocusStyle(shape = shape, glowRadius = 12.dp, focusedScale = focusedScale),
    ) {
        content()
    }
}
