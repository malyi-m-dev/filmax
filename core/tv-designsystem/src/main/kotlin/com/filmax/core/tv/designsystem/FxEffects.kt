// Эффекты редизайна: появление контента, ken-burns фона, стекло и прокрутка с опорной точкой.
// Свечение фокуса — в FxGlow.kt. Тайминги — только из FxMotion.
@file:OptIn(ExperimentalFoundationApi::class)

package com.filmax.core.tv.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.relocation.BringIntoViewResponder
import androidx.compose.foundation.relocation.bringIntoViewResponder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.InfiniteAnimationPolicy
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.delay
import kotlin.coroutines.coroutineContext
import kotlin.math.abs
import kotlin.math.roundToInt

/** Сдвиг снизу, с которого появляется контент. */
private val EnterOffset = 16.dp

/**
 * Появление по очереди: alpha 0→1 и подъём на 16dp, каждый следующий элемент на 60 мс позже.
 * [index] — порядковый номер элемента в группе (0 — первый).
 */
fun Modifier.fxEnter(index: Int): Modifier = composed {
    val progress = remember { Animatable(0f) }
    val reduced = LocalReducedMotion.current
    LaunchedEffect(Unit) {
        if (reduced) {
            progress.snapTo(1f)
        } else {
            delay(index * FxMotion.ENTER_STAGGER_MS.toLong())
            progress.animateTo(1f, tween(FxMotion.ENTER_MS, easing = FxEasing.Standard))
        }
    }
    graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * EnterOffset.toPx()
    }
}

private const val KEN_BURNS_SCALE_FROM = 1.03f
private const val KEN_BURNS_SCALE_RANGE = 0.11f
private const val KEN_BURNS_SHIFT_X = -0.025f
private const val KEN_BURNS_SHIFT_Y = -0.015f

/**
 * Ken-burns фона hero и деталей: медленное приближение 1.03→1.14 со сдвигом влево-вверх, 16 с
 * туда и обратно. [running] = false ставит на паузу (hero без фокуса); пауза и под панелью
 * ([LocalFxPaused]). Пауза — это остановка анимации, а не застывшее значение бесконечного
 * перехода: пока фон стоит, приложение не рисует ни одного лишнего кадра. Продолжает с того же
 * места и в ту же сторону. В облегчённом режиме и при сниженной анимации фон неподвижен.
 */
fun Modifier.kenBurns(running: Boolean = true): Modifier = composed {
    val still = LocalReducedMotion.current || LocalLightEffects.current
    val active = running && !still && !LocalFxPaused.current
    val phase = remember { Animatable(0f) }
    val forward = remember { mutableStateOf(true) }
    LaunchedEffect(active) {
        if (!active) return@LaunchedEffect
        // Бесконечная анимация объявляет себя такой (InfiniteAnimationPolicy), как InfiniteTransition:
        // UI-тесты не ждут её конца, а в приложении поведение то же.
        runInfinite {
            while (true) {
                val target = if (forward.value) 1f else 0f
                val duration = (FxMotion.KEN_BURNS_MS * abs(target - phase.value)).roundToInt()
                phase.animateTo(target, tween(duration, easing = FastOutSlowInEasing))
                forward.value = !forward.value
            }
        }
    }
    graphicsLayer {
        val progress = if (still) 0f else phase.value
        val scale = KEN_BURNS_SCALE_FROM + KEN_BURNS_SCALE_RANGE * progress
        scaleX = scale
        scaleY = scale
        translationX = KEN_BURNS_SHIFT_X * size.width * progress
        translationY = KEN_BURNS_SHIFT_Y * size.height * progress
    }
}

/** Бесконечная операция под политикой окружения: в тестах Compose она не мешает простою. */
private suspend fun runInfinite(block: suspend () -> Unit) {
    val policy = coroutineContext[InfiniteAnimationPolicy]
    if (policy == null) block() else policy.onInfiniteOperation { block() }
}

/**
 * Неподвижный слой поверх контента — затемнения под текст, подсветка палитры. [onDraw] рисуется
 * один раз в offscreen-слой: HWUI держит его готовой текстурой и не пересчитывает градиенты на
 * каждом кадре, пока не сменился размер или палитра. Иначе три-четыре полноэкранных градиента
 * поверх кадра заново закрашивали бы экран при каждом движении под ними — на слабом ТВ-GPU это
 * и есть рывки: упор в заливку пикселей, а не в код.
 */
fun Modifier.fxStaticOverlay(onDraw: DrawScope.() -> Unit): Modifier = drawWithCache {
    val layer = obtainGraphicsLayer().apply {
        compositingStrategy = CompositingStrategy.Offscreen
        record { onDraw() }
    }
    onDrawWithContent {
        drawContent()
        drawLayer(layer)
    }
}

/**
 * Стеклянная панель: полупрозрачный белый поверх того, что под ней, и тонкая светлая обводка.
 * Размыть то, что лежит под панелью, Compose не умеет — стекло держится на прозрачности.
 */
@Composable
fun Modifier.fxGlass(shape: Shape, fill: Color = LocalFxColors.current.surface): Modifier =
    background(fill, shape).border(1.dp, LocalFxColors.current.outline, shape)

/**
 * Прокрутка к «опорной точке», как в лентах Google TV: элемент в фокусе встаёт краем на
 * [pivotPx] от начала контейнера — двигается лента, а не фокус. Высокий блок (больше двух третей
 * контейнера — hero) прижимается к самому началу: на опорной точке его верх ушёл бы за край.
 * Ряд постеров с подписями бывает выше половины экрана, поэтому порог — две трети.
 * У краёв списка Lazy-контейнер сам ограничит скролл.
 *
 * Чтобы на опорную точку вставал ряд целиком, а не карточка внутри него, ряд помечается
 * [bringIntoViewAsWhole].
 */
class PivotBringIntoViewSpec(private val pivotPx: Float) : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float =
        if (size > containerSize * TALL_BLOCK_FRACTION) offset else offset - pivotPx
}

private const val TALL_BLOCK_FRACTION = 2f / 3f

/**
 * Прокрутка к элементу внутри этого блока показывает блок целиком: запрос «покажи карточку»
 * от вложенного фокуса уходит к родительскому списку как «покажи весь ряд». Так ряд встаёт на
 * опорную точку заголовком, а не серединой.
 */
fun Modifier.bringIntoViewAsWhole(): Modifier = composed {
    val responder = remember { WholeBlockResponder() }
    onSizeChanged { responder.size = it }.bringIntoViewResponder(responder)
}

private class WholeBlockResponder : BringIntoViewResponder {
    var size: IntSize = IntSize.Zero

    override fun calculateRectForParent(localRect: Rect): Rect = Rect(Offset.Zero, size.toSize())

    // Внутри блока прокручивать нечего — горизонтальный ряд уже показал карточку сам.
    override suspend fun bringChildIntoView(localRect: () -> Rect?) = Unit
}
