// Вкладка «Поиск»: поле с голосом, типы чипами, подсказки пустого поиска и выдача строками.
// Голос — полноэкранный оверлей из core:ui; поиск — тот же SearchScreenModel, что и у Каталога.

package com.filmax.feature.search.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxColors
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.FilmaxTheme
import com.filmax.core.designsystem.FilmaxType
import com.filmax.core.designsystem.radialWash
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.navigation.Navigator
import com.filmax.core.presentation.labels.posterMeta
import com.filmax.core.presentation.labels.ratingLabel
import com.filmax.core.presentation.labels.typeLabel
import com.filmax.core.ui.components.FilmaxChip
import com.filmax.core.ui.components.FilmaxEmptyState
import com.filmax.core.ui.components.FilmaxSearchField
import com.filmax.core.ui.components.FilmaxSearchResultRow
import com.filmax.core.ui.components.FilmaxSuggestionRow
import com.filmax.core.ui.components.FilmaxTextButton
import com.filmax.core.ui.components.LocalNavBarPadding
import com.filmax.core.ui.components.SuggestionKind
import com.filmax.core.ui.components.VoiceListeningOverlay
import com.filmax.core.ui.components.rememberInAppVoiceSearch
import com.filmax.feature.search.common.SearchEvent
import com.filmax.feature.search.common.SearchScreenModel
import com.filmax.feature.search.common.SearchState
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/** Типы поиска: документальное ищется во «Всём» — пятый чип не стоит строки на узком экране. */
private val SearchTypes = listOf(
    null to "Всё",
    ItemType.MOVIE to "Фильмы",
    ItemType.SERIES to "Сериалы",
    ItemType.ANIME to "Аниме",
)

/** Сколько примеров предложить в голосовом оверлее. */
private const val VOICE_EXAMPLES = 3

@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    screenModel: SearchScreenModel = koinViewModel(),
    navigator: Navigator = koinInject(),
) {
    val state by screenModel.collectAsState()
    val onEvent: (SearchEvent) -> Unit = screenModel::dispatch
    screenModel.collectSideEffect { effect -> openSearchEffect(navigator, effect) }
    val keyboard = LocalSoftwareKeyboardController.current
    val voice = rememberInAppVoiceSearch(onResult = { onEvent(SearchEvent.SubmitQuery(it)) })
    val focus = remember { FocusRequester() }
    // Вкладку открывают, чтобы искать: клавиатура — сразу, если запроса ещё нет.
    LaunchedEffect(Unit) { if (state.query.isEmpty()) focus.requestFocus() }

    val palette = FilmaxTheme.palette
    Box(
        modifier
            .fillMaxSize()
            .background(palette.background)
            .radialWash(palette.wash, centerX = 0.8f, centerY = 0f, radiusX = 0.9f, radiusY = 0.4f),
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            FilmaxSearchField(
                query = state.query,
                onQueryChange = { onEvent(SearchEvent.QueryChange(it)) },
                onVoice = voice::start,
                onSearch = {
                    keyboard?.hide()
                    onEvent(SearchEvent.SubmitQuery(state.query))
                },
                focusRequester = focus,
                modifier = Modifier.padding(horizontal = FilmaxMetrics.GridPadding).padding(top = 8.dp),
            )
            TypeChips(selected = state.filter, onEvent = onEvent)
            SearchBody(state = state, onEvent = onEvent)
        }
    }
    VoiceListeningOverlay(
        controller = voice,
        examples = state.trendingQueries.take(VOICE_EXAMPLES),
        onExample = { onEvent(SearchEvent.SubmitQuery(it)) },
    )
}

@Composable
private fun TypeChips(selected: ItemType?, onEvent: (SearchEvent) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = FilmaxMetrics.ScreenPadding, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SearchTypes.forEach { (type, label) ->
            FilmaxChip(
                label = label,
                selected = selected == type,
                onClick = { onEvent(SearchEvent.FilterChange(type)) },
            )
        }
    }
}

/** Пустой запрос — подсказки, набранный — выдача; клавиатура не закрывает последние строки. */
@Composable
private fun SearchBody(state: SearchState, onEvent: (SearchEvent) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(bottom = LocalNavBarPadding.current),
    ) {
        if (state.searching) results(state, onEvent) else suggestions(state, onEvent)
    }
}

private fun LazyListScope.suggestions(state: SearchState, onEvent: (SearchEvent) -> Unit) {
    if (state.recentQueries.isNotEmpty()) {
        item(key = "recent-title") { SuggestionTitle("Недавние", onClear = { onEvent(SearchEvent.ClearRecent) }) }
        items(state.recentQueries, key = { "recent-$it" }) { query ->
            FilmaxSuggestionRow(
                text = query,
                kind = SuggestionKind.Recent,
                onClick = { onEvent(SearchEvent.SubmitQuery(query)) },
            )
        }
    }
    item(key = "trending-title") { SuggestionTitle("Часто ищут", onClear = null) }
    items(state.trendingQueries, key = { "trending-$it" }) { query ->
        FilmaxSuggestionRow(
            text = query,
            kind = SuggestionKind.Trending,
            onClick = { onEvent(SearchEvent.SubmitQuery(query)) },
        )
    }
}

private fun LazyListScope.results(state: SearchState, onEvent: (SearchEvent) -> Unit) {
    when {
        state.loading && state.results.isEmpty() -> item(key = "loading") {
            Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FilmaxTheme.palette.accent)
            }
        }
        state.results.isEmpty() -> item(key = "empty") {
            FilmaxEmptyState(
                icon = if (state.error == null) Icons.Filled.SearchOff else Icons.Filled.CloudOff,
                title = if (state.error == null) "Ничего не нашлось" else "Не удалось найти",
                subtitle = state.error ?: "Попробуйте другое название",
                modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
            )
        }
        else -> item(key = "count") {
            Text(
                "Найдено ${state.results.size}",
                style = FilmaxType.meta,
                color = FilmaxColors.TextDim,
                modifier = Modifier.padding(horizontal = FilmaxMetrics.ScreenPadding).padding(bottom = 6.dp),
            )
        }
    }
    items(state.results, key = { it.id }) { item ->
        ResultRow(item = item, query = state.query, onEvent = onEvent)
    }
}

@Composable
private fun ResultRow(item: Item, query: String, onEvent: (SearchEvent) -> Unit) {
    FilmaxSearchResultRow(
        title = item.title,
        query = query,
        posterUrl = item.posters.small.ifEmpty { item.posters.medium },
        onClick = { onEvent(SearchEvent.OpenResult(item.id)) },
        rating = ratingLabel(item.rating.external),
        meta = posterMeta(typeLabel(item.type), item.year),
        onInsert = { onEvent(SearchEvent.QueryChange(item.title)) },
    )
}

/** Надзаголовок группы подсказок; у недавних справа — «Очистить». */
@Composable
private fun SuggestionTitle(title: String, onClear: (() -> Unit)?) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = FilmaxMetrics.ScreenPadding, end = 8.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title.uppercase(),
            style = FilmaxType.overline,
            color = FilmaxColors.TextDim,
            modifier = Modifier.weight(1f),
        )
        onClear?.let {
            FilmaxTextButton(text = "Очистить", onClick = it)
        }
    }
}
