@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.filmax.core.tv.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text

/**
 * Крупная pill-кнопка для пульта на базе tv-material3 [Button] — фокус/масштаб/обводка
 * берутся из коробки. [primary] — брендовая заливка, иначе «вторичная» поверхность.
 * На фокусе помимо смены цвета добавляется фирменная [TvFocus]-обводка для явной аффорданс.
 */
@Composable
fun TvButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = true,
    leadingIcon: ImageVector? = null,
    focusRequester: FocusRequester? = null,
) {
    val colors = if (primary) {
        ButtonDefaults.colors(
            containerColor = TvPrimaryContainer,
            contentColor = TvOnPrimaryContainer,
            focusedContainerColor = TvPrimary,
            focusedContentColor = TvOnPrimary,
        )
    } else {
        ButtonDefaults.colors(
            containerColor = TvSurfaceContainerHigh,
            contentColor = TvOnSurface,
            focusedContainerColor = TvOnSurface,
            focusedContentColor = TvSurface,
        )
    }

    val shape = RoundedCornerShape(percent = 50)
    Button(
        onClick = onClick,
        colors = colors,
        shape = ButtonDefaults.shape(shape),
        scale = ButtonDefaults.scale(focusedScale = 1.1f),
        border = ButtonDefaults.border(
            focusedBorder = Border(BorderStroke(3.dp, TvFocus), shape = shape),
        ),
        modifier = modifier.then(
            if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier
        ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (leadingIcon != null) {
                Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(24.dp))
            }
            Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
        }
    }
}

/**
 * Фокусируемая карточка-контейнер на базе tv-material3 clickable [Surface]. Содержимое
 * (постер, плитка и т.п.) передаётся слотом. На фокусе срабатывают нативный масштаб и
 * фирменная [TvFocus]-обводка, которая рисуется ПОВЕРХ контента (иначе постер на
 * `fillMaxSize` перекрыл бы рамку самого Surface и фокус был бы не виден).
 */
@Composable
fun TvFocusCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    focusRequester: FocusRequester? = null,
    content: @Composable () -> Unit,
) {
    // focused держим в обычном State и читаем ТОЛЬКО внутри draw-лямбды ниже, поэтому смена
    // фокуса инвалидирует лишь фазу отрисовки рамки, а не рекомпозицию content() (постера).
    // Это критично для TV: при навигации пультом фокус прыгает по десяткам карточек, и лишняя
    // рекомпозиция каждого PosterImage на каждый шаг фокуса заметно роняла FPS.
    val focused = remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.1f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent,
            pressedContainerColor = Color.Transparent,
            contentColor = TvOnSurface,
            focusedContentColor = TvOnSurface,
        ),
        modifier = modifier
            .onFocusChanged { focused.value = it.isFocused }
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
    ) {
        // Одна обводка вместо двух: рисуем её поверх контента прямо в draw-фазе (внутри Surface,
        // чтобы фокус-масштаб 1.1 применился и к рамке). Никакого второго Box со своим border.
        Box(
            Modifier.drawWithContent {
                drawContent()
                if (focused.value) {
                    val stroke = Stroke(width = 3.dp.toPx())
                    when (val outline = shape.createOutline(size, layoutDirection, this)) {
                        is Outline.Rectangle -> drawRect(color = TvFocus, style = stroke)
                        is Outline.Rounded -> {
                            val rr = outline.roundRect
                            drawRoundRect(
                                color = TvFocus,
                                topLeft = Offset(rr.left, rr.top),
                                size = Size(rr.width, rr.height),
                                cornerRadius = CornerRadius(rr.topLeftCornerRadius.x, rr.topLeftCornerRadius.y),
                                style = stroke,
                            )
                        }
                        is Outline.Generic -> drawPath(path = outline.path, color = TvFocus, style = stroke)
                    }
                }
            }
        ) {
            content()
        }
    }
}
