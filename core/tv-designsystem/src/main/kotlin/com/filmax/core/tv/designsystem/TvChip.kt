package com.filmax.core.tv.designsystem

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text

/**
 * Вид чипа: заливка — сегменты и тип контента, контур — множественный выбор (жанры), компактный —
 * плотный ряд коротких значений (номера сезонов в панели «Эпизоды»). [CompactOutlined] —
 * компактный, где выбранный отмечен контуром, а не заливкой: сезоны в панели плеера, где заливку
 * уже носит выбранная вкладка и второй залитый элемент спорил бы с ней.
 */
enum class TvChipStyle { Filled, Outlined, Compact, CompactOutlined }

/** Обводка невыбранного контурного чипа. */
private const val OUTLINE_ALPHA = 0.14f
private val ChipIconSize = 16.dp
private val TrailingIconSize = 18.dp

/**
 * Чип: тип, жанр, сортировка, сезон, сегмент «Моё». Выбор и фокус — разные сигналы: выбранный
 * залитый чип — акцентом, выбранный контурный — акцентной обводкой с галочкой; фокус — общая
 * рамка со свечением ([TvFocusSurface]).
 */
// Компонент дизайн-системы: параметры — его публичный API (подпись, выбор, хвост опций).
@Suppress("LongParameterList")
@Composable
fun TvChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: TvChipStyle = TvChipStyle.Filled,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
) {
    val outlined = style == TvChipStyle.Outlined
    val icon = leadingIcon ?: Icons.Rounded.Check.takeIf { outlined && selected }
    val outline = outlined || style == TvChipStyle.CompactOutlined && selected
    TvFocusSurface(
        onClick = onClick,
        modifier = modifier.then(if (outline) Modifier.chipOutline(selected) else Modifier),
        style = TvFocusStyle.Chip,
        colors = chipColors(style, selected),
    ) {
        Row(
            Modifier.padding(chipPadding(style, hasIcon = icon != null, hasTrailing = trailingIcon != null)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(ChipIconSize))
            Text(label, style = FxType.chip, maxLines = 1)
            // Шеврон — чип открывает список вариантов (сортировка), а не переключает значение.
            if (trailingIcon != null) {
                Icon(trailingIcon, contentDescription = null, modifier = Modifier.size(TrailingIconSize))
            }
        }
    }
}

/** Заливка и цвет подписи: контурный — без заливки, выбранный — акцентом, остальные — стеклом. */
@Composable
private fun chipColors(style: TvChipStyle, selected: Boolean): TvSurfaceColors {
    val colors = LocalFxColors.current
    return when {
        style == TvChipStyle.Outlined -> TvSurfaceColors(
            container = Color.Transparent,
            content = if (selected) colors.accentText else colors.text,
        )
        style == TvChipStyle.CompactOutlined && selected ->
            TvSurfaceColors(container = Color.Transparent, content = colors.accentText)
        selected -> TvSurfaceColors(container = colors.accent, content = colors.onAccent)
        else -> TvSurfaceColors(container = Color.White.copy(alpha = FILLED_IDLE_ALPHA), content = colors.text)
    }
}

/** Обводка контурного чипа: выбранного — акцентом, остальных — тонкой светлой линией. */
@Composable
private fun Modifier.chipOutline(selected: Boolean): Modifier = border(
    1.dp,
    if (selected) LocalFxColors.current.accent else Color.White.copy(alpha = OUTLINE_ALPHA),
    TvMetrics.ChipShape,
)

/** Поля чипа: компактный плотнее, с иконкой по краю — уже с той стороны, иконка сама даёт отступ. */
private fun chipPadding(style: TvChipStyle, hasIcon: Boolean, hasTrailing: Boolean): PaddingValues {
    val compact = style == TvChipStyle.Compact || style == TvChipStyle.CompactOutlined
    val horizontal = if (compact) 11.dp else 16.dp
    val vertical = if (compact) 6.dp else 8.dp
    return PaddingValues(
        start = if (hasIcon) 10.dp else horizontal,
        end = if (hasTrailing) 12.dp else horizontal,
        top = vertical,
        bottom = vertical
    )
}

/** Невыбранный залитый чип — стекло 8%: тише вторичной кнопки (12%). */
private const val FILLED_IDLE_ALPHA = 0.08f
