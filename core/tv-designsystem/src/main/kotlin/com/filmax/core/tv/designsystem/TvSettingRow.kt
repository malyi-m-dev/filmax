// Строка настройки Профиля: подпись слева, значение и «›» справа. Фокус — рамка, свечение и
// увеличение 1.02 (широкая строка), подложка светлеет, значение окрашивается акцентом.
package com.filmax.core.tv.designsystem

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text

private val RowHeight = 52.dp
private val ChevronSize = 18.dp

/** Строка — стекло 6%, в фокусе — 14%: фокус держат рамка и подъём фона, а не увеличение. */
private const val ROW_ALPHA = 0.06f
private const val ROW_FOCUSED_ALPHA = 0.14f

/**
 * Строка настройки. [value] — текущее значение справа, со стрелкой «›»: «ОК» открывает выбор;
 * null — строка-действие без значения («Проверить обновления»). [destructive] красит подпись
 * приглушённым красным — «Выйти из аккаунта».
 */
@Composable
fun TvSettingRow(
    label: String,
    value: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    destructive: Boolean = false,
) {
    val colors = LocalFxColors.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    TvFocusSurface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(RowHeight),
        style = TvFocusStyle.Row,
        colors = TvSurfaceColors(
            container = Color.White.copy(alpha = ROW_ALPHA),
            content = if (destructive) colors.error else colors.text,
            focusedContainer = Color.White.copy(alpha = ROW_FOCUSED_ALPHA),
        ),
        interactionSource = interaction,
    ) {
        Row(
            Modifier.align(Alignment.CenterStart).fillMaxWidth().padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(label, style = FxType.button, maxLines = 1)
            Spacer(Modifier.weight(1f))
            if (value != null) SettingValue(value, focused)
        }
    }
}

@Composable
private fun SettingValue(value: String, focused: Boolean) {
    val colors = LocalFxColors.current
    Text(value, style = FxType.metaRow, color = if (focused) colors.accentText else colors.textSecondary, maxLines = 1)
    Icon(
        Icons.AutoMirrored.Rounded.KeyboardArrowRight,
        contentDescription = null,
        tint = colors.textSecondary,
        modifier = Modifier.size(ChevronSize),
    )
}
