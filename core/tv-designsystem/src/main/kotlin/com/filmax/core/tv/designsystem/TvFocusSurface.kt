@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.filmax.core.tv.designsystem

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface

/**
 * Как элемент выглядит в фокусе: форма (по ней идут рамка и свечение) и масштаб.
 * [glowRadius] — скругление свечения; для круга — половина размера, свечение само его ограничит.
 * [pop] — масштаб с лёгким перелётом ([FxMotion.pop]), как у карточек; без него — ровный
 * [FxMotion.focus], как у кнопок, чипов и строк: перелёт на мелком элементе читается как дрожь.
 */
@Immutable
data class TvFocusStyle(val shape: Shape, val glowRadius: Dp, val focusedScale: Float, val pop: Boolean = false) {
    companion object {
        /** Постер, карточка 16:9, плитка. */
        val Card = TvFocusStyle(TvMetrics.CardShape, CARD_RADIUS, TvMetrics.FocusScale, pop = true)
        val Button = TvFocusStyle(TvMetrics.ButtonShape, CARD_RADIUS, TvMetrics.FocusScale)
        val Chip = TvFocusStyle(TvMetrics.ChipShape, CHIP_RADIUS, TvMetrics.FocusScale)

        /** Широкие строки и поля: увеличение 1.02, фокус держат рамка и свечение. */
        val Row = TvFocusStyle(TvMetrics.PanelShape, ROW_RADIUS, TvMetrics.WideFocusScale)

        /** Иконка-кнопка, аватар. */
        val Circle = TvFocusStyle(CircleShape, CIRCLE_RADIUS, TvMetrics.FocusScale)

        /** Абзац текста: рамка и свечение без увеличения — раздутый абзац читается как сбой. */
        val Text = TvFocusStyle(RoundedCornerShape(CARD_RADIUS), CARD_RADIUS, 1f)
    }
}

private val CARD_RADIUS = 12.dp
private val CHIP_RADIUS = 17.dp
private val ROW_RADIUS = 14.dp
private val CIRCLE_RADIUS = 999.dp

/**
 * Фокус без белой обводки и свечения — настройка «Настройки UI» в Профиле. Элемент в фокусе тогда
 * выделяют только увеличение и цвет подложки. Читают её [TvFocusSurface], [fxGlowPulse] и
 * [tvFocusOutline] — все места, где фокус рисует рамку или свечение.
 */
val LocalTvPlainFocus = staticCompositionLocalOf { false }

/**
 * Белая обводка элемента в фокусе для тех, кто рисует фокус сам, а не через [TvFocusSurface]:
 * та же рамка и так же отключается в «Настройках UI» ([LocalTvPlainFocus]).
 */
fun Modifier.tvFocusOutline(
    focused: Boolean,
    shape: Shape,
    width: Dp = TvMetrics.FocusBorderWidth,
): Modifier = composed {
    if (focused && !LocalTvPlainFocus.current) border(width, TvFocus, shape) else this
}

/** Цвета подложки и содержимого: без фокуса и в фокусе. */
@Immutable
data class TvSurfaceColors(
    val container: Color,
    val content: Color,
    val focusedContainer: Color = container,
    val focusedContent: Color = content,
) {
    companion object {
        /** Прозрачная подложка — у карточек цвет даёт картинка. */
        val Clear = TvSurfaceColors(container = Color.Transparent, content = Violet.text)
    }
}

/**
 * Фокусируемая поверхность — основа всех интерактивных элементов ТВ: кнопок, чипов, карточек,
 * строк, пунктов меню. Фокус описан здесь один раз и выглядит везде одинаково:
 *
 * - белая рамка 3dp — штатная рамка tv-material `Surface`;
 * - масштаб — свой, через graphicsLayer: у карточек с лёгким перелётом ([FxMotion.pop]), у
 *   кнопок, чипов и строк ровным [FxMotion.focus] ([TvFocusStyle.pop]); штатная анимация
 *   масштаба tv-material не настраивается;
 * - цветное свечение палитры, пульсирующее ([fxGlowPulse]), — внутри того же слоя, поэтому
 *   растёт вместе с элементом.
 *
 * Рамку и свечение отключают в «Настройках UI» ([LocalTvPlainFocus]): тогда фокус держат масштаб
 * и цвет подложки в фокусе.
 *
 * Состояние фокуса читается из [interactionSource] — передайте свой, если содержимому нужно
 * знать о фокусе (бегущая строка, смена цвета текста).
 */
// Компонент дизайн-системы: параметры — его публичный API, как у tv-material Surface, поверх
// которого он построен (onClick, modifier, вид, цвета, onLongClick, interactionSource, content).
@Suppress("LongParameterList")
@Composable
fun TvFocusSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: TvFocusStyle = TvFocusStyle.Card,
    colors: TvSurfaceColors = TvSurfaceColors.Clear,
    onLongClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable BoxScope.() -> Unit,
) {
    val focused by interactionSource.collectIsFocusedAsState()
    val reduced = LocalReducedMotion.current
    val outline = if (LocalTvPlainFocus.current) {
        Border.None
    } else {
        Border(BorderStroke(TvMetrics.FocusBorderWidth, TvFocus), shape = style.shape)
    }
    val scale by animateFloatAsState(
        targetValue = if (focused) style.focusedScale else 1f,
        animationSpec = when {
            reduced -> snap()
            style.pop -> FxMotion.pop()
            else -> FxMotion.focus()
        },
        label = "focusScale",
    )
    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier
            .zIndex(if (focused) 1f else 0f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .fxGlowPulse(active = focused, cornerRadius = style.glowRadius),
        shape = ClickableSurfaceDefaults.shape(style.shape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        border = ClickableSurfaceDefaults.border(focusedBorder = outline),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = colors.container,
            contentColor = colors.content,
            focusedContainerColor = colors.focusedContainer,
            focusedContentColor = colors.focusedContent,
            pressedContainerColor = colors.focusedContainer,
            pressedContentColor = colors.focusedContent,
        ),
        interactionSource = interactionSource,
        content = content,
    )
}
