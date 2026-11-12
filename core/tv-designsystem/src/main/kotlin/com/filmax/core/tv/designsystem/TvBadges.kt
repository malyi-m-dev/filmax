// Плашки и полосы ТВ-карточек: пилюля оценки, бейдж оценки с источником, текстовый бейдж и полоса
// прогресса. Сами карточки — в TvCards.
package com.filmax.core.tv.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.filmax.core.tv.designsystem.preview.TvComponentPreview

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

@TvComponentPreview
@Composable
private fun TvBadgesPreview() {
    FilmaxTvTheme {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TvRatingPill("8.1")
            TvRatingBadge(value = "7.9", source = "КиноПоиск", highlighted = true)
            TvRatingBadge(value = "7.6", source = "IMDb")
            TvBadge("Новинка")
            TvProgressBar(progress = 0.4f)
        }
    }
}
