package com.filmax.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxColors
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.FilmaxTheme
import com.filmax.core.designsystem.FilmaxType
import com.filmax.core.designsystem.ShapeFull
import com.filmax.core.designsystem.glassFill

/** Размер чипа: [Small] — сезоны в панели плеера, [Regular] — фильтры и сегменты, [Large] — ряд плеера. */
enum class FilmaxChipSize(internal val height: Dp, internal val padding: Dp, internal val text: TextStyle) {
    Small(height = 32.dp, padding = 14.dp, text = FilmaxType.cardTitle),
    Regular(height = FilmaxMetrics.ChipHeight, padding = 16.dp, text = FilmaxType.chip),
    Large(height = 44.dp, padding = 16.dp, text = FilmaxType.chip),
}

/**
 * Чип: сезон, тип тайтла, фильтр, сегмент раздела. Выбранный залит акцентом палитры, остальные —
 * стекло: поверх кадра и ленты чип читается, не перекрывая их. [trailingIcon] — «▾» у чипа,
 * открывающего выбор, или «✕» у применённого фильтра.
 */
// Компонент дизайн-системы: параметры — его публичный API (см. FilmaxButton).
@Suppress("LongParameterList")
@Composable
fun FilmaxChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    size: FilmaxChipSize = FilmaxChipSize.Regular,
) {
    val palette = FilmaxTheme.palette
    val content = if (selected) palette.onAccent else FilmaxColors.Text
    Row(
        modifier = modifier
            .height(size.height)
            .then(if (selected) Modifier.clip(ShapeFull).background(palette.accent) else Modifier.glassFill(ShapeFull))
            // selectable, а не clickable: TalkBack объявляет, какой чип выбран.
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(start = if (leadingIcon != null) 12.dp else size.padding, end = size.padding),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leadingIcon?.let { Icon(it, contentDescription = null, tint = content, modifier = Modifier.size(20.dp)) }
        Text(label, style = size.text, color = content, maxLines = 1)
        trailingIcon?.let { Icon(it, contentDescription = null, tint = content, modifier = Modifier.size(18.dp)) }
    }
}

/**
 * Сегменты в одной стеклянной дорожке: «Фильмы · Сериалы · Мульты · Аниме». Сегменты делят
 * ширину поровну. [compact] — короткие варианты фиксированной ширины («S · M · L» размера субтитров).
 */
@Composable
fun FilmaxSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val palette = FilmaxTheme.palette
    val outer = if (compact) CompactOuterShape else ShapeFull
    val inner = if (compact) CompactInnerShape else ShapeFull
    Row(
        modifier
            .clip(outer)
            .background(FilmaxColors.Field)
            .padding(if (compact) 3.dp else 4.dp)
            .selectableGroup(),
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .then(if (compact) Modifier.width(CompactSegmentWidth) else Modifier.weight(1f))
                    .height(if (compact) CompactSegmentHeight else FilmaxMetrics.SegmentHeight)
                    .clip(inner)
                    .background(if (selected) palette.accent else Color.Transparent)
                    .selectable(selected = selected, role = Role.Tab) { onSelect(index) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    option,
                    style = if (compact) FilmaxType.cardTitle else FilmaxType.chip,
                    color = if (selected) palette.onAccent else FilmaxColors.TextSecondary,
                    maxLines = 1,
                )
            }
        }
    }
}

private val CompactOuterShape = RoundedCornerShape(14.dp)
private val CompactInnerShape = RoundedCornerShape(11.dp)
private val CompactSegmentWidth = 44.dp
private val CompactSegmentHeight = 30.dp

/**
 * Вкладка панели плеера: иконка над подписью. Выбранная залита акцентом, остальные — без подложки:
 * дорожка вкладок сама стеклянная.
 */
@Composable
fun FilmaxTabButton(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = FilmaxTheme.palette
    val content = if (selected) palette.onAccent else FilmaxColors.TextSecondary
    Column(
        modifier
            .height(TabButtonHeight)
            .clip(TabButtonShape)
            .background(if (selected) palette.accent else Color.Transparent)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(22.dp))
        Text(label, style = FilmaxType.tag, color = content, maxLines = 1)
    }
}

/**
 * Чип «Далее · S4E8 через 7»: заливка акцентом слева растёт по мере отсчёта [progress] 0..1 — видно,
 * сколько осталось до автоперехода. Без отсчёта ([progress] = null) — обычный стеклянный чип.
 */
@Composable
fun FilmaxCountdownChip(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    progress: Float? = null,
) {
    val palette = FilmaxTheme.palette
    Box(
        modifier
            .height(FilmaxChipSize.Large.height)
            .clip(ShapeFull)
            .background(CountdownFill)
            .selectable(selected = false, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.CenterStart,
    ) {
        progress?.let {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(it.coerceIn(0f, 1f))
                    .background(palette.accent.copy(alpha = COUNTDOWN_ACCENT_ALPHA)),
            )
        }
        Row(
            Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = FilmaxType.chip, color = FilmaxColors.Text, maxLines = 1)
            Icon(icon, contentDescription = null, tint = FilmaxColors.Text, modifier = Modifier.size(20.dp))
        }
    }
}

private val TabButtonHeight = 52.dp
private val TabButtonShape = RoundedCornerShape(16.dp)
private val CountdownFill = Color.White.copy(alpha = 0.16f)
private const val COUNTDOWN_ACCENT_ALPHA = 0.85f
