package com.filmax.feature.search.common

import com.filmax.core.domain.catalog.CatalogFilters
import com.filmax.core.domain.catalog.CatalogSort
import com.filmax.core.domain.catalog.SortOption
import com.filmax.core.domain.catalog.model.Country
import com.filmax.core.domain.catalog.model.Genre
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemType

/** С этой длины запроса имеет смысл идти в поиск — короче отдаётся мусор релевантности. */
const val MIN_QUERY_LENGTH = 2

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
     * Включается экраном, которому витрина нужна (TV-Каталог). Без этого флага телефонный
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
    /** ТВ: открыт поиск — экранная клавиатура слева, результаты справа вместо сетки. */
    val searchOpen: Boolean = false,
    /** ТВ: раскладка экранной клавиатуры. */
    val keyboardLayout: KeyboardLayout = KeyboardLayout.RU,
    /** ТВ: открыта боковая панель каталога. */
    val sheetOpen: Boolean = false,
    /**
     * ТВ: какая панель — сортировка или фильтры. Закрытие её не сбрасывает: панель доигрывает
     * уход с прежним содержимым, а фокус возвращается на чип, который её открыл.
     */
    val sheet: CatalogSheet = CatalogSheet.SORT,
    /**
     * ТВ: фильтры, которые правят в панели. Уходят в [filters] только по «Применить» — иначе
     * выдача перезапрашивалась бы на каждый чип.
     */
    val filtersDraft: CatalogFilters = CatalogFilters(),
) {
    /** Что показывает сетка каталога: поиск — с [MIN_QUERY_LENGTH] символов, иначе витрина. */
    val visibleItems: List<Item>
        get() = if (query.length >= MIN_QUERY_LENGTH) results else catalogItems

    /** Лучшее совпадение поиска — первый результат крупной карточкой; без запроса его нет. */
    val bestMatch: Item?
        get() = results.firstOrNull()?.takeIf { query.length >= MIN_QUERY_LENGTH }

    /** Остальные результаты поиска — под лучшим совпадением. */
    val otherResults: List<Item>
        get() = if (bestMatch == null) emptyList() else results.drop(1)
}

/** Число тайтлов витрины: [exact] = false — оценка «около» по числу страниц. */
data class CatalogCount(val value: Int, val exact: Boolean)

/** Раскладка экранной клавиатуры поиска на ТВ. */
enum class KeyboardLayout { RU, EN, DIGITS }

/** Боковая панель каталога на ТВ. */
enum class CatalogSheet { SORT, FILTERS }

sealed interface SearchEvent {
    data class QueryChange(val query: String) : SearchEvent

    /** Смена выборки: тип, сортировка, жанр, фильтры — каждая перезапрашивает выдачу. */
    sealed interface Selection : SearchEvent

    data class FilterChange(val filter: ItemType?) : Selection

    /** Смена сортировки целиком (поле и/или направление) — UI собирает [SortOption] сам. */
    data class SortChange(val sort: SortOption) : Selection

    /** null — снять фильтр по жанру. */
    data class GenreChange(val genreId: Int?) : Selection

    /** Применить диапазонные фильтры (лист «Фильтры» на телефоне). */
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

    /** Открыт результат поиска: запрос запоминается в недавних — а не каждая набранная подстрока. */
    data object ResultOpened : SearchEvent

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

    /** ТВ: боковые панели каталога — сортировка и фильтры. */
    sealed interface Sheet : SearchEvent

    /** ТВ: открыть панель; у фильтров черновик начинается с применённых. */
    data class OpenSheet(val sheet: CatalogSheet) : Sheet
    data object CloseSheet : Sheet

    /** ТВ: правка фильтров в панели — только черновик, выдача не трогается. */
    data class EditFilters(val filters: CatalogFilters) : Sheet

    /** ТВ: «Сбросить» в панели фильтров — черновик к дефолту; выдачу меняет только «Применить». */
    data object ResetDraftFilters : Sheet

    /** ТВ: «Применить» в панели фильтров — черновик становится фильтрами выдачи. */
    data object ApplyDraftFilters : Sheet
}

sealed interface SearchSideEffect {
    /** ТВ: готовый запрос нашёл лучшее совпадение — фокус на него, а не на клавиатуру. */
    data object FocusBestMatch : SearchSideEffect
}
