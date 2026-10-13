package com.filmax.core.tv.designsystem

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Заглушка — стекло тише карточек: место под постер видно, но внимание не забирает. */
private const val SKELETON_ALPHA = 0.06f
private const val SHINE_ALPHA = 0.08f

/** Блик ярче всего посередине полосы. */
private const val SHINE_PEAK = 0.5f

/**
 * Блик: полоса в 40% ширины проходит заглушку за 2.6 с, первый раз — через 0.6 с. Путь — в ширинах
 * полосы: от -1.2 (целиком левее заглушки) до 3.2 (целиком правее).
 */
private const val SHINE_WIDTH = 0.4f
private const val SHINE_FROM = -1.2f
private const val SHINE_TO = 3.2f
private const val SHINE_MS = 2_600
private const val SHINE_DELAY_MS = 600

private val CaptionWidth = 100.dp
private val CaptionHeight = 12.dp
private val CaptionShape = RoundedCornerShape(6.dp)

/**
 * Заглушка постера, пока страница догружается: прямоугольник [size] и полоска под подпись, по
 * которым проходит блик. Размер — как у [TvPosterCard], поэтому сетка не прыгает, когда вместо
 * заглушек встают карточки. При сниженной анимации — без блика.
 */
@Composable
fun TvPosterSkeleton(modifier: Modifier = Modifier, size: TvPosterSize = TvPosterSize.Grid) {
    val shine = rememberShine()
    Column(modifier.width(size.width)) {
        Box(
            Modifier
                .size(size.width, size.height)
                .clip(TvMetrics.PosterShape)
                .background(Color.White.copy(alpha = SKELETON_ALPHA))
                .shine(shine),
        )
        Box(
            Modifier
                .padding(top = 10.dp)
                .size(CaptionWidth, CaptionHeight)
                .background(Color.White.copy(alpha = SKELETON_ALPHA), CaptionShape),
        )
    }
}

/** Фаза блика 0..1; null — блика нет (сниженная анимация). */
@Composable
private fun rememberShine(): State<Float>? {
    if (LocalReducedMotion.current) return null
    val transition = rememberInfiniteTransition(label = "skeletonShine")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(SHINE_MS, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(SHINE_DELAY_MS),
        ),
        label = "skeletonShinePhase",
    )
}

/** Полоса блика по фазе; фаза читается при отрисовке — анимация не перекомпоновывает сетку. */
private fun Modifier.shine(phase: State<Float>?): Modifier {
    if (phase == null) return this
    return drawWithContent {
        drawContent()
        val band = size.width * SHINE_WIDTH
        val start = band * (SHINE_FROM + (SHINE_TO - SHINE_FROM) * phase.value)
        drawRect(
            brush = Brush.horizontalGradient(
                0f to Color.Transparent,
                SHINE_PEAK to Color.White.copy(alpha = SHINE_ALPHA),
                1f to Color.Transparent,
                startX = start,
                endX = start + band,
            ),
            topLeft = Offset(start, 0f),
            size = Size(band, size.height),
        )
    }
}
