// Экранная клавиатура для пульта: системную клавиатуру ТВ поиск больше не открывает — она
// закрывает пол-экрана, и набранное не видно рядом с результатами.
@file:OptIn(ExperimentalComposeUiApi::class)

package com.filmax.core.tv.designsystem

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.SpaceBar
import androidx.compose.runtime.Composable
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

private val KeySize = 36.dp
private val KeyGap = 6.dp
private val KeyShape = RoundedCornerShape(9.dp)
private val KeyIconSize = 17.dp
private val KeyFocus = TvFocusStyle(KeyShape, glowRadius = 9.dp, focusedScale = TvMetrics.FocusScale)
private val KeyStyle = TextStyle(
    fontFamily = Onest,
    fontWeight = FontWeight.Medium,
    fontSize = 14.sp,
    lineHeight = 18.sp
)

/** Сколько клавиш в ряду: раскладки рисуются сеткой одной ширины. */
private const val KEYBOARD_COLUMNS = 8

/** Ширина клавиатуры — восемь клавиш с зазорами; по ней строится левая колонка экрана поиска. */
val TvKeyboardWidth: Dp = keyWidth(KEYBOARD_COLUMNS)

/** Что делают клавиши: символ, пробел, «Стереть». */
data class TvKeyboardActions(
    val onKey: (String) -> Unit,
    val onSpace: () -> Unit,
    val onDelete: () -> Unit,
)

/**
 * Клавиатура сеткой по восемь клавиш 36dp. В последнем, коротком ряду — широкие «Пробел» и
 * «Стереть» на оставшиеся колонки. Раскладки и правила набора — забота экрана: компонент рисует
 * [rows] и отдаёт нажатия.
 *
 * Клавиатура — группа фокуса с памятью: первый вход — на первую клавишу, возврат — на ту, с
 * которой ушли. [exitRight] — куда уводит «→» с правого края: рядом обычно результаты, и «вправо»
 * ведёт на лучший из них, а не на пространственно-ближайшую карточку. [firstKey] — первая
 * клавиша для экрана, который сам ставит на неё стартовый фокус (редактор папки).
 */
@Composable
fun TvKeyboard(
    rows: List<List<String>>,
    actions: TvKeyboardActions,
    modifier: Modifier = Modifier,
    exitRight: FocusRequester? = null,
    firstKey: FocusRequester = remember { FocusRequester() },
) {
    Box(
        modifier
            .focusProperties {
                exit = { direction ->
                    if (direction == FocusDirection.Right && exitRight != null) exitRight else FocusRequester.Default
                }
            }
            .focusGroup(),
    ) {
        Column(Modifier.tvFocusGroup(fallback = firstKey), verticalArrangement = Arrangement.spacedBy(KeyGap)) {
            rows.forEachIndexed { rowIndex, row ->
                Row(horizontalArrangement = Arrangement.spacedBy(KeyGap)) {
                    row.forEachIndexed { keyIndex, key ->
                        val first = rowIndex == 0 && keyIndex == 0
                        Key(
                            columns = 1,
                            onClick = { actions.onKey(key) },
                            modifier = if (first) Modifier.focusRequester(firstKey) else Modifier,
                        ) { Text(key, style = KeyStyle) }
                    }
                    if (rowIndex == rows.lastIndex) WideKeys(KEYBOARD_COLUMNS - row.size, actions)
                }
            }
        }
    }
}

/** «Пробел» и «Стереть» на [columns] свободных колонок последнего ряда: пробелу — бо́льшая половина. */
@Composable
private fun WideKeys(columns: Int, actions: TvKeyboardActions) {
    val spaceColumns = (columns + 1) / 2
    Key(columns = spaceColumns, onClick = actions.onSpace) { KeyLabel(Icons.Rounded.SpaceBar, "Пробел") }
    Key(columns = columns - spaceColumns, onClick = actions.onDelete) {
        KeyLabel(Icons.AutoMirrored.Rounded.Backspace, "Стереть")
    }
}

@Composable
private fun KeyLabel(icon: ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(KeyIconSize))
        Text(label, style = FxType.chip, maxLines = 1)
    }
}

@Composable
private fun Key(columns: Int, onClick: () -> Unit, modifier: Modifier = Modifier, label: @Composable () -> Unit) {
    val colors = LocalFxColors.current
    TvFocusSurface(
        onClick = onClick,
        modifier = modifier.size(keyWidth(columns), KeySize),
        style = KeyFocus,
        colors = TvSurfaceColors(container = colors.surface, content = colors.text),
    ) {
        Box(Modifier.align(Alignment.Center).padding(horizontal = 4.dp)) { label() }
    }
}

/** Ширина клавиши на [columns] колонок — вместе с зазорами между ними. */
private fun keyWidth(columns: Int): Dp = KeySize * columns + KeyGap * (columns - 1)

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
    Row(modifier.tvFocusGroup(fallback = selectedFocus), horizontalArrangement = Arrangement.spacedBy(KeyGap)) {
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
