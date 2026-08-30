package com.filmax.feature.search.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.filmax.core.domain.catalog.CatalogFilters
import com.filmax.core.domain.catalog.SortOption
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.tv.designsystem.ScrollToTopOnNavFocus
import com.filmax.core.tv.designsystem.TvMetrics
import com.filmax.core.tv.designsystem.TvOnSurface
import com.filmax.core.tv.designsystem.TvOnSurfaceVariant
import com.filmax.core.tv.designsystem.TvPosterCard
import com.filmax.core.tv.designsystem.TvScreenFocus
import com.filmax.core.tv.designsystem.TvSurface
import com.filmax.core.tv.designsystem.TvSurfaceContainer
import com.filmax.core.tv.designsystem.TvSurfaceContainerHighest
import com.filmax.core.tv.designsystem.rememberTvScreenFocus
import com.filmax.core.ui.components.PosterImage
import com.filmax.core.ui.components.VoiceListeningDialog
import com.filmax.core.ui.components.posterMeta
import com.filmax.core.ui.components.ratingLabel
import com.filmax.core.ui.components.rememberInAppVoiceSearch
import com.filmax.core.ui.components.typeLabel
import com.filmax.feature.search.common.SearchEvent
import com.filmax.feature.search.common.SearchScreenModel
import com.filmax.feature.search.common.SearchState
import org.koin.androidx.compose.koinViewModel

/** Сетка постеров: 4×190dp + 3×18dp зазора ровно ложатся в 844dp между safe area. */
private const val GRID_COLUMNS = 4

/** За сколько хвостовых рядов сетки до конца просить следующую страницу витрины. */
private const val LOAD_MORE_TAIL = 3

/** Ключ фокуса строки поиска — стартовая цель экрана и точка возврата. */
private const val SEARCH_KEY = "search"

/** Высота строки поиска: одна на кнопку и на поле ввода, чтобы шапка не прыгала. */
internal val SearchBarHeight = 56.dp

/**
 * TV-Каталог (экран «Каталог» макета) — витрина, а не строка поиска: сетка постеров живёт по
 * фильтрам тип/жанр/сортировка и наполнена ещё до того, как зритель набрал первую букву.
 *
 * Текст набирают системной клавиатурой телевизора: строка поиска — обычное поле ввода, и по
 * «ОК» на нём открывается та же клавиатура, к которой зритель привык в остальных приложениях,
 * с его раскладками, историей и голосовым вводом самой платформы. Своя экранная клавиатура тут
 * была: три раскладки, свой курсор и своя живая выдача рядом — четыреста строк, повторявших
 * платформу хуже неё самой. Выдачу показывает сама сетка: `visibleItems` переключается на
 * результаты, как только в запросе набирается пара символов.
 *
 * Поверх общего [SearchScreenModel] — тот же debounce-поиск, что и на телефоне.
 */
@Composable
fun TvCatalogScreen(
    onOpenItem: (Int) -> Unit,
    modifier: Modifier = Modifier,
    screenModel: SearchScreenModel = koinViewModel(),
) {
    val state by screenModel.collectAsState()
    val focus = rememberTvScreenFocus(startAt = SEARCH_KEY)
    val listState = rememberLazyListState()

    // Голос слушаем внутри приложения (SpeechRecognizer), без стороннего экрана распознавания.
    val voice = rememberInAppVoiceSearch { spoken ->
        screenModel.dispatch(SearchEvent.SubmitQuery(spoken))
    }
    VoiceListeningDialog(voice)

    // Витрину и жанры тянем только здесь: телефонный поиск с тем же ScreenModel показывает
    // подсказки, и выдача каталога ему не нужна.
    LaunchedEffect(Unit) { screenModel.dispatch(SearchEvent.LoadCatalog) }

    Box(modifier.fillMaxSize().background(TvSurface)) {
        CatalogContent(
            state = state,
            listState = listState,
            focus = focus,
            actions = CatalogActions(
                onOpenItem = onOpenItem,
                onQuery = { screenModel.dispatch(SearchEvent.QueryChange(it)) },
                onVoice = voice::start,
                onEditingFinished = { focus.focusOn(SEARCH_KEY) },
                onFilter = { screenModel.dispatch(SearchEvent.FilterChange(it)) },
                onSort = { screenModel.dispatch(SearchEvent.SortChange(it)) },
                onGenre = { screenModel.dispatch(SearchEvent.GenreChange(it)) },
                onApplyFilters = { screenModel.dispatch(SearchEvent.ApplyFilters(it)) },
            ),
            onLoadMore = { screenModel.dispatch(SearchEvent.LoadMoreCatalog) },
        )
    }
}

/** Действия каталога одним объектом — как TvHomeActions на главной. */
internal data class CatalogActions(
    val onOpenItem: (Int) -> Unit,
    val onQuery: (String) -> Unit,
    val onVoice: () -> Unit,
    /** Ввод закончен: фокус возвращаем на строку поиска, уже снова кнопку. */
    val onEditingFinished: () -> Unit,
    val onFilter: (ItemType?) -> Unit,
    val onSort: (SortOption) -> Unit,
    val onGenre: (Int?) -> Unit,
    val onApplyFilters: (CatalogFilters) -> Unit,
)

@Composable
private fun CatalogContent(
    state: SearchState,
    listState: LazyListState,
    focus: TvScreenFocus,
    actions: CatalogActions,
    onLoadMore: () -> Unit,
) {
    ScrollToTopOnNavFocus(listState)
    val gridItems = state.visibleItems
    // Постеры бьём на ряды по GRID_COLUMNS и кладём в ОБЩИЙ LazyColumn вместе с шапкой — так
    // скроллится ВЕСЬ экран (шапка уезжает вверх), а не только сетка под фиксированной шапкой,
    // которая занимала пол-экрана. Фокус ходит одним списком: «вниз» из чипов идёт в постеры,
    // «вверх» с первого ряда прокручивает шапку обратно и уходит на таб-бар (как на Главной).
    val rows = remember(gridItems) { gridItems.chunked(GRID_COLUMNS) }

    // Догрузка витрины: фокус/скролл в LOAD_MORE_TAIL хвостовых рядах — просим следующую
    // страницу. derivedStateOf пересчитывается без рекомпозиции, дёргает её только смена
    // «пора/не пора»; повторные вызовы гасит идемпотентность модели.
    val loadMore by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && lastVisible >= info.totalItemsCount - LOAD_MORE_TAIL
        }
    }
    LaunchedEffect(loadMore) { if (loadMore) onLoadMore() }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().then(focus.containerModifier),
        contentPadding = PaddingValues(
            top = TvMetrics.ContentTop,
            bottom = TvMetrics.SafeVertical + TvMetrics.FocusInset,
        ),
        verticalArrangement = Arrangement.spacedBy(TvMetrics.CardGap),
    ) {
        item(key = "header") {
            CatalogHeader(
                state = state,
                searchModifier = focus.item(SEARCH_KEY),
                actions = actions,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (gridItems.isEmpty() && !state.loading) {
            item(key = "empty") { CatalogEmpty() }
        }
        itemsIndexed(rows, key = { index, _ -> "row-$index" }) { _, row ->
            CatalogPosterRow(row = row, onOpenItem = actions.onOpenItem, focus = focus)
        }
        if (state.catalogLoadingMore) {
            item(key = "loading_more") { CatalogLoadingMore() }
        }
    }
}

/** Хвостовой индикатор догрузки страницы — невысокий, чтобы не дёргать сетку. */
@Composable
private fun CatalogLoadingMore() {
    Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = TvOnSurfaceVariant, modifier = Modifier.size(28.dp))
    }
}

/**
 * Ряд сетки: до [GRID_COLUMNS] постеров фиксированной ширины с шагом CardGap, по левому краю
 * safe area. Хвостовые пустые ячейки не добираем — последний ряд просто короче.
 */
@Composable
private fun CatalogPosterRow(row: List<Item>, onOpenItem: (Int) -> Unit, focus: TvScreenFocus) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = TvMetrics.SafeHorizontal),
        horizontalArrangement = Arrangement.spacedBy(TvMetrics.CardGap),
    ) {
        row.forEach { item ->
            CatalogPoster(
                item = item,
                modifier = focus.item("grid:${item.id}"),
                onClick = { onOpenItem(item.id) },
            )
        }
    }
}

@Composable
private fun CatalogEmpty() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            Icons.Filled.SearchOff,
            contentDescription = null,
            tint = TvSurfaceContainerHighest,
            modifier = Modifier.size(34.dp),
        )
        Text("Ничего не найдено", style = MaterialTheme.typography.titleMedium, color = TvOnSurface)
        Text(
            "Измените фильтры или запрос",
            style = MaterialTheme.typography.bodyMedium,
            color = TvOnSurfaceVariant,
        )
    }
}

@Composable
private fun CatalogPoster(item: Item, modifier: Modifier, onClick: () -> Unit) {
    TvPosterCard(
        title = item.title,
        meta = posterMeta(typeLabel(item.type), item.year),
        posterUrl = item.posters.medium.ifEmpty { item.posters.big },
        onClick = onClick,
        modifier = modifier,
        rating = ratingLabel(item.rating.external),
    ) { url, posterModifier ->
        PosterImage(
            url = url,
            contentDescription = item.title,
            modifier = posterModifier,
            shape = TvMetrics.PosterShape,
            // Плейсхолдер-градиент по умолчанию розовый; в монохроме под постером — поверхность.
            accentColor = TvSurfaceContainer,
        )
    }
}
