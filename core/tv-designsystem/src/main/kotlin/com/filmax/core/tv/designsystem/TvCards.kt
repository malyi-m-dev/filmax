package com.filmax.core.tv.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.filmax.core.ui.components.PosterImage

/**
 * Размер постера 2:3: в рядах — компактнее, в сетке каталога — крупнее. [cornerRadius] —
 * скругление постера: мелкому постеру карточное 12dp велико.
 */
enum class TvPosterSize(val width: Dp, val height: Dp, val cornerRadius: Dp = PosterRadius) {
    Rail(TvMetrics.PosterWidth, TvMetrics.PosterHeight),
    Grid(TvMetrics.GridPosterWidth, TvMetrics.GridPosterHeight),

    /** Результаты поиска под «Лучшим совпадением»: четыре в узкой правой колонке. */
    Compact(106.dp, 160.dp, 10.dp),
}

private val PosterRadius = 12.dp

/**
 * Что постер показывает помимо картинки. [Always] — название под постером и пилюля оценки всегда,
 * мета — в фокусе (ряды, каталог). [OnFocus] — название, мета и оценка только у карточки в фокусе;
 * место под названием занято всегда, и сетка не прыгает (подборка). [None] — чистый постер (мелкие
 * результаты поиска: подписи на 106dp не читаются, тайтл узнают по обложке).
 */
enum class TvPosterDetails { Always, OnFocus, None }

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

    /** Сетка «Продолжить» в «Моё»: три колонки по 264dp, подпись под кадром. */
    Grid(264.dp, 148.dp),

    /** История в «Моё»: четыре колонки по 195dp — день просмотров помещается в ряд. */
    Small(195.dp, 110.dp),
}

private val PanelCardWidth = 196.dp
private val PanelCardHeight = 110.dp

/** Отступ подписи от постера. */
private val CaptionGap = 9.dp

/**
 * Карточка-постер 2:3 — ряды Главной, сетки Каталога и подборок. Подпись под постером:
 * название одной строкой (в фокусе — бегущей), мета — только в фокусе, чтобы ряд не рябил.
 * [rating] — готовая строка оценки («8.3»), null — без пилюли; [ratingHighlighted] красит её вторым
 * акцентом (высокая оценка), [ratingAlignment] — угол пилюли (в каталоге — левый верхний).
 * [rank] — крупный номер в углу постера (топ-10 «В тренде»). [details] — что видно без фокуса.
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
    ratingAlignment: Alignment = Alignment.TopEnd,
    details: TvPosterDetails = TvPosterDetails.Always,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = remember(size) { RoundedCornerShape(size.cornerRadius) }
    val style = remember(size) { TvFocusStyle(shape, size.cornerRadius, TvMetrics.FocusScale) }
    val showRating = details == TvPosterDetails.Always || details == TvPosterDetails.OnFocus && focused
    Column(modifier.width(size.width).tvNeighborDim(focused)) {
        TvFocusSurface(
            onClick = onClick,
            onLongClick = onLongClick,
            modifier = Modifier.size(size.width, size.height),
            style = style,
            interactionSource = interaction,
        ) {
            TvCardImage(posterUrl, title, shape)
            if (rating != null && showRating) {
                TvRatingPill(rating, Modifier.align(ratingAlignment).padding(8.dp), highlighted = ratingHighlighted)
            }
            if (rank != null) RankNumeral(rank, Modifier.align(Alignment.BottomStart))
            FocusSweep(focused, TvMetrics.PosterShape)
        }
        if (details != TvPosterDetails.None) PosterCaption(title, meta, focused, details)
    }
}

/**
 * Подпись постера. В [TvPosterDetails.OnFocus] название без фокуса прозрачно, но место держит:
 * строка сетки одной высоты, с фокусом и без.
 */
@Composable
private fun PosterCaption(title: String, meta: String?, focused: Boolean, details: TvPosterDetails) {
    val hidden = details == TvPosterDetails.OnFocus && !focused
    TvCardTitle(
        title,
        focused,
        Modifier.padding(top = CaptionGap).graphicsLayer { alpha = if (hidden) 0f else 1f },
        style = if (focused) FxType.cardTitle else RestingCardTitle,
    )
    if (focused && !meta.isNullOrBlank()) {
        val color = LocalFxColors.current.text.copy(alpha = META_ALPHA)
        Text(meta, style = FxType.metaOnImage, color = color, maxLines = 1)
    }
}

/**
 * Название под постером без фокуса — на ступень легче: в ряду читается спокойно, а карточка в
 * фокусе выделяется ещё и начертанием.
 */
private val RestingCardTitle = FxType.cardTitle.copy(fontWeight = FontWeight.Medium)

/** Мета под постером в фокусе — тише названия, как в макете. */
private const val META_ALPHA = 0.6f

/** Блик поверх кадра в фокусе, обрезанный по форме карточки. */
@Composable
private fun BoxScope.FocusSweep(focused: Boolean, shape: Shape) {
    if (focused) Box(Modifier.matchParentSize().clip(shape).fxFocusSweep(active = true))
}

/** Затемнение снизу кадра под подписью «продолжить»: текст читается на любом кадре. */
private val CaptionScrim = Brush.verticalGradient(0.4f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.8f))

/**
 * Карточка 16:9 с прогрессом — «Продолжить просмотр», «Моё», серии сезона. [progress] — доля
 * 0..1, полоса акцентом по нижнему краю кадра; null — без полосы (новинки). [meta] несёт то,
 * ради чего карточка существует: «S4E7 · осталось 41 мин». [badge] — плашка акцентом в углу кадра («S4E7», «Сейчас»).
 * [fallbackLabel] — крупная подпись вместо кадра, если его нет (номер серии: у kino.pub кадр
 * серии часто пустой, а пустые плитки в ряду не отличить друг от друга). [current] — то, что
 * сейчас идёт (играющая серия в панели плеера): кадр обведён акцентом, мета — цветом акцента, и
 * среди приглушённых соседей карточка остаётся ярче — её ищут глазами первой.
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
    current: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val captionInside = size == TvCardSize.Continue
    val dimmed = if (current) CURRENT_DIMMED_ALPHA else TvMetrics.DimmedAlpha
    Column(modifier.width(size.width).tvNeighborDim(focused, dimmed)) {
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
            FocusSweep(focused, TvMetrics.CardShape)
            if (current) CurrentOutline()
        }
        if (!captionInside) BelowCaption(title, meta, focused, current)
    }
}

/** Играющая карточка среди приглушённых соседей — ярче их (0.85 против 0.6). */
private const val CURRENT_DIMMED_ALPHA = 0.85f
private val CurrentOutlineWidth = 2.dp

/** Обводка играющей карточки акцентом — внутри поверхности: растёт с фокусом, под белой рамкой. */
@Composable
private fun BoxScope.CurrentOutline() {
    Box(Modifier.matchParentSize().border(CurrentOutlineWidth, LocalFxColors.current.accent, TvMetrics.CardShape))
}

/** Подпись под кадром: название и мета, обе одной строкой; у играющей мета — цветом акцента. */
@Composable
private fun BelowCaption(title: String, meta: String?, focused: Boolean, current: Boolean) {
    val colors = LocalFxColors.current
    TvCardTitle(title, focused, Modifier.padding(top = CaptionGap))
    if (!meta.isNullOrBlank()) {
        Text(meta, style = FxType.meta, color = if (current) colors.accentText else colors.textSecondary, maxLines = 1)
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

/**
 * Полоса прогресса по нижнему краю кадра: акцент по полупрозрачному белому треку. При появлении
 * карточки заливка вырастает от левого края за 1.1 с — масштабом слоя, без перемера раскладки;
 * при сниженной анимации стоит сразу.
 */
@Composable
fun TvProgressBar(progress: Float, modifier: Modifier = Modifier) {
    val grow = rememberGrow()
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
                .graphicsLayer {
                    scaleX = grow.value
                    transformOrigin = GrowOrigin
                }
                .background(LocalFxColors.current.accent),
        )
    }
}

/** Доля выросшей заливки, 0→1 один раз при появлении. */
@Composable
private fun rememberGrow(): State<Float> {
    val reduced = LocalReducedMotion.current
    val grow = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) { grow.animateTo(1f, FxMotion.grow()) }
    return grow.asState()
}

private val GrowOrigin = TransformOrigin(0f, 0.5f)
private val ProgressHeight = 4.dp
private const val PROGRESS_TRACK_ALPHA = 0.25f

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
        style = TvFocusStyle(shape = shape, glowRadius = 12.dp, focusedScale = focusedScale, pop = true),
    ) {
        content()
    }
}
