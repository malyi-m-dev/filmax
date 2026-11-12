package com.filmax.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxColors
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.FilmaxTheme
import com.filmax.core.designsystem.FilmaxType
import com.filmax.core.designsystem.ShapePanel

/**
 * Группа настроек: надзаголовок и стеклянная карточка со строками. Строки разделяет
 * [FilmaxSettingsDivider] — его ставит место вызова: группа не знает, какая строка последняя.
 */
@Composable
fun FilmaxSettingsGroup(title: String?, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth()) {
        title?.let {
            Text(
                it.uppercase(),
                style = FilmaxType.overline,
                color = FilmaxColors.TextDim,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 10.dp),
            )
        }
        Column(
            Modifier
                .fillMaxWidth()
                .clip(GroupShape)
                .background(FilmaxColors.Card)
                .border(1.dp, FilmaxColors.CardBorder, GroupShape),
            content = content,
        )
    }
}

/** Разделитель строк группы — с отступом под иконку, как в списках системы. */
@Composable
fun FilmaxSettingsDivider() {
    Box(
        Modifier
            .padding(start = DividerInset)
            .fillMaxWidth()
            .height(1.dp)
            .background(FilmaxColors.Divider),
    )
}

/**
 * Строка настройки: иконка в плитке, подпись, значение и «›». [destructive] — «Выйти»: строка
 * красится цветом ошибки целиком, это единственный красный на экране.
 */
// Компонент дизайн-системы: параметры — его публичный API (см. FilmaxButton).
@Suppress("LongParameterList")
@Composable
fun FilmaxSettingsRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    value: String? = null,
    chevron: Boolean = true,
    destructive: Boolean = false,
) {
    SettingsRowFrame(icon, label, destructive, modifier.clickable(role = Role.Button, onClick = onClick)) {
        value?.let {
            Text(
                it,
                style = FilmaxType.bodySmall,
                color = FilmaxColors.TextDim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (chevron && !destructive) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = FilmaxColors.TextDim,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** Строка-переключатель: тап по всей строке переключает, как у системных настроек. */
@Composable
fun FilmaxToggleRow(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsRowFrame(
        icon = icon,
        label = label,
        destructive = false,
        modifier = modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
    ) {
        FilmaxToggle(checked = checked)
    }
}

@Composable
private fun SettingsRowFrame(
    icon: ImageVector,
    label: String,
    destructive: Boolean,
    modifier: Modifier,
    trailing: @Composable () -> Unit,
) {
    val palette = FilmaxTheme.palette
    val tint = if (destructive) FilmaxColors.Error else palette.accentText
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = FilmaxMetrics.SettingsRowHeight)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(IconTileShape)
                .background(FilmaxColors.Field),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Text(
            label,
            style = FilmaxType.label,
            color = if (destructive) FilmaxColors.Error else FilmaxColors.Text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        trailing()
    }
}

private val GroupShape = ShapePanel
private val IconTileShape = RoundedCornerShape(12.dp)
private val DividerInset = 66.dp
