package com.filmax.feature.search.common

import com.filmax.core.domain.catalog.CatalogFilters
import com.filmax.core.domain.catalog.CatalogSection
import com.filmax.core.domain.catalog.CatalogSort
import com.filmax.core.domain.catalog.SortOption
import com.filmax.core.domain.catalog.model.Country
import com.filmax.core.domain.catalog.model.Genre
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.presentation.labels.sectionLabel

/** С этой длины запроса имеет смысл идти в поиск — короче отдаётся мусор релевантности. */
const val MIN_QUERY_LENGTH = 2

private const val RECENT_LIMIT = 8
private const val DEFAULT_TITLE = "Каталог"

/**
 * Состояние каталога и поиска — одно на телефонный Каталог, телефонный Поиск и ТВ-Каталог. Сырые
 * поля правит модель; поля сетки ([searching], [visibleItems], [bestMatch], [otherResults])
 * пересчитываются её же методами `with*`, чтобы экраны ничего не считали сами.
 */
data class SearchState(
    val query: String = "",
    val filter: ItemType? = null,
    /** Сортировка выдачи каталога: поле + направление. VIEWS = «Просмотры» — дефолт витрины. */
    val sort: SortOption = SortOption(CatalogSort.VIEWS),
    /** Диапазонные фильтры каталога (год, рейтинги, страна, 4K, завершённость). */
    val filters: CatalogFilters = CatalogFilters(),
    /** Реальные жанры из API. Пусты, пока экран не запросил [SearchEvent.LoadCatalog]. */
    val genres: List<Genre> = emptyList(),
    val selectedGenreId: Int? = null,
    /** Страны для фильтра. Пусты, пока экран не запросил [SearchEvent.LoadCatalog]. */
    val countries: List<Country> = emptyList(),
    /** Результаты поиска по [query]. Пусты, пока запрос короче [MIN_QUERY_LENGTH]. */
    val results: List<Item> = emptyList(),
    /**
     * Выдача каталога по фильтрам без запроса. Живёт отдельно от [results], потому что на
     * телефоне пустой запрос значит «показать подсказки», а не «показать витрину».
     */
    val catalogItems: List<Item> = emptyList(),
    /**
     * Включается экраном, которому витрина нужна (Каталог, ТВ). Без этого флага телефонный
     * поиск на каждом открытии тянул бы выдачу, которую никогда не рисует.
     */
    val catalogEnabled: Boolean = false,
    /** Идёт догрузка следующей страницы витрины (хвостовые скелетоны сетки). */
    val catalogLoadingMore: Boolean = false,
    /** Все типы витрины исчерпаны — больше не грузим. */
    val catalogEndReached: Boolean = false,
    /**
     * Сколько тайтлов в витрине по текущим фильтрам. null — витрина ещё не загружена. Точное
     * число известно, только когда выдача догружена до конца: kino.pub отдаёт число страниц, а
     * не тайтлов, и до конца это оценка.
     */
    val catalogCount: CatalogCount? = null,
    val recentQueries: List<String> = emptyList(),
    val trendingQueries: List<String> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    /**
     * Раздел с Главной («Фильмы», «4K и HDR»), поверх которого открыт экран. С разделом экран —
     * страница поверх вкладки: заголовок — название раздела, есть «назад».
     */
    val section: CatalogSection? = null,
    val title: String = DEFAULT_TITLE,
    /** ТВ: открыт поиск — экранная клавиатура слева, результаты справа вместо сетки. */
    val searchOpen: Boolean = false,
    /** ТВ: раскладка экранной клавиатуры. */
    val keyboardLayout: KeyboardLayout = KeyboardLayout.RU,
    /** Открыта шторка (телефон) или боковая панель (ТВ) каталога. */
    val sheetOpen: Boolean = false,
    /**
     * Какая шторка или панель. Закрытие её не сбрасывает: панель ТВ доигрывает уход с прежним
     * содержимым, а фокус возвращается на чип, который её открыл.
     */
    val sheet: CatalogSheet = CatalogSheet.SORT,
    /**
     * Фильтры, которые правят в шторке. Уходят в [filters] только по «Применить» — иначе выдача
     * перезапрашивалась бы на каждый чип.
     */
    val filtersDraft: CatalogFilters = CatalogFilters(),
    /** Запрос достаточно длинный — сетка показывает поиск, а не витрину. */
    val searching: Boolean = query.length >= MIN_QUERY_LENGTH,
    /** Что показывает сетка каталога: поиск — с [MIN_QUERY_LENGTH] символов, иначе витрина. */
    val visibleItems: List<Item> = if (searching) results else catalogItems,
    /** Лучшее совпадение поиска — первый результат крупной карточкой; без запроса его нет. */
    val bestMatch: Item? = results.firstOrNull()?.takeIf { searching },
    /** Остальные результаты поиска — под лучшим совпадением. */
    val otherResults: List<Item> = if (bestMatch == null) emptyList() else results.drop(1),
) {
    /**
     * Выборка раздела с Главной поверх витрины по умолчанию. Раздел задаёт стартовую выборку и
     * заголовок, дальше зритель меняет её чипами, как в обычном Каталоге.
     */
    fun withSection(section: CatalogSection?): SearchState {
        val titled = copy(section = section, title = section?.let(::sectionLabel) ?: DEFAULT_TITLE)
        return when (section) {
            null -> this
            CatalogSection.MOVIES -> titled.copy(filter = ItemType.MOVIE)
            CatalogSection.SERIES -> titled.copy(filter = ItemType.SERIES)
            CatalogSection.ANIME -> titled.copy(filter = ItemType.ANIME)
            CatalogSection.DOCUMENTARY -> titled.copy(filter = ItemType.DOCUMENTARY)
            CatalogSection.TV_SHOWS -> titled.copy(filter = ItemType.TV_SHOW)
            CatalogSection.CONCERTS -> titled.copy(filter = ItemType.CONCERT)
            CatalogSection.UHD -> titled.copy(filters = filters.copy(only4k = true))
            CatalogSection.FRESH -> titled.copy(sort = SortOption(CatalogSort.CREATED))
        }
    }

    /** Новый запрос: ошибка прошлого поиска снята; стёртый запрос — выдача пуста и ждать нечего. */
    fun withQuery(query: String): SearchState {
        val cleared = query.isBlank()
        return copy(
            query = query,
            error = null,
            results = if (cleared) emptyList() else results,
            loading = if (cleared) false else loading,
        ).rendered()
    }

    fun withResults(results: List<Item>): SearchState = copy(loading = false, results = results).rendered()

    /** Первая страница витрины — прежняя выдача заменяется целиком. */
    fun withCatalog(items: List<Item>, endReached: Boolean, count: CatalogCount): SearchState = copy(
        loading = false,
        catalogItems = items,
        catalogEndReached = endReached,
        catalogCount = count,
        error = null,
    ).rendered()

    /** Догруженная страница уже слита с показанной — сюда приходит выдача целиком. */
    fun withMorePage(items: List<Item>, endReached: Boolean, count: CatalogCount): SearchState = copy(
        catalogLoadingMore = false,
        catalogItems = items,
        catalogEndReached = endReached,
        catalogCount = count,
    ).rendered()

    fun withCatalogFailed(message: String?): SearchState =
        copy(loading = false, catalogItems = emptyList(), catalogCount = null, error = message).rendered()

    /**
     * Выборка после события. Сортировка и жанр выбираются в шторке или панели — выбор её закрывает;
     * «Сбросить» в шторке фильтров телефона тоже закрывает её, применив пустые фильтры.
     */
    fun withSelection(event: SearchEvent.Selection): SearchState = when (event) {
        is SearchEvent.FilterChange -> copy(filter = event.filter)
        is SearchEvent.SortChange -> copy(sort = event.sort, sheetOpen = false)
        is SearchEvent.GenreChange -> copy(selectedGenreId = event.genreId, sheetOpen = false)
        is SearchEvent.ApplyFilters -> copy(filters = event.filters)
        SearchEvent.ResetFilters -> copy(filters = CatalogFilters(), sheetOpen = false)
    }

    /**
     * Шторки и панели. Фильтры открываются с черновиком, равным применённым: закрытая без
     * «Применить» шторка не оставляет следов, а «Применить» переносит черновик в выдачу.
     */
    fun withSheet(event: SearchEvent.Sheet): SearchState = when (event) {
        is SearchEvent.OpenSheet -> copy(sheetOpen = true, sheet = event.sheet, filtersDraft = filters)
        SearchEvent.CloseSheet -> copy(sheetOpen = false)
        is SearchEvent.EditFilters -> copy(filtersDraft = event.filters)
        SearchEvent.ResetDraftFilters -> copy(filtersDraft = CatalogFilters())
        SearchEvent.ApplyDraftFilters -> copy(filters = filtersDraft, sheetOpen = false)
    }

    /** Запрос в начало недавних — если он годится для поиска. */
    fun withRecent(query: String): SearchState {
        val trimmed = query.trim()
        if (trimmed.length < MIN_QUERY_LENGTH) return this
        return copy(recentQueries = (listOf(trimmed) + recentQueries).distinct().take(RECENT_LIMIT))
    }

    /** Поля сетки — из запроса и выдач. Зовётся после каждой их правки: `copy` их не пересчитывает. */
    private fun rendered(): SearchState {
        val searching = query.length >= MIN_QUERY_LENGTH
        val match = results.firstOrNull()?.takeIf { searching }
        return copy(
            searching = searching,
            visibleItems = if (searching) results else catalogItems,
            bestMatch = match,
            otherResults = if (match == null) emptyList() else results.drop(1),
        )
    }
}

/** Число тайтлов витрины: [exact] = false — оценка «около» по числу страниц. */
data class CatalogCount(val value: Int, val exact: Boolean)

/** Раскладка экранной клавиатуры поиска на ТВ. */
enum class KeyboardLayout { RU, EN, DIGITS }

/** Шторка каталога на телефоне и боковая панель на ТВ. GENRE — только телефон: на ТВ жанры — чипы в шапке. */
enum class CatalogSheet { SORT, GENRE, FILTERS }

sealed interface SearchEvent {
    data class QueryChange(val query: String) : SearchEvent

    /** Смена выборки: тип, сортировка, жанр, фильтры — каждая перезапрашивает выдачу. */
    sealed interface Selection : SearchEvent

    data class FilterChange(val filter: ItemType?) : Selection

    /** Смена сортировки целиком (поле и/или направление) — UI собирает [SortOption] сам. */
    data class SortChange(val sort: SortOption) : Selection

    /** null — снять фильтр по жанру. */
    data class GenreChange(val genreId: Int?) : Selection

    /** Применить диапазонные фильтры как есть — «✕» на чипе применённого фильтра. */
    data class ApplyFilters(val filters: CatalogFilters) : Selection

    /** Сбросить диапазонные фильтры в дефолт (кнопка «Сбросить»). Сортировку не трогает. */
    data object ResetFilters : Selection

    /** Тап по подсказке (тренды/недавние): подставить запрос и сразу искать. */
    data class SubmitQuery(val query: String) : SearchEvent
    data object ClearRecent : SearchEvent

    /** Экран-витрина заявляет о себе: подтянуть жанры, страны и выдачу по текущим фильтрам. */
    data object LoadCatalog : SearchEvent

    /** Догрузить следующую страницу витрины (триггерится при подходе скролла к концу сетки). */
    data object LoadMoreCatalog : SearchEvent

    /** Витрина не загрузилась — «Повторить». */
    data object RetryCatalog : SearchEvent

    /** Карточка витрины — в детали. */
    data class OpenItem(val itemId: Int) : SearchEvent

    /** Результат поиска — в детали; запрос запоминается в недавних — а не каждая набранная подстрока. */
    data class OpenResult(val itemId: Int) : SearchEvent

    /** «Назад» из раздела каталога, открытого с Главной (телефон). */
    data object Back : SearchEvent

    /** ТВ: режим поиска с экранной клавиатурой. */
    sealed interface Keyboard : SearchEvent

    /**
     * ТВ: открыть поиск; [query] — сразу искать готовый запрос (голос, недавний) и, когда найдётся,
     * перевести фокус на лучшее совпадение ([SearchSideEffect.FocusBestMatch]).
     */
    data class OpenSearch(val query: String? = null) : Keyboard

    /** ТВ: закрыть поиск и вернуться к витрине — запрос сбрасывается, витрина не перезагружается. */
    data object CloseSearch : Keyboard

    /** ТВ: клавиша экранной клавиатуры — буква, цифра или пробел. */
    data class TypeKey(val key: String) : Keyboard

    /** ТВ: «Стереть» — последний символ запроса. */
    data object DeleteLast : Keyboard

    data class SwitchLayout(val layout: KeyboardLayout) : Keyboard

    /** Шторки каталога (телефон) и боковые панели (ТВ). */
    sealed interface Sheet : SearchEvent

    /** Открыть шторку или панель; у фильтров черновик начинается с применённых. */
    data class OpenSheet(val sheet: CatalogSheet) : Sheet
    data object CloseSheet : Sheet

    /** Правка фильтров в шторке — только черновик, выдача не трогается. */
    data class EditFilters(val filters: CatalogFilters) : Sheet

    /** ТВ: «Сбросить» в панели фильтров — черновик к дефолту; выдачу меняет только «Применить». */
    data object ResetDraftFilters : Sheet

    /** «Применить» — черновик становится фильтрами выдачи, шторка закрывается. */
    data object ApplyDraftFilters : Sheet
}

/**
 * ТВ: куда переезжает фокус при смене режима. Переводы идут одним каналом с состоянием, поэтому
 * «на клавиатуру» и следом «на найденное» приходят в том порядке, в каком решила модель.
 */
sealed interface SearchSideEffect {
    /** Поиск открыт — набирать. */
    data object FocusKeyboard : SearchSideEffect

    /** Поиск закрыт — фокус обратно на поле витрины. */
    data object FocusSearchField : SearchSideEffect

    /** Готовый запрос нашёл лучшее совпадение — фокус на него, а не на клавиатуру. */
    data object FocusBestMatch : SearchSideEffect
}
