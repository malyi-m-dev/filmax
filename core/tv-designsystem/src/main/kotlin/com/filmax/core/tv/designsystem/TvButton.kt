package com.filmax.core.tv.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text

private val ButtonHeight = 44.dp
private val ButtonIconSize = 20.dp
private val IconButtonSize = 44.dp
private val IconButtonGlyph = 22.dp

/** Отступы по бокам: у главной кнопки шире — она заметнее и в макете крупнее. */
private val PrimaryPadding = 22.dp
private val SecondaryPadding = 20.dp

/**
 * Вид кнопки: [Primary] — главное действие экрана, заливка акцентом палитры; [Secondary] —
 * стекло поверх кадра; [Light] — белая с тёмным текстом: главное действие на цветной шапке
 * подборки и папки, где акцентная заливка сливалась бы с фоном.
 */
enum class TvButtonStyle { Primary, Secondary, Light }

/**
 * Кнопка для пульта, вид — [style]. Фокус — общий для всех элементов: рамка, свечение, масштаб
 * ([TvFocusSurface]).
 */
// Компонент дизайн-системы: параметры — его публичный API (Compose-конвенция: modifier — прямой
// параметр, хвост — опции с дефолтами). Обёртка в data-класс сломала бы «минимальный API» и modifier.
@Suppress("LongParameterList")
@Composable
fun TvButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: TvButtonStyle = TvButtonStyle.Primary,
    leadingIcon: ImageVector? = null,
    focusRequester: FocusRequester? = null,
) {
    TvFocusSurface(
        onClick = onClick,
        modifier = modifier
            .height(ButtonHeight)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
        style = TvFocusStyle.Button,
        colors = buttonColors(style),
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = if (style == TvButtonStyle.Secondary) SecondaryPadding else PrimaryPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (leadingIcon != null) {
                Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(ButtonIconSize))
            }
            Text(text, style = FxType.button, maxLines = 1, softWrap = false)
        }
    }
}

/**
 * Круглая кнопка-иконка 44dp: вторичные действия рядом с главной («В список», «Трейлер» у
 * сериала), пауза в плеере. [contentDescription] обязателен — подписи у кнопки нет.
 */
@Composable
fun TvIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalFxColors.current
    TvFocusSurface(
        onClick = onClick,
        modifier = modifier.size(IconButtonSize),
        style = TvFocusStyle.Circle,
        colors = TvSurfaceColors(container = colors.surfaceHigh, content = colors.text),
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            modifier = Modifier.align(Alignment.Center).size(IconButtonGlyph),
        )
    }
}

@Composable
private fun buttonColors(style: TvButtonStyle): TvSurfaceColors {
    val colors = LocalFxColors.current
    return when (style) {
        TvButtonStyle.Primary -> TvSurfaceColors(container = colors.accent, content = colors.onAccent)
        TvButtonStyle.Secondary -> TvSurfaceColors(container = colors.surfaceHigh, content = colors.text)
        TvButtonStyle.Light -> TvSurfaceColors(container = Color.White, content = colors.background)
    }
}
