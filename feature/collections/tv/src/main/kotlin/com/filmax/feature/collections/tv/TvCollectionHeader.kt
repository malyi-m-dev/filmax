// Шапка подборки: цветная полоса с кадром, название, число тайтлов и «Смотреть». Сворачивается
// 230 → 72dp, когда фокус уходит ниже первой строки сетки, — одной анимацией на всё.
package com.filmax.feature.collections.tv

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import androidx.tv.material3.Text
import com.filmax.core.tv.designsystem.FxMotion
import com.filmax.core.tv.designsystem.FxType
import com.filmax.core.tv.designsystem.LocalFxColors
import com.filmax.core.tv.designsystem.LocalReducedMotion
import com.filmax.core.tv.designsystem.TvBackHint
import com.filmax.core.tv.designsystem.TvButtonStyle
import com.filmax.core.tv.designsystem.TvCollapsibleButton
import com.filmax.core.tv.designsystem.TvKeyCapSize
import com.filmax.core.tv.designsystem.TvMetrics
import com.filmax.core.tv.designsystem.TvSurfaceContainerHigh
import com.filmax.core.tv.designsystem.fxEnter
import com.filmax.core.tv.designsystem.kenBurns
import com.filmax.core.ui.components.PosterImage
import kotlin.math.abs
import kotlin.math.roundToInt

// ── Геометрия из макета (S12 — раскрытая, S19 — свёрнутая) ───────────────────────────────
private val ExpandedHeight = 230.dp
private val CollapsedHeight = 72.dp
private val BackHintTop = 25.dp
private val OverlineTop = 64.dp
private val TitleExpandedTop = 84.dp
private val TitleCollapsedTop = 26.dp
private val TitleMaxWidth = 560.dp
private val CaptionGap = 8.dp
private val InlineGap = 14.dp
private val ActionsEnd = 40.dp
private val ActionsExpandedCenter = 136.dp
private val ActionsCollapsedCenter = 36.dp
private val CollapsedShadow = 10.dp

/** Затухание под свёрнутой полосой: предыдущая строка сетки уходит под неё мягко. */
private val PreviousRowFadeHeight = 120.dp
private const val PREVIOUS_ROW_FADE_SOLID = 0.45f

/** Название в свёрнутой полосе — 20sp из 42sp: масштабом слоя, без перерасчёта раскладки. */
private const val TITLE_COLLAPSED_SCALE = 20f / 42f
private const val COVER_ALPHA = 0.35f

/** Надзаголовок «Подборка» — белым на 80%: на цветной полосе акцентная точка и цвет терялись. */
private const val OVERLINE_ALPHA = 0.8f

/** Шапка появляется по очереди (шаг fxEnter — 60 мс): надзаголовок, название, подпись, кнопка. */
private const val OVERLINE_ENTER = 0
private const val TITLE_ENTER = 1
private const val CAPTION_ENTER = 2
private const val ACTIONS_ENTER = 3
private const val BAND_FADE_START = 0.3f

/** Надзаголовок гаснет за первые 120 мс сворачивания, а возвращается, когда полоса уже раскрылась. */
private const val OVERLINE_OUT_MS = 120
private const val OVERLINE_IN_DELAY_MS = 120

/** Полоса поверх сетки только в свёрнутом виде: раскрытая лежит под первой строкой постеров. */
private const val HEADER_OVER_GRID = 2f
private const val HEADER_UNDER_GRID = 0f

private val TitleStyle = FxType.hero.copy(
    fontSize = 42.sp,
    lineHeight = 42.sp,
    fontWeight = FontWeight.ExtraBold,
    letterSpacing = (-0.03).em,
)

/** Что шапка показывает: название, подпись с числом тайтлов, кадр и цвет полосы. */
@Immutable
internal data class CollectionHeaderModel(
    val title: String,
    val caption: String?,
    val cover: String?,
    val tint: Color,
)

/**
 * Шапка подборки. [collapsed] приходит из модели (фокус ниже первой строки), а всё движение — одна
 * `updateTransition`: высота полосы, масштаб и сдвиг названия, кроссфейд подписи, кнопка, тень.
 * Значения читаются в фазах раскладки и отрисовки — анимация не перекомпоновывает шапку.
 *
 * [onPlay] == null — смотреть нечего (подборка пуста), кнопки нет. [onFocused] — фокус пришёл на
 * кнопку шапки: модель раскрывает шапку.
 */
@Composable
internal fun CollectionHeader(
    model: CollectionHeaderModel,
    collapsed: Boolean,
    onPlay: (() -> Unit)?,
    onFocused: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val transition = updateTransition(collapsed, label = "collectionHeader")
    val progress = transition.collapseProgress()
    val overline = transition.overlineAlpha()
    val collapse = remember(progress) { { progress.value } }
    Box(modifier.fillMaxSize().zIndex(if (collapsed) HEADER_OVER_GRID else HEADER_UNDER_GRID)) {
        PreviousRowFade(collapse)
        HeaderBand(model, running = !collapsed, collapse)
        HeaderTexts(model, collapse, overline)
        if (onPlay != null) {
            // Белая кнопка: акцентная сливалась бы с цветной полосой подборки.
            TvCollapsibleButton(
                text = "Смотреть подряд",
                icon = Icons.Rounded.PlayArrow,
                onClick = onPlay,
                collapse = collapse,
                style = TvButtonStyle.Light,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = ActionsEnd)
                    .centeredAt { lerp(ActionsExpandedCenter, ActionsCollapsedCenter, collapse()) }
                    .fxEnter(ACTIONS_ENTER)
                    .onFocusChanged { if (it.hasFocus) onFocused() },
            )
        }
    }
}

/** Доля сворачивания 0..1. Сниженная анимация — сразу в конечное положение. */
@Composable
private fun Transition<Boolean>.collapseProgress(): State<Float> {
    val reduced = LocalReducedMotion.current
    return animateFloat(
        transitionSpec = { if (reduced) snap() else FxMotion.collapse() },
        label = "collapseProgress",
    ) { collapsed -> if (collapsed) 1f else 0f }
}

@Composable
private fun Transition<Boolean>.overlineAlpha(): State<Float> {
    val reduced = LocalReducedMotion.current
    return animateFloat(
        transitionSpec = { if (reduced) snap() else overlineSpec(collapsing = targetState) },
        label = "overlineAlpha",
    ) { collapsed -> if (collapsed) 0f else 1f }
}

private fun overlineSpec(collapsing: Boolean): FiniteAnimationSpec<Float> = if (collapsing) {
    keyframes { durationMillis = OVERLINE_OUT_MS }
} else {
    tween(FxMotion.FADE_MS, delayMillis = OVERLINE_IN_DELAY_MS)
}

/** Затухание сверху под свёрнутой полосой — поверх уехавшей вверх строки сетки. */
@Composable
private fun PreviousRowFade(collapse: () -> Float) {
    val background = LocalFxColors.current.background
    Box(
        Modifier
            .fillMaxWidth()
            .height(PreviousRowFadeHeight)
            .graphicsLayer { alpha = collapse() }
            .background(Brush.verticalGradient(PREVIOUS_ROW_FADE_SOLID to background, 1f to Color.Transparent)),
    )
}

/** Цветная полоса с кадром: высота 230 → 72dp, тень в свёрнутом виде, ken-burns — только в раскрытом. */
@Composable
private fun HeaderBand(model: CollectionHeaderModel, running: Boolean, collapse: () -> Float) {
    val background = LocalFxColors.current.background
    Box(
        Modifier
            .fillMaxWidth()
            .animatedHeight { lerp(ExpandedHeight, CollapsedHeight, collapse()) }
            .graphicsLayer {
                shadowElevation = CollapsedShadow.toPx() * collapse()
                shape = RectangleShape
                clip = true
            }
            .background(model.tint),
    ) {
        model.cover?.let { url ->
            PosterImage(
                url = url,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().monochromeCover().kenBurns(running),
                shape = RectangleShape,
                accentColor = TvSurfaceContainerHigh,
            )
        }
        // Низ раскрытой полосы растворяется в фоне экрана — первая строка постеров лежит на нём.
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer { alpha = 1f - collapse() }
                .background(Brush.verticalGradient(BAND_FADE_START to Color.Transparent, 1f to background)),
        )
    }
}

/**
 * «Назад», надзаголовок, название и подпись. Название уменьшается слоем и переезжает в строку
 * полосы; подпись под ним гаснет, а такая же справа от уменьшенного названия — проявляется.
 */
@Composable
private fun BoxScope.HeaderTexts(model: CollectionHeaderModel, collapse: () -> Float, overline: State<Float>) {
    val colors = LocalFxColors.current
    var titleWidth by remember { mutableIntStateOf(0) }
    // Подборки на ТВ открываются только с Главной — туда «назад» и ведёт. В свёрнутой полосе
    // подпись гаснет, остаётся значок, и название встаёт сразу за ним.
    TvBackHint(
        "Главная",
        Modifier.padding(start = TvMetrics.SafeHorizontal, top = BackHintTop),
        labelAlpha = { 1f - collapse() },
    )
    Text(
        "Подборка".uppercase(),
        style = FxType.overline,
        color = Color.White.copy(alpha = OVERLINE_ALPHA),
        modifier = Modifier
            .padding(start = TvMetrics.SafeHorizontal, top = OverlineTop)
            .fxEnter(OVERLINE_ENTER)
            .graphicsLayer { alpha = overline.value },
    )
    Text(
        model.title,
        style = TitleStyle,
        color = colors.text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .padding(start = TvMetrics.SafeHorizontal)
            .widthIn(max = TitleMaxWidth)
            .onSizeChanged { titleWidth = it.width }
            .graphicsLayer {
                val progress = collapse()
                val scale = lerp(1f, TITLE_COLLAPSED_SCALE, progress)
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0f, 0f)
                translationX = (TvKeyCapSize.Regular.size + InlineGap).toPx() * progress
                translationY = lerp(TitleExpandedTop, TitleCollapsedTop, progress).toPx()
            }
            .fxEnter(TITLE_ENTER),
    )
    model.caption?.let { caption ->
        HeaderCaption(caption, collapse) { progress ->
            val backWidth = TvKeyCapSize.Regular.size.roundToPx()
            val collapsedX = backWidth + InlineGap.roundToPx() + (titleWidth * TITLE_COLLAPSED_SCALE).roundToInt()
            val expanded = IntOffset(0, (TitleExpandedTop + TitleStyle.lineHeight.value.dp + CaptionGap).roundToPx())
            val collapsed = IntOffset(collapsedX + InlineGap.roundToPx(), TitleCollapsedTop.roundToPx())
            if (progress < CAPTION_SWAP) expanded else collapsed
        }
    }
}

/** Подпись меняет место не переездом, а кроссфейдом: на середине пути она невидима. */
private const val CAPTION_SWAP = 0.5f

@Composable
private fun HeaderCaption(
    caption: String,
    collapse: () -> Float,
    position: Density.(progress: Float) -> IntOffset,
) {
    Text(
        caption,
        style = FxType.metaRow,
        color = LocalFxColors.current.textSecondary,
        maxLines = 1,
        modifier = Modifier
            .padding(start = TvMetrics.SafeHorizontal)
            .offset { position(collapse()) }
            .fxEnter(CAPTION_ENTER)
            .graphicsLayer {
                val progress = collapse()
                // 1 → 0 к середине и 0 → 1 после неё: старое место гаснет, новое проявляется.
                alpha = abs(progress - CAPTION_SWAP) / CAPTION_SWAP
            },
    )
}

/**
 * Кадр в полосе — монохромом поверх цвета подборки (luminosity в макете): цвет полосы задаёт
 * подборка, а кадр даёт только светотень. Яркость кадра без цвета смешивается с полосой режимом
 * Luminosity; где режим не поддерживается (до Android 10), остаётся серый кадр на 35% — близко.
 */
private fun Modifier.monochromeCover(): Modifier = drawWithCache {
    val paint = Paint().apply {
        alpha = COVER_ALPHA
        colorFilter = MonochromeFilter
        blendMode = BlendMode.Luminosity
    }
    onDrawWithContent {
        drawIntoCanvas { canvas -> canvas.saveLayer(size.toRect(), paint) }
        drawContent()
        drawIntoCanvas { canvas -> canvas.restore() }
    }
}

private val MonochromeFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

/** Высота по доле анимации — в фазе раскладки, без рекомпозиции. */
private fun Modifier.animatedHeight(height: () -> Dp): Modifier = layout { measurable, constraints ->
    val heightPx = height().roundToPx()
    val placeable = measurable.measure(Constraints.fixed(constraints.maxWidth, heightPx))
    layout(placeable.width, heightPx) { placeable.place(0, 0) }
}

/** Ставит элемент так, чтобы его центр по вертикали был на [centerY] от верха. */
private fun Modifier.centeredAt(centerY: () -> Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout(placeable.width, placeable.height) {
        placeable.place(0, centerY().roundToPx() - placeable.height / 2)
    }
}
