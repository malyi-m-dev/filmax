// Капля бокового меню — общая подсветка пунктов разделов и карточки профиля. Ведётся по
// замеренным границам цели, поэтому перетекает и на профиль, хотя тот стоит отдельно от пунктов.
package com.filmax.app.tv.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.filmax.core.tv.designsystem.FxMotion
import com.filmax.core.tv.designsystem.LocalFxColors
import com.filmax.core.tv.designsystem.TvFocus
import com.filmax.core.tv.designsystem.TvMetrics
import com.filmax.core.tv.designsystem.fxGlowPulse
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Номер карточки профиля среди целей капли — сразу после пунктов разделов. */
internal val PROFILE_INDEX = SECTIONS.size

/** Тег слоя капли: по его границам UI-тест проверяет, на ком она стоит. */
internal const val NAV_DROP_TAG = "navDrop"

private val DropRadius = 14.dp

/**
 * Границы целей капли — пунктов и карточки профиля — относительно колонки меню. Замеряются при
 * раскладке: высота карточки профиля и её место внизу раскрытой панели заранее неизвестны.
 */
@Stable
internal class NavDropAnchors {
    private val targets = mutableStateMapOf<Int, Rect>()
    private var container by mutableStateOf(Rect.Zero)

    /** Модификатор колонки, внутри которой живут цели и рисуется капля. */
    val containerModifier: Modifier = Modifier.onGloballyPositioned { container = it.boundsInWindow() }

    /** Модификатор цели с номером [index]. */
    fun target(index: Int): Modifier = Modifier.onGloballyPositioned { targets[index] = it.boundsInWindow() }

    /** Границы цели внутри колонки; null — она ещё не размещена. */
    fun boundsOf(index: Int): Rect? = targets[index]?.translate(-container.left, -container.top)
}

/**
 * На ком капля: номер цели, видна ли она и в фокусе ли (в раскрытом меню капля — фокус: белая
 * рамка поверх заливки). [glowing] — под каплей горит пульсирующее свечение фокуса; только когда
 * меню раскрылось до конца — пока панель растёт, у цели меняется размер, а свечение пришлось бы
 * заново размывать на каждом кадре.
 */
internal data class NavDropTarget(val index: Int, val visible: Boolean, val focused: Boolean, val glowing: Boolean)

/** Масштаб пункта меню в фокусе — мягче карточек: пункт во всю ширину панели. */
internal const val NAV_FOCUS_SCALE = 1.03f

/**
 * Капля: край со стороны движения быстро тянется к новой цели, хвост догоняет мягче и чуть
 * покачивается. Пока хвост в пути, капля вытянута на обе цели — так же она перетекает и на
 * карточку профиля внизу. Рисуется слоем во всю колонку под целями; в фокусе — чуть крупнее
 * ([NAV_FOCUS_SCALE]) и со свечением, как фокус остального интерфейса.
 */
@Composable
internal fun NavDropLayer(anchors: NavDropAnchors, target: NavDropTarget, modifier: Modifier) {
    val bounds = anchors.boundsOf(target.index)
    val drop = rememberLiquidDrop(top = bounds?.top ?: 0f, bottom = bounds?.bottom ?: 0f)
    val alpha by animateFloatAsState(
        targetValue = if (target.visible && bounds != null) 1f else 0f,
        animationSpec = FxMotion.focus(),
        label = "dropAlpha",
    )
    val focusScale = animateFloatAsState(
        targetValue = if (target.focused) NAV_FOCUS_SCALE else 1f,
        animationSpec = FxMotion.focus(),
        label = "dropScale",
    )
    Box(modifier.graphicsLayer { this.alpha = alpha }) {
        if (target.glowing && bounds != null) DropGlow(bounds, focusScale)
        Box(
            Modifier
                .matchParentSize()
                .layout { measurable, constraints ->
                    val height = (drop.bottom.value - drop.top.value).roundToInt().coerceAtLeast(0)
                    val placeable = measurable.measure(Constraints.fixed(constraints.maxWidth, height))
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        placeable.place(0, drop.top.value.roundToInt())
                    }
                }
                .testTag(NAV_DROP_TAG)
                .dropShape(target.focused, focusScale),
        )
    }
}

/**
 * Свечение под каплей — на самой цели, а не на перетекающей капле: размер цели постоянен, и
 * размытая текстура свечения считается один раз ([fxGlowPulse]), а не на каждом кадре перетекания.
 */
@Composable
private fun BoxScope.DropGlow(bounds: Rect, focusScale: State<Float>) {
    Box(
        Modifier
            .matchParentSize()
            .layout { measurable, constraints ->
                val placeable = measurable.measure(
                    Constraints.fixed(bounds.width.roundToInt(), bounds.height.roundToInt()),
                )
                layout(constraints.maxWidth, constraints.maxHeight) {
                    placeable.place(bounds.left.roundToInt(), bounds.top.roundToInt())
                }
            }
            .graphicsLayer {
                scaleX = focusScale.value
                scaleY = focusScale.value
            }
            .fxGlowPulse(active = true, cornerRadius = DropRadius),
    )
}

/** Заливка и рамка капли. Масштаб — при отрисовке: границы капли остаются границами цели. */
@Composable
private fun Modifier.dropShape(focused: Boolean, focusScale: State<Float>): Modifier {
    val colors = LocalFxColors.current
    return drawBehind {
        val radius = CornerRadius(minOf(DropRadius.toPx(), size.height / 2, size.width / 2))
        scale(focusScale.value) {
            drawRoundRect(colors.accent, Offset.Zero, size, radius)
            if (focused) {
                drawRoundRect(TvFocus, Offset.Zero, size, radius, style = Stroke(TvMetrics.FocusBorderWidth.toPx()))
            }
        }
    }
}

/** Верхний и нижний края капли, пиксели от верха колонки, — каждый на своей пружине. */
private class LiquidDrop(
    val top: Animatable<Float, AnimationVector1D>,
    val bottom: Animatable<Float, AnimationVector1D>
)

@Composable
private fun rememberLiquidDrop(top: Float, bottom: Float): LiquidDrop {
    val drop = remember { LiquidDrop(Animatable(top), Animatable(bottom)) }
    LaunchedEffect(top, bottom) {
        // Первая цель — встать сразу, без перелёта от верха колонки.
        if (drop.top.value == 0f && drop.bottom.value == 0f) {
            drop.top.snapTo(top)
            drop.bottom.snapTo(bottom)
            return@LaunchedEffect
        }
        val movingDown = top > drop.top.value
        launch { drop.top.animateTo(top, if (movingDown) TrailingEdge else LeadingEdge) }
        launch { drop.bottom.animateTo(bottom, if (movingDown) LeadingEdge else TrailingEdge) }
    }
    return drop
}

private val LeadingEdge = spring<Float>(dampingRatio = 0.85f, stiffness = Spring.StiffnessMedium)
private val TrailingEdge = spring<Float>(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow)
