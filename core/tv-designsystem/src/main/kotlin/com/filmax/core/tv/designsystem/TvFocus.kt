package com.filmax.core.tv.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Делает элемент управляемым с пульта: фокусируемым, кликабельным (OK/DPAD-центр)
 * и подсвечивает его в фокусе — масштаб + жёлтая обводка [LocalTvFocusColor], повторяя
 * `.focus-ring.focused` из макета.
 *
 * @param big меньшее увеличение (1.04 вместо 1.08) — для крупных элементов (кнопки/чипы).
 * @param focusRequester опциональный реквестер для авто-фокуса при появлении.
 */
@Composable
fun Modifier.tvFocusable(
    shape: Shape,
    onClick: () -> Unit,
    big: Boolean = false,
    enabled: Boolean = true,
    focusRequester: FocusRequester? = null,
): Modifier {
    var focused by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (!focused) 1f else if (big) 1.04f else 1.08f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tvFocusScale",
    )
    val ringColor by animateColorAsState(
        targetValue = if (focused) LocalTvFocusColor.current else Color.Transparent,
        label = "tvFocusRing",
    )

    return this
        .scale(scale)
        .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
        .onFocusChanged { focused = it.isFocused }
        // Постоянная (прозрачная вне фокуса) обводка — не сдвигает layout при появлении.
        .border(3.dp, ringColor, shape)
        .clickable(enabled = enabled, onClick = onClick)
}
