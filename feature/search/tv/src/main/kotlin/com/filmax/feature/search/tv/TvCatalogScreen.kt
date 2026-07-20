package com.filmax.feature.search.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filmax.core.domain.catalog.CatalogFilters
import com.filmax.core.domain.catalog.CatalogSort
import com.filmax.core.domain.catalog.SortOption
import com.filmax.core.domain.catalog.model.Genre
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.tv.designsystem.ScrollToTopOnNavFocus
import com.filmax.core.tv.designsystem.TvChip
import com.filmax.core.tv.designsystem.TvFocusCard
import com.filmax.core.tv.designsystem.TvMetrics
import com.filmax.core.tv.designsystem.TvOnSurface
import com.filmax.core.tv.designsystem.TvOnSurfaceDim
import com.filmax.core.tv.designsystem.TvOnSurfaceVariant
import com.filmax.core.tv.designsystem.TvPosterCard
import com.filmax.core.tv.designsystem.TvReturnFocus
import com.filmax.core.tv.designsystem.TvSurface
import com.filmax.core.tv.designsystem.TvSurfaceContainer
import com.filmax.core.tv.designsystem.TvSurfaceContainerHighest
import com.filmax.core.tv.designsystem.posterMeta
import com.filmax.core.tv.designsystem.ratingLabel
import com.filmax.core.tv.designsystem.rememberTvReturnFocus
import com.filmax.core.ui.components.PosterImage
import com.filmax.feature.search.common.SearchEvent
import com.filmax.feature.search.common.SearchScreenModel
import com.filmax.feature.search.common.SearchState
import org.koin.androidx.compose.koinViewModel

/** Сетка постеров: 4×190dp + 3×18dp зазора ровно ложатся в 844dp между safe area. */
private const val GRID_COLUMNS = 4

/** Типы в порядке чипов. null — «Все» (объединение типов, см. SearchScreenModel). */
private val TypeOptions = listOf(
    null to "Все",
    ItemType.MOVIE to "Фильмы",
    ItemType.SERIES to "Сериалы",
    ItemType.ANIME to "Аниме",
    ItemType.DOCUMENTARY to "Документальные",
)

/** Порядок перебора чипа сортировки (OK листает поле по кругу). Подписи — из макета. */
private val SortOptions = listOf(
    CatalogSort.UPDATED to "Обновлённые",
    CatalogSort.CREATED to "Добавленные",
    CatalogSort.RATING to "Рейтинг Filmax",
    CatalogSort.VIEWS to "Просмотры",
    CatalogSort.YEAR to "Год",
    CatalogSort.KINOPOISK_RATING to "Рейтинг КП",
    CatalogSort.IMDB_RATING to "Рейтинг IMDb",
)

/**
 * TV-Каталог (экран «Каталог» макета) — витрина, а не строка поиска: сетка постеров живёт по
 * фильтрам тип/жанр/сортировка и наполнена ещё до того, как зритель набрал первую букву. Текст
 * набирают в оверлее [TvKeyboardOverlay], и только когда без него не обойтись.
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
    var keyboardOpen by remember { mutableStateOf(false) }
    var restoreFocus by remember { mutableStateOf(false) }
    val searchBarFocus = remember { FocusRequester() }
    val listState = rememberLazyListState()

    // Витрину и жанры тянем только здесь: телефонный поиск с тем же ScreenModel показывает
    // подсказки, и выдача каталога ему не нужна.
    LaunchedEffect(Unit) { screenModel.dispatch(SearchEvent.LoadCatalog) }

    // Клавиатура снимает каталог из композиции целиком — это ловушка фокуса (иначе D-pad
    // уходил бы сквозь оверлей на карточки под ним). После закрытия фокус возвращаем на строку
    // поиска: кадр ожидания нужен, чтобы ленивая сетка успела разложить её обратно.
    LaunchedEffect(restoreFocus) {
        if (!restoreFocus) return@LaunchedEffect
        withFrameNanos { }
        runCatching { searchBarFocus.requestFocus() }
        restoreFocus = false
    }

    Box(modifier.fillMaxSize().background(TvSurface)) {
        if (keyboardOpen) {
            TvKeyboardOverlay(
                state = state,
                actions = TvKeyboardActions(
                    onQuery = { screenModel.dispatch(SearchEvent.QueryChange(it)) },
                    onSubmit = { screenModel.dispatch(SearchEvent.SubmitQuery(it)) },
                    onOpenItem = onOpenItem,
                    onClose = {
                        keyboardOpen = false
                        restoreFocus = true
                    },
                ),
            )
        } else {
            CatalogContent(
                state = state,
                listState = listState,
                searchBarFocus = searchBarFocus,
                actions = CatalogActions(
                    onOpenItem = onOpenItem,
                    onOpenKeyboard = { keyboardOpen = true },
                    onFilter = { screenModel.dispatch(SearchEvent.FilterChange(it)) },
                    onSort = { screenModel.dispatch(SearchEvent.SortChange(it)) },
                    onGenre = { screenModel.dispatch(SearchEvent.GenreChange(it)) },
                    onApplyFilters = { screenModel.dispatch(SearchEvent.ApplyFilters(it)) },
                ),
            )
        }
    }
}

/** Действия каталога одним объектом — как TvHomeActions на главной. */
private data class CatalogActions(
    val onOpenItem: (Int) -> Unit,
    val onOpenKeyboard: () -> Unit,
    val onFilter: (ItemType?) -> Unit,
    val onSort: (SortOption) -> Unit,
    val onGenre: (Int?) -> Unit,
    val onApplyFilters: (CatalogFilters) -> Unit,
)

@Composable
private fun CatalogContent(
    state: SearchState,
    listState: LazyListState,
    searchBarFocus: FocusRequester,
    actions: CatalogActions,
) {
    ScrollToTopOnNavFocus(listState)
    val returnFocus = rememberTvReturnFocus()
    val gridItems = state.visibleItems
    // Постеры бьём на ряды по GRID_COLUMNS и кладём в ОБЩИЙ LazyColumn вместе с шапкой — так
    // скроллится ВЕСЬ экран (шапка уезжает вверх), а не только сетка под фиксированной шапкой,
    // которая занимала пол-экрана. Фокус ходит одним списком: «вниз» из чипов идёт в постеры,
    // «вверх» с первого ряда прокручивает шапку обратно и уходит на таб-бар (как на Главной).
    val rows = remember(gridItems) { gridItems.chunked(GRID_COLUMNS) }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = TvMetrics.ContentTop,
            bottom = TvMetrics.SafeVertical + TvMetrics.FocusInset,
        ),
        verticalArrangement = Arrangement.spacedBy(TvMetrics.CardGap),
    ) {
        item(key = "header") {
            CatalogHeader(
                state = state,
                searchBarFocus = searchBarFocus,
                actions = actions,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (gridItems.isEmpty() && !state.loading) {
            item(key = "empty") { CatalogEmpty() }
        }
        itemsIndexed(rows, key = { index, _ -> "row-$index" }) { _, row ->
            CatalogPosterRow(row = row, onOpenItem = actions.onOpenItem, returnFocus = returnFocus)
        }
    }
}

/**
 * Ряд сетки: до [GRID_COLUMNS] постеров фиксированной ширины с шагом CardGap, по левому краю
 * safe area. Хвостовые пустые ячейки не добираем — последний ряд просто короче.
 */
@Composable
private fun CatalogPosterRow(row: List<Item>, onOpenItem: (Int) -> Unit, returnFocus: TvReturnFocus) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = TvMetrics.SafeHorizontal),
        horizontalArrangement = Arrangement.spacedBy(TvMetrics.CardGap),
    ) {
        row.forEach { item ->
            CatalogPoster(
                item = item,
                focusRequester = returnFocus.target("grid:${item.id}"),
                onClick = {
                    returnFocus.onOpen("grid:${item.id}")
                    onOpenItem(item.id)
                },
            )
        }
    }
}

@Composable
private fun CatalogHeader(
    state: SearchState,
    searchBarFocus: FocusRequester,
    actions: CatalogActions,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        CatalogSearchBar(
            query = state.query,
            focusRequester = searchBarFocus,
            onClick = actions.onOpenKeyboard,
        )
        Spacer(Modifier.height(16.dp))
        // Явная связь «вниз»: ряд типов → первый жанр. Спатиальный поиск здесь ненадёжен:
        // с focusRestorer на обоих рядах DOWN проскакивал жанры и падал сразу в сетку постеров.
        val firstGenreFocus = remember { FocusRequester() }
        val hasGenres = state.genres.isNotEmpty()
        CatalogTypeRow(
            state = state,
            actions = actions,
            downFocus = firstGenreFocus.takeIf { hasGenres },
        )
        if (hasGenres) {
            Spacer(Modifier.height(12.dp))
            CatalogGenreRow(
                genres = state.genres,
                selectedId = state.selectedGenreId,
                onGenre = actions.onGenre,
                firstChipFocus = firstGenreFocus,
            )
        }
        Spacer(Modifier.height(22.dp))
        Text(
            text = catalogSummary(state),
            style = MaterialTheme.typography.bodySmall,
            color = TvOnSurfaceDim,
            modifier = Modifier.padding(horizontal = TvMetrics.SafeHorizontal),
        )
    }
}

/** Строка поиска. Это кнопка, а не поле: набор текста живёт в оверлее клавиатуры. */
@Composable
private fun CatalogSearchBar(
    query: String,
    focusRequester: FocusRequester,
    onClick: () -> Unit,
) {
    TvFocusCard(
        onClick = onClick,
        shape = TvMetrics.PanelShape,
        focusRequester = focusRequester,
        // Строка поиска — не чип-ряд: остаётся в safe-области собственным отступом.
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = TvMetrics.SafeHorizontal)
            .height(56.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clip(TvMetrics.PanelShape)
                .background(TvSurfaceContainer)
                .padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = TvOnSurfaceDim,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = query.ifEmpty { "Название фильма или сериала" },
                style = MaterialTheme.typography.titleMedium,
                color = if (query.isEmpty()) TvOnSurfaceDim else TvOnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun CatalogTypeRow(
    state: SearchState,
    actions: CatalogActions,
    downFocus: FocusRequester?,
) {
    val sort = state.sort
    val filters = state.filters
    var filtersOpen by remember { mutableStateOf(false) }
    // Первый вход фокуса в ряд — всегда на первый чип (fallback focusRestorer): без него D-pad
    // сажал фокус на пространственно-ближайший чип в середине ряда (строка поиска сверху и сетка
    // снизу — во всю ширину). Повторные входы восстанавливают последний сфокусированный.
    val firstTypeChipFocus = remember { FocusRequester() }
    // «Вниз» с любого чипа — на первый жанр. Свойство стоит на КАЖДОМ чипе: focusProperties
    // контейнера на детей не распространяется, и спатиальный поиск скипал ряд жанров в сетку.
    val chipModifier = Modifier.focusProperties { downFocus?.let { down = it } }
    // Горизонтальный скролл, а не Row: тип + сортировка + «Фильтры» не влезали в safe area, и
    // последний чип клипился. Разделители-палочки убраны — от них между группами зиял большой
    // отступ; теперь шаг между всеми чипами одинаковый. offset/contentPadding — как у ряда жанров.
    LazyRow(
        modifier = Modifier.fillMaxWidth().focusRestorer(firstTypeChipFocus),
        contentPadding = PaddingValues(horizontal = TvMetrics.SafeHorizontal),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        itemsIndexed(TypeOptions) { index, (type, label) ->
            TvChip(
                label = label,
                selected = state.filter == type,
                onClick = { actions.onFilter(type) },
                modifier = if (index == 0) chipModifier.focusRequester(firstTypeChipFocus) else chipModifier,
            )
        }
        // Поле сортировки: OK листает по кругу. Стрелка ↕ (U+2195), а не ⇅ из макета: у второй
        // покрытие во встроенных шрифтах Android TV не гарантировано.
        item {
            TvChip(
                label = "↕ ${sortLabel(sort.field)}",
                selected = false,
                onClick = { actions.onSort(SortOption(nextSort(sort.field), sort.ascending)) },
                modifier = chipModifier,
            )
        }
        // Направление: ↑ по возрастанию (kino.pub `-field`), ↓ по убыванию.
        item {
            TvChip(
                label = if (sort.ascending) "↑ Возр." else "↓ Убыв.",
                selected = false,
                onClick = { actions.onSort(sort.copy(ascending = !sort.ascending)) },
                modifier = chipModifier,
            )
        }
        // Полный набор фильтров (год, рейтинги, страна, 4K, завершённость) — в оверлей-панели.
        item {
            TvChip(
                label = if (filters.activeCount > 0) "Фильтры · ${filters.activeCount}" else "Фильтры",
                selected = filters.activeCount > 0,
                onClick = { filtersOpen = true },
                modifier = chipModifier,
            )
        }
    }
    if (filtersOpen) {
        TvCatalogFilterDialog(
            current = filters,
            countries = state.countries,
            onApply = { actions.onApplyFilters(it) },
            onDismiss = { filtersOpen = false },
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun CatalogGenreRow(
    genres: List<Genre>,
    selectedId: Int?,
    onGenre: (Int?) -> Unit,
    /** Привязывается к первому жанру: fallback focusRestorer и цель `down` ряда типов. */
    firstChipFocus: FocusRequester,
) {
    LazyRow(
        // От края до края: viewport на всю ширину, а первый/последний чип держит на линии safe area
        // contentPadding. Так чипы скроллятся к самым краям экрана, а не обрываются на safe-границе.
        modifier = Modifier.fillMaxWidth().focusRestorer(firstChipFocus),
        contentPadding = PaddingValues(horizontal = TvMetrics.SafeHorizontal),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        itemsIndexed(genres, key = { _, genre -> genre.id }) { index, genre ->
            TvChip(
                label = genre.title,
                selected = genre.id == selectedId,
                // Повторный OK по выбранному жанру снимает фильтр — отдельного чипа «Все» в ряду нет.
                onClick = { onGenre(if (genre.id == selectedId) null else genre.id) },
                modifier = if (index == 0) Modifier.focusRequester(firstChipFocus) else Modifier,
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
private fun CatalogPoster(item: Item, focusRequester: FocusRequester?, onClick: () -> Unit) {
    TvPosterCard(
        title = item.title,
        meta = posterMeta(itemTypeLabel(item.type), item.year),
        posterUrl = item.posters.medium.ifEmpty { item.posters.big },
        onClick = onClick,
        rating = formatRating(item.rating.external),
        focusRequester = focusRequester,
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

/** Описание текущей выборки: `Фильмы · Драма · 24 результата` (макет: catFilterLabel). */
private fun catalogSummary(state: SearchState): String {
    val parts = buildList {
        add(typeLabel(state.filter))
        state.genres.firstOrNull { it.id == state.selectedGenreId }?.let { add(it.title) }
        add(resultsCount(state.visibleItems.size))
    }
    return parts.joinToString(" · ")
}

/** Подпись чипа-фильтра: множественное число. */
private fun typeLabel(type: ItemType?): String =
    TypeOptions.firstOrNull { it.first == type }?.second ?: TypeOptions.first().second

/** Подпись под карточкой — единственное число, в отличие от чипа-фильтра. */
private fun itemTypeLabel(type: ItemType): String = when (type) {
    ItemType.MOVIE -> "Фильм"
    ItemType.SERIES -> "Сериал"
    ItemType.ANIME -> "Аниме"
    ItemType.DOCUMENTARY -> "Документальный"
    ItemType.TV -> "ТВ"
}

private fun sortLabel(sort: CatalogSort): String =
    SortOptions.firstOrNull { it.first == sort }?.second ?: SortOptions.first().second

private fun nextSort(current: CatalogSort): CatalogSort {
    val index = SortOptions.indexOfFirst { it.first == current }
    return SortOptions[(index + 1) % SortOptions.size].first
}

/** «24 результата» — с русским числительным, иначе строка читается как машинный лог. */
private fun resultsCount(count: Int): String {
    val word = when {
        count % HUNDRED in TEENS -> "результатов"
        count % TEN == 1 -> "результат"
        count % TEN in FEW -> "результата"
        else -> "результатов"
    }
    return "$count $word"
}

/**
 * Усреднённая внешняя оценка как «8.3». Точка, а не локальная запятая: шкала международная.
 * Ноль отсекается в [ratingLabel] — у kino.pub это «оценки нет», а не «ноль баллов».
 */
internal fun formatRating(rating: Double?): String? = ratingLabel(rating)

private const val TEN = 10
private const val HUNDRED = 100
private val TEENS = 11..14
private val FEW = 2..4
