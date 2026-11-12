// Секции каталога дизайн-системы: цвета, шрифты, кнопки, чипы и бейджи. Карточки, строки, поля,
// плеер и состояния — в DesignSystemComponents.

package com.filmax.feature.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxColors
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.FilmaxTheme
import com.filmax.core.designsystem.FilmaxType
import com.filmax.core.ui.components.FilmaxAvatar
import com.filmax.core.ui.components.FilmaxBadge
import com.filmax.core.ui.components.FilmaxButton
import com.filmax.core.ui.components.FilmaxButtonSize
import com.filmax.core.ui.components.FilmaxButtonStyle
import com.filmax.core.ui.components.FilmaxChip
import com.filmax.core.ui.components.FilmaxChipSize
import com.filmax.core.ui.components.FilmaxCountdownChip
import com.filmax.core.ui.components.FilmaxIconButton
import com.filmax.core.ui.components.FilmaxIconButtonStyle
import com.filmax.core.ui.components.FilmaxOverline
import com.filmax.core.ui.components.FilmaxPageDots
import com.filmax.core.ui.components.FilmaxPlayButton
import com.filmax.core.ui.components.FilmaxRatingPill
import com.filmax.core.ui.components.FilmaxRoundButton
import com.filmax.core.ui.components.FilmaxSectionHeader
import com.filmax.core.ui.components.FilmaxSegmentedControl
import com.filmax.core.ui.components.FilmaxTabButton
import com.filmax.core.ui.components.FilmaxTagBadge
import com.filmax.core.ui.components.FilmaxTextButton
import com.filmax.core.ui.components.FilmaxWordmark
import com.filmax.core.ui.components.icon

/** Секция каталога: надзаголовок и примеры под ним. */
@Composable
internal fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 20.dp)) {
        FilmaxSectionHeader(title = title)
        Column(
            Modifier.fillMaxWidth().padding(horizontal = FilmaxMetrics.GridPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

/** Пример в ряд с горизонтальной прокруткой — карточки шире экрана. */
@Composable
internal fun Showcase(content: @Composable () -> Unit) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) { content() }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ColorsSection() {
    val palette = FilmaxTheme.palette
    val swatches = listOf(
        "accent" to palette.accent, "accent2" to palette.accent2, "accentText" to palette.accentText,
        "glow" to palette.glow, "wash" to palette.wash, "background" to palette.background,
        "Text" to FilmaxColors.Text, "Secondary" to FilmaxColors.TextSecondary, "Dim" to FilmaxColors.TextDim,
        "Glass" to FilmaxColors.Glass, "Card" to FilmaxColors.Card, "Error" to FilmaxColors.Error,
    )
    Section("Цвета") {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            swatches.forEach { (name, color) ->
                Column(Modifier.width(SwatchWidth)) {
                    Box(
                        Modifier
                            .size(SwatchWidth)
                            .clip(CircleShape)
                            .background(color)
                            .border(1.dp, FilmaxColors.GlassBorder, CircleShape),
                    )
                    Text(name, style = FilmaxType.metaSmall, color = FilmaxColors.TextDim, maxLines = 1)
                }
            }
        }
    }
}

@Composable
internal fun TypographySection() {
    val styles: List<Pair<String, TextStyle>> = listOf(
        "hero" to FilmaxType.hero, "title" to FilmaxType.title, "screenTitle" to FilmaxType.screenTitle,
        "sheetTitle" to FilmaxType.sheetTitle, "sectionTitle" to FilmaxType.sectionTitle, "button" to FilmaxType.button,
        "itemTitle" to FilmaxType.itemTitle, "chip" to FilmaxType.chip, "cardTitle" to FilmaxType.cardTitle,
        "body" to FilmaxType.body, "meta" to FilmaxType.meta, "metaSmall" to FilmaxType.metaSmall,
    )
    Section("Шрифты") {
        styles.forEach { (name, style) -> Text("$name — Кино", style = style, color = FilmaxColors.Text, maxLines = 1) }
        FilmaxOverline("overline — выбор редакции")
        FilmaxWordmark()
    }
}

@Composable
internal fun ButtonsSection() {
    Section("Кнопки") {
        FilmaxButton("Смотреть", onClick = {}, icon = Icons.Filled.PlayArrow, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilmaxButton(
                "Вторичная",
                onClick = {},
                style = FilmaxButtonStyle.Secondary,
                size = FilmaxButtonSize.Compact
            )
            FilmaxButton(
                "Удалить",
                onClick = {},
                style = FilmaxButtonStyle.Destructive,
                size = FilmaxButtonSize.Compact
            )
            FilmaxButton("Белая", onClick = {}, style = FilmaxButtonStyle.Light, size = FilmaxButtonSize.Compact)
        }
        FilmaxPlayButton(
            "Продолжить",
            "S4E7 «Мост» · осталось 29 мин",
            Icons.Filled.PlayArrow,
            onClick = {},
            progress = 0.45f
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilmaxIconButton(Icons.Filled.Cast, contentDescription = null, onClick = {})
            FilmaxIconButton(
                Icons.Filled.Add,
                contentDescription = null,
                onClick = {},
                style = FilmaxIconButtonStyle.Accent
            )
            FilmaxIconButton(Icons.Filled.Close, contentDescription = null, onClick = null)
            FilmaxRoundButton(Icons.Filled.Pause, contentDescription = "Пауза", onClick = {}, emphasized = true)
        }
        FilmaxTextButton("Читать полностью ›", onClick = {})
    }
}

@Composable
internal fun ChipsSection() {
    var segment by remember { mutableIntStateOf(0) }
    var size by remember { mutableIntStateOf(1) }
    Section("Чипы и сегменты") {
        Showcase {
            FilmaxChip("Выбран", selected = true, onClick = {})
            FilmaxChip("Жанр", selected = false, onClick = {}, trailingIcon = Icons.Filled.KeyboardArrowDown)
            FilmaxChip("Рейтинг 7+", selected = true, onClick = {}, trailingIcon = Icons.Filled.Close)
            FilmaxChip(
                "Серии",
                selected = false,
                onClick = {},
                leadingIcon = Icons.Filled.VideoLibrary,
                size = FilmaxChipSize.Large
            )
            FilmaxChip("Сезон 2", selected = false, onClick = {}, size = FilmaxChipSize.Small)
        }
        FilmaxSegmentedControl(
            listOf("Фильмы", "Сериалы", "Аниме"),
            segment,
            onSelect = { segment = it },
            modifier = Modifier.fillMaxWidth()
        )
        FilmaxSegmentedControl(listOf("S", "M", "L"), size, onSelect = { size = it }, compact = true)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilmaxTabButton(
                Icons.Filled.GraphicEq,
                "Аудио",
                selected = true,
                onClick = {},
                modifier = Modifier.width(TabWidth)
            )
            FilmaxTabButton(
                Icons.Filled.Subtitles,
                "Субтитры",
                selected = false,
                onClick = {},
                modifier = Modifier.width(TabWidth)
            )
        }
        FilmaxCountdownChip("Далее · S4E8 через 7", Icons.Filled.SkipNext, onClick = {}, progress = 0.3f)
    }
}

@Composable
internal fun BadgesSection() {
    Section("Бейджи") {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilmaxRatingPill("8.4")
            FilmaxTagBadge("5.1")
            FilmaxBadge("16+")
            FilmaxBadge("4K")
        }
        FilmaxPageDots(count = 4, current = 1)
        FilmaxAvatar(initials = "МФ")
    }
}

private val SwatchWidth = 52.dp
private val TabWidth = 84.dp
