package com.filmax.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

/** Переключатель 52×32: акцент палитры — включено, стекло — выключено. Сам не кликается: тап ловит строка. */
@Composable
fun FilmaxToggle(checked: Boolean, modifier: Modifier = Modifier) {
    val palette = FilmaxTheme.palette
    val track by animateColorAsState(if (checked) palette.accent else FilmaxColors.Track, label = "toggleTrack")
    val offset by animateDpAsState(if (checked) ToggleTravel else 0.dp, label = "toggleThumb")
    Box(
        modifier
            .size(width = 52.dp, height = 32.dp)
            .clip(CircleShape)
            .background(track)
            .padding(4.dp),
    ) {
        Box(
            Modifier
                .offset(x = offset)
                .size(24.dp)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

/**
 * Вариант выбора с радиокнопкой: дорожка, субтитры, качество. [badge] — «5.1» рядом с подписью,
 * [subtitle] — «Многоголосый · LostFilm». Выбранная строка подсвечена подложкой.
 */
// Компонент дизайн-системы: параметры — его публичный API (см. FilmaxButton).
@Suppress("LongParameterList")
@Composable
fun FilmaxRadioRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    badge: String? = null,
    swatch: Color? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 46.dp)
            .clip(RadioRowShape)
            .background(if (selected) FilmaxColors.Field else Color.Transparent)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilmaxRadio(selected)
        swatch?.let { Box(Modifier.size(22.dp).clip(CircleShape).background(it)) }
        Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    label,
                    style = FilmaxType.label,
                    color = FilmaxColors.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                badge?.let { FilmaxTagBadge(it) }
            }
            subtitle?.let {
                Text(
                    it,
                    style = FilmaxType.metaSmall,
                    color = FilmaxColors.TextDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Радиокнопка 22dp: выбранная — толстое кольцо акцента вокруг светлой точки. */
@Composable
fun FilmaxRadio(selected: Boolean, modifier: Modifier = Modifier) {
    val palette = FilmaxTheme.palette
    Box(
        modifier
            .size(22.dp)
            .clip(CircleShape)
            .then(
                if (selected) {
                    Modifier.background(palette.onAccent).border(6.dp, palette.accent, CircleShape)
                } else {
                    Modifier.border(2.dp, OffRing, CircleShape)
                },
            ),
    )
}

/**
 * Строка выбора с отметкой: папка в шторке «В папку». [leading] — плитка-иконка, справа — кружок
 * с галочкой. Тап переключает отметку сразу, без подтверждения.
 */
// Компонент дизайн-системы: параметры — его публичный API (см. FilmaxButton).
@Suppress("LongParameterList")
@Composable
fun FilmaxCheckRow(
    title: String,
    checked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: @Composable () -> Unit = {},
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = FilmaxMetrics.ListRowHeight)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onClick() })
            .padding(horizontal = FilmaxMetrics.ScreenPadding),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = FilmaxType.itemTitle,
                color = FilmaxColors.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            subtitle?.let { Text(it, style = FilmaxType.metaSmall, color = FilmaxColors.TextDim, maxLines = 1) }
        }
        FilmaxCheckCircle(checked)
    }
}

/** Кружок-отметка 26dp: отмечено — акцент с галочкой, нет — светлое кольцо. */
@Composable
fun FilmaxCheckCircle(checked: Boolean, modifier: Modifier = Modifier) {
    val palette = FilmaxTheme.palette
    Box(
        modifier
            .size(26.dp)
            .clip(CircleShape)
            .then(if (checked) Modifier.background(palette.accent) else Modifier.border(2.dp, OffRing, CircleShape)),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = palette.onAccent,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private val GroupShape = ShapePanel
private val IconTileShape = RoundedCornerShape(12.dp)
private val RadioRowShape = RoundedCornerShape(16.dp)
private val DividerInset = 66.dp
private val ToggleTravel = 20.dp
private val OffRing = Color.White.copy(alpha = 0.35f)
