package com.filmax.core.tv.designsystem

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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

/** Вид чипа: заливка — сегменты и тип контента, контур — множественный выбор (жанры). */
enum class TvChipStyle { Filled, Outlined }

/** Обводка невыбранного контурного чипа. */
private const val OUTLINE_ALPHA = 0.14f
private val ChipIconSize = 16.dp

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
) {
    val colors = LocalFxColors.current
    val outlined = style == TvChipStyle.Outlined
    val surfaceColors = when {
        outlined -> TvSurfaceColors(
            container = Color.Transparent,
            content = if (selected) colors.accentText else colors.text,
        )
        selected -> TvSurfaceColors(container = colors.accent, content = colors.onAccent)
        else -> TvSurfaceColors(container = Color.White.copy(alpha = FILLED_IDLE_ALPHA), content = colors.text)
    }
    val icon = leadingIcon ?: Icons.Rounded.Check.takeIf { outlined && selected }
    TvFocusSurface(
        onClick = onClick,
        modifier = modifier.then(
            if (outlined) {
                Modifier.border(
                    1.dp,
                    if (selected) colors.accent else Color.White.copy(alpha = OUTLINE_ALPHA),
                    TvMetrics.ChipShape,
                )
            } else {
                Modifier
            },
        ),
        style = TvFocusStyle.Chip,
        colors = surfaceColors,
    ) {
        Row(
            Modifier.padding(start = if (icon != null) 10.dp else 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(ChipIconSize))
            Text(label, style = FxType.chip, maxLines = 1)
        }
    }
}

/** Невыбранный залитый чип — стекло 8%: тише вторичной кнопки (12%). */
private const val FILLED_IDLE_ALPHA = 0.08f
