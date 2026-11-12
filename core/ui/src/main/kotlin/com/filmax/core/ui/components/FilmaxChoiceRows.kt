// Строки выбора: переключатель, радио и галочка — сами и строками списка. Группа настроек и строка
// со значением — в FilmaxRows.
package com.filmax.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxColors
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.FilmaxTheme
import com.filmax.core.designsystem.FilmaxType

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

private val RadioRowShape = RoundedCornerShape(16.dp)
private val ToggleTravel = 20.dp
private val OffRing = Color.White.copy(alpha = 0.35f)
