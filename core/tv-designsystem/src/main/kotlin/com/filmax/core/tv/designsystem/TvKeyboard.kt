// Экранная клавиатура для пульта: системную клавиатуру ТВ поиск больше не открывает — она
// закрывает пол-экрана, и набранное не видно рядом с результатами.
@file:OptIn(ExperimentalComposeUiApi::class)

package com.filmax.core.tv.designsystem

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.SpaceBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.filmax.core.designsystem.Onest

private val KeyIconSize = 17.dp

/** Сетка поиска: восемь квадратных клавиш 36dp через 6dp. */
private const val GRID_COLUMNS = 8
private val GridKey = 36.dp
private val GridGap = 6.dp

/**
 * Вид клавиатуры. [Grid] — поиск: восемь квадратных клавиш 36dp в ряд, широкие «Пробел» и
 * «Стереть» дописаны в короткий последний ряд. [Panel] — боковая панель (редактор папки): клавиши
 * делят ширину панели на [columns] колонок, высота 32dp, «Пробел · Стереть · Голосом» — отдельным
 * нижним рядом.
 */
@Immutable
class TvKeyboardStyle private constructor(
    internal val columns: Int,
    internal val keyHeight: Dp,
    internal val gap: Dp,
    internal val fillWidth: Boolean,
    internal val shape: RoundedCornerShape,
    internal val text: TextStyle,
) {
    internal val focus = TvFocusStyle(shape, glowRadius = 9.dp, focusedScale = TvMetrics.FocusScale)

    companion object {
        val Grid = TvKeyboardStyle(
            columns = GRID_COLUMNS,
            keyHeight = GridKey,
            gap = GridGap,
            fillWidth = false,
            shape = RoundedCornerShape(9.dp),
            text = keyText(14),
        )

        /** Панель: ЙЦУКЕН в одиннадцать колонок. */
        val Panel = TvKeyboardStyle(
            columns = 11,
            keyHeight = 32.dp,
            gap = 5.dp,
            fillWidth = true,
            shape = RoundedCornerShape(8.dp),
            text = keyText(13),
        )
    }
}

private fun keyText(size: Int) = TextStyle(
    fontFamily = Onest,
    fontWeight = FontWeight.Medium,
    fontSize = size.sp,
    lineHeight = (size + 4).sp,
)

/** Ширина клавиатуры поиска — восемь клавиш с зазорами; по ней строится левая колонка экрана поиска. */
// Из констант, а не из TvKeyboardStyle.Grid: стиль при инициализации зовёт функции этого файла,
// и чтение Grid отсюда замкнуло бы инициализацию классов в круг (Grid ещё null).
val TvKeyboardWidth: Dp = spanWidth(GRID_COLUMNS, GridKey, GridGap)

/**
 * Что делают клавиши: символ, пробел, «Стереть». [onVoice] — голосовой ввод; null — кнопки
 * «Голосом» нет (экран не умеет слушать).
 */
data class TvKeyboardActions(
    val onKey: (String) -> Unit,
    val onSpace: () -> Unit,
    val onDelete: () -> Unit,
    val onVoice: (() -> Unit)? = null,
)

/**
 * Клавиатура сеткой клавиш вида [style]. Раскладки и правила набора — забота экрана: компонент
 * рисует [rows] и отдаёт нажатия.
 *
 * Клавиатура — группа фокуса с памятью: первый вход — на первую клавишу, возврат — на ту, с
 * которой ушли. [exitRight] — куда уводит «→» с правого края: рядом обычно результаты, и «вправо»
 * ведёт на лучший из них, а не на пространственно-ближайшую карточку. [firstKey] — первая
 * клавиша для экрана, который сам ставит на неё стартовый фокус (редактор папки).
 */
// Компонент дизайн-системы: параметры — его публичный API (ряды, действия, хвост опций с дефолтами).
@Suppress("LongParameterList")
@Composable
fun TvKeyboard(
    rows: List<List<String>>,
    actions: TvKeyboardActions,
    modifier: Modifier = Modifier,
    style: TvKeyboardStyle = TvKeyboardStyle.Grid,
    exitRight: FocusRequester? = null,
    firstKey: FocusRequester = remember { FocusRequester() },
) {
    val keys = KeyboardKeys(rows, actions, style, firstKey)
    Box(
        modifier
            .focusProperties {
                exit = { direction ->
                    if (direction == FocusDirection.Right && exitRight != null) exitRight else FocusRequester.Default
                }
            }
            .focusGroup(),
    ) {
        if (style.fillWidth) {
            // Ширина колонки — доля ширины панели: клавиатура заполняет её от края до края.
            BoxWithConstraints {
                KeyGrid(keys, unit = (maxWidth - style.gap * (style.columns - 1)) / style.columns)
            }
        } else {
            KeyGrid(keys, unit = style.keyHeight)
        }
    }
}

/** Всё, из чего строится сетка клавиш. */
private class KeyboardKeys(
    val rows: List<List<String>>,
    val actions: TvKeyboardActions,
    val style: TvKeyboardStyle,
    val firstKey: FocusRequester,
)

/** Ряды клавиш; [unit] — ширина одной колонки. */
@Composable
private fun KeyGrid(keys: KeyboardKeys, unit: Dp) {
    val style = keys.style
    Column(Modifier.tvFocusGroup(fallback = keys.firstKey), verticalArrangement = Arrangement.spacedBy(style.gap)) {
        keys.rows.forEachIndexed { rowIndex, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(style.gap)) {
                row.forEachIndexed { keyIndex, key ->
                    val first = rowIndex == 0 && keyIndex == 0
                    Key(
                        width = unit,
                        style = style,
                        onClick = { keys.actions.onKey(key) },
                        modifier = if (first) Modifier.focusRequester(keys.firstKey) else Modifier,
                    ) { Text(key, style = style.text) }
                }
                if (!style.fillWidth && rowIndex == keys.rows.lastIndex) {
                    WideKeys(style.columns - row.size, unit, keys)
                }
            }
        }
        if (style.fillWidth) {
            Row(horizontalArrangement = Arrangement.spacedBy(style.gap)) { WideKeys(style.columns, unit, keys) }
        }
    }
}

/** «Стереть» в нижнем ряду панели с «Голосом» — три колонки: пробелу и голосу поровну остального. */
private const val ERASE_COLUMNS_WITH_VOICE = 3

/**
 * «Пробел», «Стереть» и, если экран умеет слушать, «Голосом» на [columns] колонок. Без голоса
 * пробелу — бо́льшая половина.
 */
@Composable
private fun WideKeys(columns: Int, unit: Dp, keys: KeyboardKeys) {
    val style = keys.style
    val onVoice = keys.actions.onVoice?.takeIf { style.fillWidth }
    val erase = if (onVoice != null) ERASE_COLUMNS_WITH_VOICE else columns / 2
    val space = if (onVoice != null) (columns - erase) / 2 else columns - erase
    WideKey(space, unit, style, keys.actions.onSpace, Icons.Rounded.SpaceBar, "Пробел")
    WideKey(erase, unit, style, keys.actions.onDelete, Icons.AutoMirrored.Rounded.Backspace, "Стереть")
    if (onVoice != null) WideKey(columns - space - erase, unit, style, onVoice, Icons.Rounded.Mic, "Голосом")
}

// Широкая клавиша — часть приватной сетки: колонки, ширина колонки и вид идут прямо из неё.
@Suppress("LongParameterList")
@Composable
private fun WideKey(
    span: Int,
    unit: Dp,
    style: TvKeyboardStyle,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String
) {
    Key(width = spanWidth(span, unit, style.gap), style = style, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(KeyIconSize))
            Text(label, style = FxType.chip, maxLines = 1)
        }
    }
}

@Composable
private fun Key(
    width: Dp,
    style: TvKeyboardStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable () -> Unit,
) {
    val colors = LocalFxColors.current
    TvFocusSurface(
        onClick = onClick,
        modifier = modifier.size(width, style.keyHeight),
        style = style.focus,
        colors = TvSurfaceColors(container = colors.surface, content = colors.text),
    ) {
        Box(Modifier.align(Alignment.Center).padding(horizontal = 4.dp)) { label() }
    }
}

/** Ширина клавиши на [span] колонок шириной [unit] — вместе с зазорами между ними. */
private fun spanWidth(span: Int, unit: Dp, gap: Dp): Dp = unit * span + gap * (span - 1)

private val LayoutShape = RoundedCornerShape(14.dp)
private val LayoutFocus = TvFocusStyle(LayoutShape, glowRadius = 14.dp, focusedScale = TvMetrics.FocusScale)
private val LayoutStyle = TextStyle(
    fontFamily = Onest,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    lineHeight = 16.sp
)

/** Подложка выбранной раскладки — тише акцента: это состояние клавиатуры, а не главное действие. */
private const val LAYOUT_SELECTED_ALPHA = 0.14f

/**
 * Переключатель раскладок под клавиатурой: «RU · EN · 123». Выбранная — на светлой подложке,
 * остальные — приглушённым текстом. Ряд — группа фокуса: первый вход — на выбранную раскладку.
 */
@Composable
fun TvKeyboardLayouts(labels: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val selectedFocus = remember { FocusRequester() }
    Row(
        modifier.tvFocusGroup(fallback = selectedFocus),
        horizontalArrangement = Arrangement.spacedBy(TvKeyboardStyle.Grid.gap),
    ) {
        labels.forEachIndexed { index, label ->
            LayoutChip(
                label = label,
                selected = index == selected,
                onClick = { onSelect(index) },
                modifier = if (index == selected) Modifier.focusRequester(selectedFocus) else Modifier,
            )
        }
    }
}

@Composable
private fun LayoutChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val colors = LocalFxColors.current
    TvFocusSurface(
        onClick = onClick,
        modifier = modifier,
        style = LayoutFocus,
        colors = TvSurfaceColors(
            container = if (selected) Color.White.copy(alpha = LAYOUT_SELECTED_ALPHA) else Color.Transparent,
            content = if (selected) colors.text else colors.textSecondary,
            focusedContainer = colors.surfaceHigh,
            focusedContent = colors.text,
        ),
    ) {
        Text(label, style = LayoutStyle, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
    }
}
