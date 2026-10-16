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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
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
import com.filmax.core.tv.designsystem.LocalTvPlainFocus
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
 * Капля: край со стороны движения быстро тянется к новой цели, хвост догоняет мягче, без
 * отскока ([TrailingEdge]). Пока хвост в пути, капля вытянута на обе цели — так же она перетекает и на
 * карточку профиля внизу. Рисуется слоем во всю колонку под целями; в фокусе — чуть крупнее
 * ([NAV_FOCUS_SCALE]) и со свечением, как фокус остального интерфейса.
 */
@Composable
internal fun NavDropLayer(anchors: NavDropAnchors, target: NavDropTarget, modifier: Modifier) {
    val bounds = anchors.boundsOf(target.index)
    val drop = rememberLiquidDrop(target.index, target.focused, bounds)
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
    // Капля рисуется в фазе отрисовки: границы пунктов приходят после раскладки, и капля,
    // поставленная раскладкой, на кадр отставала бы от пункта, когда меню раскрывается или
    // закрывается и пункты разом сдвигаются.
    val edges = { drop.edges(target.index, target.focused, anchors.boundsOf(target.index)) }
    Box(modifier.graphicsLayer { this.alpha = alpha }.dropShape(target.focused, focusScale, edges)) {
        if (target.glowing && bounds != null) DropGlow(bounds, focusScale, edges)
        // Узел по границам капли — для UI-теста: по нему видно, на ком она стоит.
        Box(
            Modifier
                .matchParentSize()
                .layout { measurable, constraints ->
                    val current = edges()
                    val height = (current.bottom - current.top).roundToInt().coerceAtLeast(0)
                    val placeable = measurable.measure(Constraints.fixed(constraints.maxWidth, height))
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        placeable.place(0, current.top.roundToInt())
                    }
                }
                .testTag(NAV_DROP_TAG),
        )
    }
}

/**
 * Свечение под каплей — размером с цель: размер цели постоянен, и размытая текстура свечения
 * считается один раз ([fxGlowPulse]), а не на каждом кадре перетекания. Но едет оно за центром
 * капли ([edges], сдвиг слоя при отрисовке): стоя на цели сразу, оно горело на пункте, до которого
 * капля ещё не дотекла.
 */
@Composable
private fun BoxScope.DropGlow(bounds: Rect, focusScale: State<Float>, edges: () -> DropEdges) {
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
                val current = edges()
                translationY = (current.top + current.bottom) / 2 - bounds.center.y
                scaleX = focusScale.value
                scaleY = focusScale.value
            }
            .fxGlowPulse(active = true, cornerRadius = DropRadius),
    )
}

/**
 * Заливка и рамка капли поверх свечения, по краям [edges], прочитанным при отрисовке. Масштаб —
 * тоже при отрисовке, вокруг центра капли: границы капли остаются границами цели.
 */
@Composable
private fun Modifier.dropShape(focused: Boolean, focusScale: State<Float>, edges: () -> DropEdges): Modifier {
    val colors = LocalFxColors.current
    // Рамку капли, как и всю рамку фокуса, отключают в «Настройках UI».
    val outlined = focused && !LocalTvPlainFocus.current
    return drawWithContent {
        drawContent()
        val current = edges()
        val dropSize = Size(size.width, (current.bottom - current.top).coerceAtLeast(0f))
        val topLeft = Offset(0f, current.top)
        val radius = CornerRadius(minOf(DropRadius.toPx(), dropSize.height / 2, dropSize.width / 2))
        scale(focusScale.value, pivot = topLeft + Offset(dropSize.width / 2, dropSize.height / 2)) {
            drawRoundRect(colors.accent, topLeft, dropSize, radius)
            if (outlined) {
                drawRoundRect(TvFocus, topLeft, dropSize, radius, style = Stroke(TvMetrics.FocusBorderWidth.toPx()))
            }
        }
    }
}

/**
 * Верхний и нижний края капли, пиксели от верха колонки, — каждый на своей пружине. [index] —
 * цель, к которой капля шла последней (null — ещё ни к какой); [focused] — было ли меню тогда
 * раскрыто; [flowing] — перетекание к цели ещё не закончилось: прерванное сдвигом цели, оно
 * продолжается к её новому месту.
 */
private class LiquidDrop(
    val top: Animatable<Float, AnimationVector1D>,
    val bottom: Animatable<Float, AnimationVector1D>,
) {
    var index: Int? = null
    var focused = false
    var flowing by mutableStateOf(false)

    /**
     * Где рисовать каплю. Пока она не перетекает, — прямо по границам цели [index], прочитанным в
     * фазе раскладки: раскрытие меню двигает пункты каждый кадр, и капля идёт с ними без отставания.
     * Перетекание, которое ещё не запущено ([willFlow]), — тоже по пружинам: иначе капля на кадр
     * прыгала на новую цель и возвращалась назад, чтобы оттуда перетечь.
     */
    fun edges(index: Int, focused: Boolean, bounds: Rect?): DropEdges =
        if (flowing || bounds == null || willFlow(index, focused)) {
            DropEdges(top.value, bottom.value)
        } else {
            DropEdges(bounds.top, bounds.bottom)
        }

    /**
     * Пульт перевёл каплю по раскрытому меню на другую цель. Эффект, который запускает пружины,
     * стартует уже после первого кадра с новой целью, поэтому решение нужно и при отрисовке.
     */
    fun willFlow(index: Int, focused: Boolean): Boolean =
        this.index != null && this.index != index && this.focused && focused
}

/** Верхний и нижний края капли, пиксели от верха колонки. */
private data class DropEdges(val top: Float, val bottom: Float)

/**
 * Капля перетекает, только когда пульт ведёт её по раскрытому меню ([focused]) с пункта на пункт.
 * Раскрытие и закрытие меню её не переносят: закрытое меню отмечает открытый раздел, и капля с
 * пункта в фокусе сразу встаёт на него, а не течёт через соседей. Сдвинулась сама цель (меню
 * раскрывается, пункты растут) — капля стоит на ней без пружины.
 */
@Composable
private fun rememberLiquidDrop(index: Int, focused: Boolean, bounds: Rect?): LiquidDrop {
    val drop = remember { LiquidDrop(Animatable(0f), Animatable(0f)) }
    LaunchedEffect(index, focused, bounds) {
        val target = bounds ?: return@LaunchedEffect
        // Перетекание, прерванное сдвигом цели, продолжается к её новому месту.
        val flows = drop.willFlow(index, focused) || (drop.flowing && drop.index == index && focused)
        drop.index = index
        drop.focused = focused
        if (!flows) {
            drop.flowing = false
            drop.top.snapTo(target.top)
            drop.bottom.snapTo(target.bottom)
            return@LaunchedEffect
        }
        drop.flowing = true
        val movingDown = target.top > drop.top.value
        val topEdge = launch { drop.top.animateTo(target.top, if (movingDown) TrailingEdge else LeadingEdge) }
        val bottomEdge = launch { drop.bottom.animateTo(target.bottom, if (movingDown) LeadingEdge else TrailingEdge) }
        topEdge.join()
        bottomEdge.join()
        drop.flowing = false
    }
    return drop
}

private val LeadingEdge = spring<Float>(dampingRatio = 0.85f, stiffness = Spring.StiffnessMedium)

/**
 * Хвост — без отскока. Пружиной с покачиванием он пролетал цель на долю пути: с профиля на «Моё»
 * это десятки пикселей, и капля на несколько кадров сплющивалась в полоску над пунктом — на
 * глаз как подлагивание.
 */
private val TrailingEdge = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
