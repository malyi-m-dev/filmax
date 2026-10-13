// Главная кнопка шапки, которая схлопывается в круг вместе со шапкой (подборка: «Смотреть» в
// раскрытой шапке → круг с иконкой в свёрнутой полосе).
package com.filmax.core.tv.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import kotlin.math.roundToInt

private val ExpandedHeight = 44.dp
private val CollapsedSize = 38.dp
private val ExpandedRadius = 12.dp
private val GlyphSize = 20.dp
private val ExpandedPadding = 22.dp

/** Текст гаснет за первую треть схлопывания (100 из 320 мс): ширина ещё едет, а слова уже нет. */
private const val TEXT_FADE_SHARE = 0.3f

/**
 * Кнопка главного действия, которая умеет схлопываться в круг с иконкой. [collapse] — доля
 * схлопывания 0..1 (0 — кнопка с подписью, 1 — круг 38dp). Читается только в фазах раскладки и
 * отрисовки: анимация шапки двигает кнопку, не перекомпоновывая её на каждом кадре.
 *
 * Фокус — общий для всех элементов ([TvFocusSurface]); схлопнутая кнопка остаётся фокусируемой.
 */
// Компонент дизайн-системы: параметры — его публичный API (подпись, иконка, действие, доля
// схлопывания и modifier), как у TvButton.
@Suppress("LongParameterList")
@Composable
fun TvCollapsibleButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    collapse: () -> Float,
    modifier: Modifier = Modifier,
) {
    val colors = LocalFxColors.current
    val shape = remember(collapse) { CollapsingShape(collapse) }
    val style = remember(shape) { TvFocusStyle(shape, ExpandedRadius, TvMetrics.FocusScale) }
    TvFocusSurface(
        onClick = onClick,
        modifier = modifier.collapsingSize(collapse),
        style = style,
        colors = TvSurfaceColors(container = colors.accent, content = colors.onAccent),
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
                // Содержимое всегда во всю ширину подписи — поверхность обрезает его своей формой.
                .wrapContentWidth(Alignment.Start, unbounded = true)
                .padding(horizontal = ExpandedPadding)
                .graphicsLayer { translationX = -collapseShift(collapse()).toPx() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Подпись остаётся в дереве и прозрачной — по ней кнопку называет озвучка и в круге.
            Icon(icon, contentDescription = null, modifier = Modifier.size(GlyphSize))
            Text(
                text,
                style = FxType.button,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.graphicsLayer { alpha = textAlpha(collapse()) },
            )
        }
    }
}

/** На сколько сдвинуть содержимое влево, чтобы иконка встала в центр круга. */
private fun collapseShift(progress: Float) = (ExpandedPadding - (CollapsedSize - GlyphSize) / 2) * progress

private fun textAlpha(progress: Float): Float = (1f - progress / TEXT_FADE_SHARE).coerceIn(0f, 1f)

/**
 * Размер кнопки по доле схлопывания: высота 44 → 38dp, ширина — от полной (подпись целиком) до
 * круга. Полная ширина берётся из intrinsic-измерения содержимого — подпись не переносится и
 * не сжимается, пока кнопка сужается.
 */
private fun Modifier.collapsingSize(collapse: () -> Float): Modifier = layout { measurable, _ ->
    val progress = collapse()
    val height = lerp(ExpandedHeight.toPx(), CollapsedSize.toPx(), progress).roundToInt()
    val fullWidth = measurable.maxIntrinsicWidth(height)
    val width = lerp(fullWidth.toFloat(), CollapsedSize.toPx(), progress).roundToInt()
    val placeable = measurable.measure(Constraints.fixed(width, height))
    layout(width, height) { placeable.place(0, 0) }
}

/** Скруглённый прямоугольник, который к концу схлопывания становится кругом. */
private class CollapsingShape(private val collapse: () -> Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val radius = lerp(with(density) { ExpandedRadius.toPx() }, size.minDimension / 2, collapse())
        return Outline.Rounded(RoundRect(size.toRect(), CornerRadius(radius)))
    }
}
