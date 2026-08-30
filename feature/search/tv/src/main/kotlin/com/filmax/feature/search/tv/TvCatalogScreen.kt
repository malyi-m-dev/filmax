package com.filmax.feature.search.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
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
import com.filmax.feature.search.common.SortOptions
import com.filmax.feature.search.common.TypeOptions
import com.filmax.feature.search.common.sortLabel
import kotlinx.coroutines.flow.drop
import org.koin.androidx.compose.koinViewModel

/** Сетка постеров: 4×190dp + 3×18dp зазора ровно ложатся в 844dp между safe area. */
private const val GRID_COLUMNS = 4

/** За сколько хвостовых рядов сетки до конца просить следующую страницу витрины. */
private const val LOAD_MORE_TAIL = 3

/** Ключ фокуса строки поиска — стартовая цель экрана и точка возврата. */
private const val SEARCH_KEY = "search"

/** Высота строки поиска: одна на кнопку и на поле ввода, чтобы шапка не прыгала. */
private val SearchBarHeight = 56.dp

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
private data class CatalogActions(
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
private fun CatalogHeader(
    state: SearchState,
    searchModifier: Modifier,
    actions: CatalogActions,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        CatalogSearchBar(
            query = state.query,
            onQuery = actions.onQuery,
            onVoice = actions.onVoice,
            onEditingFinished = actions.onEditingFinished,
            modifier = searchModifier,
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

/**
 * Строка поиска: поле ввода и кнопка голоса.
 *
 * Поле настоящее — «ОК» на нём открывает системную клавиатуру телевизора. Своей мы больше не
 * держим: платформенная знает раскладки зрителя, помнит, что он вводил, и умеет голос сама.
 * Рамка фокуса рисуется здесь, а не через `TvFocusCard`: у карточки фокус живёт на ней самой,
 * а поле должно получать его себе — иначе клавиатуре некуда печатать.
 */
@Composable
private fun CatalogSearchBar(
    query: String,
    onQuery: (String) -> Unit,
    onVoice: () -> Unit,
    onEditingFinished: () -> Unit,
    modifier: Modifier,
) {
    // Две роли одной строки. В навигации это кнопка: пульт ходит по экрану, стрелки достаются
    // фокусу, клавиатура не всплывает. По «ОК» строка становится полем ввода и зовёт системную
    // клавиатуру телевизора. Иначе никак: поле, получив фокус, показывает клавиатуру само — на
    // пульте она вылезала на пол-экрана каждый раз, когда фокус просто проходил мимо строки.
    var editing by rememberSaveable { mutableStateOf(false) }

    Row(
        // Строка поиска — не чип-ряд: остаётся в safe-области собственным отступом.
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = TvMetrics.SafeHorizontal),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val barModifier = modifier.weight(1f).height(SearchBarHeight)
        if (editing) {
            SearchInput(
                query = query,
                onQuery = onQuery,
                onDone = {
                    editing = false
                    onEditingFinished()
                },
                modifier = barModifier,
            )
        } else {
            SearchButton(query = query, onClick = { editing = true }, modifier = barModifier)
        }
        VoiceSearchButton(onVoice)
    }
}

/** Строка в состоянии навигации: показывает запрос и по «ОК» уступает место полю ввода. */
@Composable
private fun SearchButton(query: String, onClick: () -> Unit, modifier: Modifier) {
    TvFocusCard(onClick = onClick, shape = TvMetrics.PanelShape, modifier = modifier) {
        SearchBarSurface {
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

/**
 * Строка в состоянии ввода: настоящее поле, за которым открывается системная клавиатура —
 * та же, к которой зритель привык в остальных приложениях, с его раскладками и историей.
 *
 * Выход из ввода — «Поиск» на клавиатуре, «Назад» или уход фокуса: строка возвращается
 * в состояние кнопки, и пульт снова ходит по экрану.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchInput(
    query: String,
    onQuery: (String) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier,
) {
    val fieldState = rememberTextFieldState(query)
    val fieldFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    // Первое событие фокуса приходит ещё до запроса — «поле не в фокусе», и без этой отметки
    // строка откатывалась в кнопку в том же кадре, в котором открылась.
    var hadFocus by remember { mutableStateOf(false) }

    // Набранное уходит в модель (там debounce). Обратно текст не возвращаем: пока строка в
    // режиме ввода, источник правды — она сама, а голосовой запрос приходит уже в состоянии
    // кнопки, где текст берётся прямо из модели.
    LaunchedEffect(fieldState) {
        snapshotFlow { fieldState.text.toString() }.drop(1).collect(onQuery)
    }
    LaunchedEffect(Unit) { fieldFocus.requestFocus() }

    // Клавиатуру закрывают «Назад» — и это событие достаётся ей, а не нам. Единственный
    // надёжный признак конца ввода поэтому такой: клавиатура была на экране и ушла.
    val keyboardVisible = WindowInsets.isImeVisible
    var keyboardWasVisible by remember { mutableStateOf(false) }
    LaunchedEffect(keyboardVisible) {
        if (keyboardVisible) keyboardWasVisible = true else if (keyboardWasVisible) onDone()
    }

    SearchBarSurface(modifier = modifier, focused = true) {
        BasicTextField(
            state = fieldState,
            lineLimits = TextFieldLineLimits.SingleLine,
            textStyle = MaterialTheme.typography.titleMedium.copy(color = TvOnSurface),
            cursorBrush = SolidColor(TvOnSurface),
            // «Поиск» вместо перевода строки: выдача под клавиатурой уже готова — запрос ушёл
            // в модель с первой буквы, и подтверждать нечего, кроме конца ввода.
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            onKeyboardAction = {
                keyboard?.hide()
                onDone()
            },
            modifier = Modifier
                .weight(1f)
                .focusRequester(fieldFocus)
                .onFocusChanged {
                    if (it.isFocused) hadFocus = true else if (hadFocus) onDone()
                },
        )
    }
}

/** Общая поверхность строки поиска: подложка, иконка и рамка фокуса — одна на оба состояния. */
@Composable
private fun SearchBarSurface(
    modifier: Modifier = Modifier,
    focused: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .clip(TvMetrics.PanelShape)
            .background(TvSurfaceContainer)
            .border(
                width = if (focused) TvMetrics.FocusBorderWidth else 0.dp,
                color = if (focused) TvOnSurface else Color.Transparent,
                shape = TvMetrics.PanelShape,
            )
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        content = {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = TvOnSurfaceDim,
                modifier = Modifier.size(20.dp),
            )
            content()
        },
    )
}

/** Кнопка голосового ввода рядом со строкой: на пульте это самый быстрый способ искать. */
@Composable
private fun VoiceSearchButton(onVoice: () -> Unit) {
    TvFocusCard(
        onClick = onVoice,
        shape = TvMetrics.PanelShape,
        modifier = Modifier.size(56.dp),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(TvMetrics.PanelShape)
                .background(TvSurfaceContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Mic,
                contentDescription = "Голосовой поиск",
                tint = TvOnSurface,
                modifier = Modifier.size(24.dp),
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

private const val TEN = 10
private const val HUNDRED = 100
private val TEENS = 11..14
private val FEW = 2..4
