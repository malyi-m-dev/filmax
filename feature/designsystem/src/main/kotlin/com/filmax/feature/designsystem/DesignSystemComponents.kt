// Секции каталога дизайн-системы: карточки, строки списков, поля ввода, элементы плеера и состояния
// экрана (пусто, ошибка, загрузка). Каркас секции и цвета со шрифтами — в DesignSystemSections.
package com.filmax.feature.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Palette
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
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxColors
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.FilmaxType
import com.filmax.core.domain.catalog.CatalogSection
import com.filmax.core.presentation.format.label
import com.filmax.core.ui.components.EpisodeMark
import com.filmax.core.ui.components.FilmaxActionTile
import com.filmax.core.ui.components.FilmaxButton
import com.filmax.core.ui.components.FilmaxButtonSize
import com.filmax.core.ui.components.FilmaxCastCard
import com.filmax.core.ui.components.FilmaxCheckRow
import com.filmax.core.ui.components.FilmaxCollectionCard
import com.filmax.core.ui.components.FilmaxContinueCard
import com.filmax.core.ui.components.FilmaxCreateFolderCard
import com.filmax.core.ui.components.FilmaxEmptyState
import com.filmax.core.ui.components.FilmaxEpisodeRow
import com.filmax.core.ui.components.FilmaxFolderCard
import com.filmax.core.ui.components.FilmaxFolderIcon
import com.filmax.core.ui.components.FilmaxHistoryRow
import com.filmax.core.ui.components.FilmaxListRow
import com.filmax.core.ui.components.FilmaxPosterCard
import com.filmax.core.ui.components.FilmaxPosterSkeleton
import com.filmax.core.ui.components.FilmaxProgressBar
import com.filmax.core.ui.components.FilmaxRadioRow
import com.filmax.core.ui.components.FilmaxRatingStars
import com.filmax.core.ui.components.FilmaxSearchField
import com.filmax.core.ui.components.FilmaxSearchResultRow
import com.filmax.core.ui.components.FilmaxSectionTile
import com.filmax.core.ui.components.FilmaxSeekBar
import com.filmax.core.ui.components.FilmaxSettingsDivider
import com.filmax.core.ui.components.FilmaxSettingsGroup
import com.filmax.core.ui.components.FilmaxSettingsRow
import com.filmax.core.ui.components.FilmaxSubscriptionCard
import com.filmax.core.ui.components.FilmaxSuggestionRow
import com.filmax.core.ui.components.FilmaxTextArea
import com.filmax.core.ui.components.FilmaxTextField
import com.filmax.core.ui.components.FilmaxToggleRow
import com.filmax.core.ui.components.FilmaxTopTenCard
import com.filmax.core.ui.components.SuggestionKind
import com.filmax.core.ui.components.collectionHue
import com.filmax.core.ui.components.folderHue
import com.filmax.core.ui.components.hue
import com.filmax.core.ui.components.icon

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
                    section.label,
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
        var secret by remember { mutableStateOf("") }
        FilmaxTextField(secret, { secret = it }, placeholder = "Пароль", onDone = {}, label = "Пароль", secret = true)
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

private val SectionTileWidth = 112.dp
private const val DEMO_DURATION_MS = 7_200_000L
private const val DEMO_BUFFERED = 0.65f
