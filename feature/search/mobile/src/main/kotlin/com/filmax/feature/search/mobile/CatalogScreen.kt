package com.filmax.feature.search.mobile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.FilmaxOnSurfaceDim
import com.filmax.core.designsystem.ShapeButton
import com.filmax.core.designsystem.ShapeFull
import com.filmax.core.domain.catalog.CatalogSort
import com.filmax.core.domain.catalog.SortOption
import com.filmax.core.domain.catalog.model.Genre
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.ui.components.FilmaxEmptyState
import com.filmax.core.ui.components.FilmaxPosterCard
import com.filmax.core.ui.components.posterMeta
import com.filmax.core.ui.components.ratingLabel
import com.filmax.core.ui.components.rememberVoiceSearch
import com.filmax.feature.search.common.MIN_QUERY_LENGTH
import com.filmax.feature.search.common.SearchEvent
import com.filmax.feature.search.common.SearchScreenModel
import com.filmax.feature.search.common.SearchState
import org.koin.androidx.compose.koinViewModel

/** Сетка каталога: 3×98dp постера + 2×12dp зазора + поля 20dp ровно ложатся в кадр 360dp. */
private const val GRID_COLUMNS = 3

private const val SEARCH_PLACEHOLDER = "Поиск фильмов, сериалов, людей"

/** Типы в порядке чипов. null — «Все» (объединение типов, см. SearchScreenModel). */
private val TypeOptions = listOf(
    null to "Все",
    ItemType.MOVIE to "Фильмы",
    ItemType.SERIES to "Сериалы",
    ItemType.ANIME to "Аниме",
    ItemType.DOCUMENTARY to "Документальные",
)

/** Поля сортировки в порядке меню. Русские подписи — из макета «Filmax Mobile». */
private val SortOptions = listOf(
    CatalogSort.UPDATED to "Обновлённые",
    CatalogSort.CREATED to "Добавленные",
    CatalogSort.RATING to "Рейтинг Filmax",
    CatalogSort.VIEWS to "Просмотры",
    CatalogSort.YEAR to "Год",
    CatalogSort.KINOPOISK_RATING to "Рейтинг КП",
    CatalogSort.IMDB_RATING to "Рейтинг IMDb",
)

/** Действия каталога одним объектом: у detekt порог LongParameterList — 6. */
private data class CatalogActions(
    val onOpenItem: (Int) -> Unit,
    val onOpenSearch: () -> Unit,
    val onFilter: (ItemType?) -> Unit,
    val onSort: (SortOption) -> Unit,
    val onGenre: (Int?) -> Unit,
    val onOpenFilters: () -> Unit,
)

private data class SearchActions(
    val onQueryChange: (String) -> Unit,
    val onSubmitQuery: (String) -> Unit,
    val onOpenItem: (Int) -> Unit,
    val onClose: () -> Unit,
)

/**
 * Каталог (экран «Каталог» макета) — витрина, а не строка поиска: сетка постеров наполнена
 * фильтрами тип/жанр/сортировка ещё до того, как зритель набрал первую букву. Строка поиска
 * здесь — кнопка: набор текста живёт в отдельном режиме [SearchOverlay], где под поле отдана
 * вся шапка, а системная клавиатура открыта сразу.
 *
 * Поверх общего [SearchScreenModel] — тот же debounce-поиск, что и на TV.
 */
@Composable
fun CatalogScreen(
    onOpenItem: (Int) -> Unit,
    modifier: Modifier = Modifier,
    screenModel: SearchScreenModel = koinViewModel(),
) {
    val state by screenModel.collectAsState()
    var searchMode by remember { mutableStateOf(false) }
    var filtersOpen by remember { mutableStateOf(false) }

    // Витрину и жанры общий ScreenModel тянет только по заявке экрана — без этого сетка пуста.
    LaunchedEffect(Unit) { screenModel.dispatch(SearchEvent.LoadCatalog) }

    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        if (searchMode) {
            SearchOverlay(
                state = state,
                actions = SearchActions(
                    onQueryChange = { screenModel.dispatch(SearchEvent.QueryChange(it)) },
                    onSubmitQuery = { screenModel.dispatch(SearchEvent.SubmitQuery(it)) },
                    onOpenItem = onOpenItem,
                    onClose = { searchMode = false },
                ),
            )
        } else {
            CatalogContent(
                state = state,
                actions = CatalogActions(
                    onOpenItem = onOpenItem,
                    onOpenSearch = { searchMode = true },
                    onFilter = { screenModel.dispatch(SearchEvent.FilterChange(it)) },
                    onSort = { screenModel.dispatch(SearchEvent.SortChange(it)) },
                    onGenre = { screenModel.dispatch(SearchEvent.GenreChange(it)) },
                    onOpenFilters = { filtersOpen = true },
                ),
            )
        }
    }

    if (filtersOpen) {
        CatalogFilterSheet(
            filters = state.filters,
            countries = state.countries,
            onApply = {
                screenModel.dispatch(SearchEvent.ApplyFilters(it))
                filtersOpen = false
            },
            onReset = {
                screenModel.dispatch(SearchEvent.ResetFilters)
                filtersOpen = false
            },
            onDismiss = { filtersOpen = false },
        )
    }
}

@Composable
private fun CatalogContent(state: SearchState, actions: CatalogActions) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(GRID_COLUMNS),
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(
            start = FilmaxMetrics.ScreenPadding,
            end = FilmaxMetrics.ScreenPadding,
            top = 6.dp,
            bottom = FilmaxMetrics.ScreenPadding,
        ),
        horizontalArrangement = Arrangement.spacedBy(FilmaxMetrics.CardGap),
        verticalArrangement = Arrangement.spacedBy(FilmaxMetrics.CardGap),
    ) {
        item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
            CatalogHeader(state = state, actions = actions)
        }
        posterItems(
            state = state,
            emptySubtitle = "Измените фильтры или запрос",
            onOpenItem = actions.onOpenItem,
        )
    }
}

@Composable
private fun CatalogHeader(state: SearchState, actions: CatalogActions) {
    Column {
        CatalogTitleRow(
            sort = state.sort,
            filterCount = state.filters.activeCount,
            onSort = actions.onSort,
            onOpenFilters = actions.onOpenFilters,
        )
        Spacer(Modifier.height(16.dp))
        CatalogSearchButton(query = state.query, onClick = actions.onOpenSearch)
        Spacer(Modifier.height(16.dp))
        CatalogTypeRow(filter = state.filter, onFilter = actions.onFilter)
        if (state.genres.isNotEmpty()) {
            Spacer(Modifier.height(11.dp))
            CatalogGenreRow(
                genres = state.genres,
                selectedId = state.selectedGenreId,
                onGenre = actions.onGenre,
            )
        }
        Spacer(Modifier.height(6.dp))
    }
}

/** Сортировка и фильтры живут в строке заголовка: место рядом с «Каталогом» в макете пустует. */
@Composable
private fun CatalogTitleRow(
    sort: SortOption,
    filterCount: Int,
    onSort: (SortOption) -> Unit,
    onOpenFilters: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Каталог",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterButton(count = filterCount, onClick = onOpenFilters)
            SortControl(sort = sort, onSort = onSort)
        }
    }
}

/** Кнопка листа фильтров с бейджем: число активных фильтров прямо на иконке. */
@Composable
private fun FilterButton(count: Int, onClick: () -> Unit) {
    Box(contentAlignment = Alignment.TopEnd) {
        Row(
            modifier = Modifier
                .height(FilmaxMetrics.GenreChipHeight)
                .clip(ShapeFull)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(
                Icons.Filled.Tune,
                contentDescription = "Фильтры",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp),
            )
            Text(
                "Фильтры",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (count > 0) {
            Box(
                modifier = Modifier
                    .offset(x = 4.dp, y = (-4).dp)
                    .size(16.dp)
                    .clip(ShapeFull)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Выбор поля сортировки (меню) плюс стрелка направления рядом. */
@Composable
private fun SortControl(sort: SortOption, onSort: (SortOption) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SortFieldChip(field = sort.field) { field -> onSort(SortOption(field, sort.ascending)) }
        DirectionToggle(ascending = sort.ascending) { onSort(sort.copy(ascending = !sort.ascending)) }
    }
}

/**
 * Выпадающее меню полей, а не перебор по кругу (как на пульте): пальцем вариант выбирают из
 * списка, и текущее поле должно быть видно вместе с остальными.
 */
@Composable
private fun SortFieldChip(field: CatalogSort, onPick: (CatalogSort) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .height(FilmaxMetrics.GenreChipHeight)
                .clip(ShapeFull)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable { expanded = true }
                .padding(start = 14.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                sortLabel(field),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Icon(
                Icons.Filled.ExpandMore,
                contentDescription = "Сортировка",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SortOptions.forEach { (option, label) ->
                SortMenuItem(
                    label = label,
                    selected = option == field,
                    onClick = {
                        expanded = false
                        onPick(option)
                    },
                )
            }
        }
    }
}

/** Стрелка направления: вверх — по возрастанию, вниз — по убыванию (kino.pub `-field`). */
@Composable
private fun DirectionToggle(ascending: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(FilmaxMetrics.GenreChipHeight)
            .clip(ShapeFull)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (ascending) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
            contentDescription = if (ascending) "По возрастанию" else "По убыванию",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(15.dp),
        )
    }
}

@Composable
private fun SortMenuItem(label: String, selected: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        onClick = onClick,
        trailingIcon = {
            if (selected) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
            }
        },
    )
}

/** Строка поиска каталога. Это кнопка, а не поле: текст набирают в режиме поиска. */
@Composable
private fun CatalogSearchButton(query: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(FilmaxMetrics.SearchFieldHeight)
            .clip(ShapeButton)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            Icons.Filled.Search,
            contentDescription = null,
            tint = FilmaxOnSurfaceDim,
            modifier = Modifier.size(19.dp),
        )
        Text(
            text = query.ifEmpty { SEARCH_PLACEHOLDER },
            style = MaterialTheme.typography.bodyLarge,
            color = if (query.isEmpty()) FilmaxOnSurfaceDim else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CatalogTypeRow(filter: ItemType?, onFilter: (ItemType?) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        items(TypeOptions) { (type, label) ->
            CatalogChip(
                label = label,
                selected = filter == type,
                onClick = { onFilter(type) },
                modifier = Modifier.height(FilmaxMetrics.ChipHeight),
            )
        }
    }
}

@Composable
private fun CatalogGenreRow(genres: List<Genre>, selectedId: Int?, onGenre: (Int?) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        items(genres, key = { it.id }) { genre ->
            CatalogChip(
                label = genre.title,
                selected = genre.id == selectedId,
                // Повторный тап по выбранному жанру снимает фильтр — отдельного чипа «Все» в ряду нет.
                onClick = { onGenre(if (genre.id == selectedId) null else genre.id) },
                modifier = Modifier.height(FilmaxMetrics.GenreChipHeight),
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            )
        }
    }
}

/**
 * Чип каталога. Выбранный — белая заливка с тёмным текстом: в монохроме заливка и есть
 * единственный маркер выбора, галочек и рамок макет не знает.
 */
@Composable
private fun CatalogChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
) {
    Box(
        modifier = modifier
            .clip(ShapeFull)
            .background(if (selected) MaterialTheme.colorScheme.primary else containerColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
        )
    }
}

/**
 * Режим поиска: та же выдача, но под набор текста отдана вся шапка, а клавиатура — системная
 * (в макете она нарисована лишь как иллюстрация). Пустой запрос показывает витрину каталога:
 * пустой экран с подсказками-заглушками зрителю не нужен.
 */
@Composable
private fun SearchOverlay(state: SearchState, actions: SearchActions) {
    // Пока открыт поиск, «Назад» возвращает в каталог, а не уводит с вкладки.
    BackHandler(onBack = actions.onClose)

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        SearchHeader(query = state.query, actions = actions)
        SearchResults(state = state, onOpenItem = actions.onOpenItem)
    }
}

@Composable
private fun SearchHeader(query: String, actions: SearchActions) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = FilmaxMetrics.ScreenPadding, top = 8.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IconButton(onClick = actions.onClose, modifier = Modifier.size(40.dp)) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Назад",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp),
            )
        }
        SearchInput(query = query, actions = actions, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SearchInput(query: String, actions: SearchActions, modifier: Modifier = Modifier) {
    val focusRequester = remember { FocusRequester() }
    val startVoiceSearch = rememberVoiceSearch(onResult = actions.onSubmitQuery)

    // Автофокус: в режим поиска входят, чтобы набирать, — клавиатуру система поднимет сама.
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Row(
        modifier = modifier
            .height(46.dp)
            .clip(ShapeButton)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(1.5.dp, MaterialTheme.colorScheme.surfaceContainerHighest, ShapeButton)
            .padding(horizontal = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Icon(
            Icons.Filled.Search,
            contentDescription = null,
            tint = FilmaxOnSurfaceDim,
            modifier = Modifier.size(18.dp),
        )
        SearchTextField(
            query = query,
            onQueryChange = actions.onQueryChange,
            focusRequester = focusRequester,
            modifier = Modifier.weight(1f),
        )
        SearchInputTrailing(
            query = query,
            onClear = { actions.onQueryChange("") },
            onVoiceSearch = startVoiceSearch,
        )
    }
}

@Composable
private fun SearchTextField(
    query: String,
    onQueryChange: (String) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    // TextFieldValue, а не голая строка: запрос переживает закрытие режима поиска, и без явной
    // позиции курсор при возврате встал бы перед уже набранным текстом. Голос пишет в запрос
    // мимо поля — отсюда синхронизация по [query].
    var fieldValue by remember { mutableStateOf(TextFieldValue(query, TextRange(query.length))) }
    LaunchedEffect(query) {
        if (query != fieldValue.text) fieldValue = TextFieldValue(query, TextRange(query.length))
    }

    BasicTextField(
        value = fieldValue,
        onValueChange = { value ->
            fieldValue = value
            onQueryChange(value.text)
        },
        modifier = modifier.focusRequester(focusRequester),
        textStyle = MaterialTheme.typography.titleMedium.copy(
            color = MaterialTheme.colorScheme.onSurface,
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        // Выдача и так живёт по debounce — «Найти» остаётся убрать клавиатуру с результатов.
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        decorationBox = { inner ->
            if (fieldValue.text.isEmpty()) {
                Text(
                    SEARCH_PLACEHOLDER,
                    style = MaterialTheme.typography.titleMedium,
                    color = FilmaxOnSurfaceDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            inner()
        },
    )
}

@Composable
private fun SearchInputTrailing(query: String, onClear: () -> Unit, onVoiceSearch: () -> Unit) {
    if (query.isEmpty()) {
        Icon(
            Icons.Filled.Mic,
            contentDescription = "Голосовой поиск",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp).clickable(onClick = onVoiceSearch),
        )
    } else {
        Icon(
            Icons.Filled.Close,
            contentDescription = "Очистить",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp).clickable(onClick = onClear),
        )
    }
}

@Composable
private fun SearchResults(state: SearchState, onOpenItem: (Int) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(GRID_COLUMNS),
        // Клавиатура перекрывает низ окна — без отступа последний ряд из-под неё не достать.
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(
            start = FilmaxMetrics.ScreenPadding,
            end = FilmaxMetrics.ScreenPadding,
            top = 2.dp,
            bottom = FilmaxMetrics.ScreenPadding,
        ),
        horizontalArrangement = Arrangement.spacedBy(FilmaxMetrics.CardGap),
        verticalArrangement = Arrangement.spacedBy(FilmaxMetrics.CardGap),
    ) {
        if (state.query.length >= MIN_QUERY_LENGTH) {
            item(key = "count", span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    "Результаты · ${state.results.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = FilmaxOnSurfaceDim,
                )
            }
        }
        posterItems(
            state = state,
            emptySubtitle = "Попробуйте другой запрос",
            onOpenItem = onOpenItem,
        )
    }
}

/** Тело обеих сеток: индикатор, пустое состояние или постеры. */
private fun LazyGridScope.posterItems(
    state: SearchState,
    emptySubtitle: String,
    onOpenItem: (Int) -> Unit,
) {
    val posters = state.visibleItems
    when {
        state.loading && posters.isEmpty() ->
            item(key = "loading", span = { GridItemSpan(maxLineSpan) }) { CatalogLoading() }

        posters.isEmpty() -> item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
            CatalogEmpty(error = state.error, subtitle = emptySubtitle)
        }
    }
    items(posters, key = { it.id }) { item ->
        CatalogPoster(item = item, onClick = { onOpenItem(item.id) })
    }
}

@Composable
private fun CatalogLoading() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

/** Сбой сети — не «ничего не найдено»: пустая сетка по обеим причинам, а причины разные. */
@Composable
private fun CatalogEmpty(error: String?, subtitle: String) {
    FilmaxEmptyState(
        icon = if (error == null) Icons.Filled.SearchOff else Icons.Filled.CloudOff,
        title = if (error == null) "Ничего не найдено" else "Не удалось загрузить",
        subtitle = error ?: subtitle,
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
    )
}

@Composable
private fun CatalogPoster(item: Item, onClick: () -> Unit) {
    FilmaxPosterCard(
        title = item.title,
        posterUrl = item.posters.medium.ifEmpty { item.posters.big },
        onClick = onClick,
        width = FilmaxMetrics.GridPosterWidth,
        height = FilmaxMetrics.GridPosterHeight,
        rating = ratingLabel(item.rating.external),
        meta = posterMeta(itemTypeLabel(item.type), item.year),
    )
}

/** Подпись под карточкой: тип по-русски. `serial`/`docuserial` из API зрителю не показываем. */
private fun itemTypeLabel(type: ItemType): String = when (type) {
    ItemType.MOVIE -> "Фильм"
    ItemType.SERIES -> "Сериал"
    ItemType.ANIME -> "Аниме"
    ItemType.DOCUMENTARY -> "Документальный"
    ItemType.TV -> "ТВ"
}

private fun sortLabel(sort: CatalogSort): String =
    SortOptions.firstOrNull { it.first == sort }?.second ?: SortOptions.first().second
