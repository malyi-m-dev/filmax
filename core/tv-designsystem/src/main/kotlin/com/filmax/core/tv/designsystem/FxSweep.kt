package com.filmax.core.tv.designsystem

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.util.lerp
import kotlin.math.tan

/** Ширина полосы блика — доля ширины карточки. */
private const val BAND_FRACTION = 0.4f

/** Яркость середины полосы. */
private const val BAND_ALPHA = 0.22f

/** Наклон полосы — тангенс 20°: верх полосы впереди низа, блик идёт «косой». */
private val BandSkew = -tan(Math.toRadians(20.0)).toFloat()

/** Путь полосы в её ширинах: из-за левого края карточки за правый. */
private const val SWEEP_FROM = -1.2f
private const val SWEEP_TO = 3.2f

/** ease-in-out из макета: полоса разгоняется и тормозит у краёв, середину пролетает. */
private val SweepEasing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)

/**
 * Блик на карточке в фокусе: светлая косая полоса раз в 2.6 с проходит по кадру слева направо.
 * Рисуется поверх содержимого; обрезку по форме карточки даёт `clip` перед модификатором.
 *
 * Положение полосы читается при отрисовке — карточка в фокусе не перекомпонуется на каждом кадре.
 * Бесконечный переход объявляет себя таким (InfiniteAnimationPolicy): UI-тесты его не ждут. В
 * облегчённом режиме, при сниженной анимации и под панелью блика нет — как и другого фонового
 * движения.
 */
fun Modifier.fxFocusSweep(active: Boolean): Modifier = composed {
    val still = LocalReducedMotion.current || LocalLightEffects.current || LocalFxPaused.current
    if (!active || still) return@composed this
    val progress = rememberSweepProgress()
    drawWithCache {
        val band = size.width * BAND_FRACTION
        val brush = Brush.horizontalGradient(
            0f to Color.Transparent,
            0.5f to Color.White.copy(alpha = BAND_ALPHA),
            1f to Color.Transparent,
            startX = 0f,
            endX = band,
        )
        val bandSize = Size(band, size.height)
        // Наклон вокруг середины высоты: полоса по центру кадра стоит там, где её сдвиг.
        val skew = Matrix().apply { values[Matrix.SkewX] = BandSkew }
        val skewShift = -BandSkew * size.height / 2
        onDrawWithContent {
            drawContent()
            skew.values[Matrix.TranslateX] = lerp(SWEEP_FROM, SWEEP_TO, progress.value) * band + skewShift
            withTransform({ transform(skew) }) { drawRect(brush, size = bandSize) }
        }
    }
}

@Composable
private fun rememberSweepProgress(): State<Float> = rememberInfiniteTransition(label = "focusSweep").animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
        animation = tween(FxMotion.SWEEP_MS, easing = SweepEasing),
        repeatMode = RepeatMode.Restart,
        initialStartOffset = StartOffset(FxMotion.SWEEP_DELAY_MS),
    ),
    label = "focusSweepProgress",
)
