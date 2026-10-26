// Секции каталога дизайн-системы: цвета, шрифты и каждая группа компонентов core:ui.

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
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import com.filmax.core.domain.catalog.CatalogSection
import com.filmax.core.presentation.labels.sectionLabel
import com.filmax.core.ui.components.EpisodeMark
import com.filmax.core.ui.components.FilmaxActionTile
import com.filmax.core.ui.components.FilmaxAvatar
import com.filmax.core.ui.components.FilmaxBadge
import com.filmax.core.ui.components.FilmaxButton
import com.filmax.core.ui.components.FilmaxButtonSize
import com.filmax.core.ui.components.FilmaxButtonStyle
import com.filmax.core.ui.components.FilmaxCastCard
import com.filmax.core.ui.components.FilmaxCheckRow
import com.filmax.core.ui.components.FilmaxChip
import com.filmax.core.ui.components.FilmaxChipSize
import com.filmax.core.ui.components.FilmaxCollectionCard
import com.filmax.core.ui.components.FilmaxContinueCard
import com.filmax.core.ui.components.FilmaxCountdownChip
import com.filmax.core.ui.components.FilmaxCreateFolderCard
import com.filmax.core.ui.components.FilmaxEmptyState
import com.filmax.core.ui.components.FilmaxEpisodeRow
import com.filmax.core.ui.components.FilmaxFolderCard
import com.filmax.core.ui.components.FilmaxFolderIcon
import com.filmax.core.ui.components.FilmaxHistoryRow
import com.filmax.core.ui.components.FilmaxIconButton
import com.filmax.core.ui.components.FilmaxIconButtonStyle
import com.filmax.core.ui.components.FilmaxListRow
import com.filmax.core.ui.components.FilmaxOverline
import com.filmax.core.ui.components.FilmaxPageDots
import com.filmax.core.ui.components.FilmaxPlayButton
import com.filmax.core.ui.components.FilmaxPosterCard
import com.filmax.core.ui.components.FilmaxPosterSkeleton
import com.filmax.core.ui.components.FilmaxProgressBar
import com.filmax.core.ui.components.FilmaxRadioRow
import com.filmax.core.ui.components.FilmaxRatingPill
import com.filmax.core.ui.components.FilmaxRatingStars
import com.filmax.core.ui.components.FilmaxRoundButton
import com.filmax.core.ui.components.FilmaxSearchField
import com.filmax.core.ui.components.FilmaxSearchResultRow
import com.filmax.core.ui.components.FilmaxSectionHeader
import com.filmax.core.ui.components.FilmaxSectionTile
import com.filmax.core.ui.components.FilmaxSeekBar
import com.filmax.core.ui.components.FilmaxSegmentedControl
import com.filmax.core.ui.components.FilmaxSettingsDivider
import com.filmax.core.ui.components.FilmaxSettingsGroup
import com.filmax.core.ui.components.FilmaxSettingsRow
import com.filmax.core.ui.components.FilmaxSubscriptionCard
import com.filmax.core.ui.components.FilmaxSuggestionRow
import com.filmax.core.ui.components.FilmaxTabButton
import com.filmax.core.ui.components.FilmaxTagBadge
import com.filmax.core.ui.components.FilmaxTextArea
import com.filmax.core.ui.components.FilmaxTextButton
import com.filmax.core.ui.components.FilmaxTextField
import com.filmax.core.ui.components.FilmaxToggleRow
import com.filmax.core.ui.components.FilmaxTopTenCard
import com.filmax.core.ui.components.FilmaxWordmark
import com.filmax.core.ui.components.SuggestionKind
import com.filmax.core.ui.components.collectionHue
import com.filmax.core.ui.components.folderHue
import com.filmax.core.ui.components.hue
import com.filmax.core.ui.components.icon

/** Секция каталога: надзаголовок и примеры под ним. */
@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
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
private fun Showcase(content: @Composable () -> Unit) {
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

@Composable
internal fun CardsSection() {
    Section("Карточки") {
        Showcase {
            FilmaxPosterCard(
                "Дюна",
                "",
                onClick = {},
                rating = "8.1",
                meta = "Фильм · 2024",
                modifier = Modifier.width(FilmaxMetrics.PosterWidth)
            )
            FilmaxContinueCard(
                "Северный ветер",
                "S2 · осталось 18 мин",
                "",
                0.6f,
                onClick = {},
                modifier = Modifier.width(FilmaxMetrics.ContinueWidth)
            )
            FilmaxTopTenCard(rank = 1, title = "Дюна", posterUrl = "", onClick = {})
        }
        Showcase {
            FilmaxCollectionCard("Оскар 2024", hue = collectionHue(1), onClick = {}, subtitle = "24 тайтла")
            FilmaxCastCard("Тимоти Шаламе", photoUrl = null, onClick = {}, role = "Пол Атрейдес")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilmaxCreateFolderCard(onClick = {}, modifier = Modifier.weight(1f))
            FilmaxFolderCard(
                "На выходные",
                "12 тайтлов",
                hue = folderHue(2),
                posters = emptyList(),
                onClick = {},
                modifier = Modifier.weight(1f)
            )
        }
        Showcase {
            CatalogSection.entries.take(3).forEach { section ->
                FilmaxSectionTile(
                    sectionLabel(section),
                    section.icon,
                    section.hue,
                    onClick = {},
                    modifier = Modifier.width(SectionTileWidth)
                )
            }
        }
        FilmaxSubscriptionCard("Filmax Премиум", "активна до 12 декабря · ещё 47 дней", active = true)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilmaxActionTile(Icons.Filled.Movie, "Трейлер", onClick = {}, modifier = Modifier.weight(1f))
            FilmaxActionTile(Icons.Filled.Tv, "В списке", onClick = {}, active = true, modifier = Modifier.weight(1f))
        }
        FilmaxPosterSkeleton(Modifier.width(FilmaxMetrics.PosterWidth))
    }
}

@Composable
internal fun RowsSection() {
    var toggle by remember { mutableStateOf(true) }
    var radio by remember { mutableIntStateOf(0) }
    var checked by remember { mutableStateOf(true) }
    Section("Строки") {
        FilmaxListRow("Гостиная", onClick = {}, icon = Icons.Filled.Tv, subtitle = "192.168.1.50")
        FilmaxSettingsGroup(title = "Просмотр") {
            FilmaxSettingsRow(Icons.Filled.HighQuality, "Качество видео", onClick = {}, value = "1080p")
            FilmaxSettingsDivider()
            FilmaxToggleRow(Icons.Filled.Palette, "Переключатель", checked = toggle, onCheckedChange = { toggle = it })
        }
        listOf("Русский · Многоголосый" to "5.1", "Английский" to null).forEachIndexed { index, (label, badge) ->
            FilmaxRadioRow(
                label,
                selected = radio == index,
                onClick = { radio = index },
                subtitle = "LostFilm",
                badge = badge
            )
        }
        FilmaxCheckRow("На выходные", checked = checked, onClick = { checked = !checked }, subtitle = "12 тайтлов") {
            FilmaxFolderIcon(hue = folderHue(0))
        }
        FilmaxEpisodeRow(
            "7. Мост",
            "Продолжить · осталось 29 мин",
            "",
            EpisodeMark.Current,
            0.45f,
            onClick = {},
            placeholder = "7"
        )
        FilmaxHistoryRow("Северный ветер", "S4E7", "21:10 · на 45%", "", 0.45f, onClick = {}, onMore = {})
        FilmaxSearchResultRow(
            "Дюна: Часть вторая",
            "дюна",
            "",
            onClick = {},
            rating = "8.4",
            meta = "Фильм · 2024",
            onInsert = {}
        )
        FilmaxSuggestionRow("Интерстеллар", SuggestionKind.Trending, onClick = {})
    }
}

@Composable
internal fun FieldsSection() {
    var query by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }
    Section("Поля") {
        FilmaxSearchField(query = query, onQueryChange = { query = it }, onVoice = {})
        FilmaxTextField(
            text,
            { text = it },
            placeholder = "Например, «На выходные»",
            onDone = {},
            label = "Название",
            maxLength = 40
        )
        FilmaxTextArea(comment, { comment = it }, placeholder = "Комментарий")
        var stars by remember { mutableIntStateOf(4) }
        FilmaxRatingStars(rating = stars, onRate = { stars = it })
    }
}

@Composable
internal fun PlaybackSection() {
    var position by remember { mutableFloatStateOf(0.4f) }
    Section("Воспроизведение") {
        FilmaxProgressBar(progress = 0.6f)
        FilmaxSeekBar(
            positionMs = (position * DEMO_DURATION_MS).toLong(),
            durationMs = DEMO_DURATION_MS,
            bufferedMs = (DEMO_DURATION_MS * DEMO_BUFFERED).toLong(),
            onScrub = { position = it / DEMO_DURATION_MS.toFloat() },
            onScrubFinished = {},
        )
    }
}

@Composable
internal fun StatesSection() {
    Section("Состояния") {
        FilmaxEmptyState(
            Icons.Filled.VideoLibrary,
            "Пока пусто",
            subtitle = "Отмечайте «Буду смотреть» в карточке тайтла",
            modifier = Modifier.fillMaxWidth()
        ) {
            FilmaxButton("Открыть каталог", onClick = {}, size = FilmaxButtonSize.Compact)
        }
        Text("Ошибка — цветом ошибки", style = FilmaxType.meta, color = FilmaxColors.Error)
    }
}

private val SwatchWidth = 52.dp
private val TabWidth = 84.dp
private val SectionTileWidth = 112.dp
private const val DEMO_DURATION_MS = 7_200_000L
private const val DEMO_BUFFERED = 0.65f
