package com.filmax.feature.search.common

import com.filmax.core.domain.catalog.CatalogCount
import com.filmax.core.domain.catalog.CatalogFilters
import com.filmax.core.domain.catalog.CatalogSection
import com.filmax.core.domain.catalog.CatalogSort
import com.filmax.core.domain.catalog.SortOption
import com.filmax.core.domain.catalog.model.Country
import com.filmax.core.domain.catalog.model.Genre
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.source.ContentSource
import com.filmax.core.domain.source.SourceCapabilities
import com.filmax.core.domain.source.capabilities
import com.filmax.core.presentation.cards.TitleCard
import com.filmax.core.presentation.cards.toTitleCard

/** С этой длины запроса имеет смысл идти в поиск — короче отдаётся мусор релевантности. */
const val MIN_QUERY_LENGTH = 2

data class SearchState(
    /** Раздел с Главной («Фильмы», «4K и HDR»), которым открыт Каталог; null — вкладка Каталога. */
    val section: CatalogSection? = null,
    val query: String = "",
    val filter: ItemType? = null,
    /** Сортировка выдачи каталога: поле + направление. VIEWS = «Просмотры» — дефолт витрины. */
    val sort: SortOption = SortOption(CatalogSort.VIEWS),
    /** Что умеет источник: какие оценки, сортировки и фильтры экран предлагает. */
    val capabilities: SourceCapabilities = ContentSource.KINOPUB.capabilities,
    /** Диапазонные фильтры каталога (год, рейтинги, страна, 4K, завершённость). */
    val filters: CatalogFilters = CatalogFilters(),
    /** Реальные жанры из API. Пусты, пока экран не запросил [SearchEvent.LoadCatalog]. */
    val genres: List<Genre> = emptyList(),
    val selectedGenreId: Int? = null,
    /** Страны для фильтра. Пусты, пока экран не запросил [SearchEvent.LoadCatalog]. */
    val countries: List<Country> = emptyList(),
    /**
     * Результаты поиска по [query] — тайтлы целиком: их фильтрует и досортировывает модель. Экран
     * рисует [resultCards]. Пусты, пока запрос короче [MIN_QUERY_LENGTH].
     */
    internal val results: List<Item> = emptyList(),
    /**
     * Выдача каталога по фильтрам без запроса — тайтлы целиком: следующую страницу модель вливает
     * в них с пересортировкой. Экран рисует [catalogCards]. Живёт отдельно от [results], потому что
     * на телефоне пустой запрос значит «показать подсказки», а не «показать витрину».
     */
    internal val catalogItems: List<Item> = emptyList(),
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
    /** Открыта шторка (телефон) или боковая панель (ТВ) каталога. */
    val sheetOpen: Boolean = false,
    /**
     * Какая панель — сортировка, жанр или фильтры. Закрытие её не сбрасывает: панель доигрывает
     * уход с прежним содержимым, а фокус ТВ возвращается на чип, который её открыл.
     */
    val sheet: CatalogSheet = CatalogSheet.SORT,
    /**
     * Фильтры, которые правят в панели. Уходят в [filters] только по «Применить» — иначе выдача
     * перезапрашивалась бы на каждый сдвиг ползунка и чип.
     */
    val filtersDraft: CatalogFilters = CatalogFilters(),
) {
    /**
     * Карточки найденного с готовыми подписями. Лениво и один раз на снимок: снимок меняется на
     * каждую букву запроса, а карточки нужны только тому, кто их рисует.
     */
    val resultCards: List<TitleCard> by lazy { results.map { it.toTitleCard() } }

    /** Карточки витрины — так же, лениво и один раз на снимок. */
    val catalogCards: List<TitleCard> by lazy { catalogItems.map { it.toTitleCard() } }

    /** Что показывает сетка каталога: поиск — с [MIN_QUERY_LENGTH] символов, иначе витрина. */
    val visibleCards: List<TitleCard>
        get() = if (query.length >= MIN_QUERY_LENGTH) resultCards else catalogCards

    /** Лучшее совпадение поиска — первый результат крупной карточкой; без запроса его нет. */
    val bestMatch: TitleCard?
        get() = resultCards.firstOrNull()?.takeIf { query.length >= MIN_QUERY_LENGTH }

    /** Остальные результаты поиска — под лучшим совпадением. */
    val otherResults: List<TitleCard>
        get() = if (bestMatch == null) emptyList() else resultCards.drop(1)
}

/** Раскладка экранной клавиатуры поиска на ТВ. */
enum class KeyboardLayout { RU, EN, DIGITS }

/** Панель каталога: сортировка, жанр (только телефон — на ТВ жанры рядом чипов) и фильтры. */
enum class CatalogSheet { SORT, GENRE, FILTERS }

sealed interface SearchEvent {
    data class QueryChange(val query: String) : SearchEvent

    /** Смена выборки: тип, сортировка, жанр, фильтры — каждая перезапрашивает выдачу. */
    sealed interface Selection : SearchEvent

    data class FilterChange(val filter: ItemType?) : Selection

    /**
     * Смена сортировки целиком (поле и/или направление) — UI собирает [SortOption] сам. Панель не
     * закрывает: на телефоне шторка уезжает сама, на ТВ экран следом шлёт [CloseSheet].
     */
    data class SortChange(val sort: SortOption) : Selection

    /** null — снять фильтр по жанру. */
    data class GenreChange(val genreId: Int?) : Selection

    /** Применить диапазонные фильтры целиком: «✕» на чипе применённого фильтра. */
    data class ApplyFilters(val filters: CatalogFilters) : Selection

    /**
     * Сбросить диапазонные фильтры в дефолт: «Сбросить» в шторке телефона и на пустой выдаче.
     * Сортировку не трогает.
     */
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

    /**
     * Панели каталога. Выбор в панели её не закрывает — закрывает [CloseSheet]: шторка телефона
     * шлёт его, когда доехала вниз (закрой её модель сразу — шторка исчезла бы рывком), ТВ — следом
     * за выбором.
     */
    sealed interface Sheet : SearchEvent

    /** Открыть панель; у фильтров черновик начинается с применённых. */
    data class OpenSheet(val sheet: CatalogSheet) : Sheet
    data object CloseSheet : Sheet

    /** Правка фильтров в панели — только черновик, выдача не трогается. */
    data class EditFilters(val filters: CatalogFilters) : Sheet

    /** ТВ: «Сбросить» в панели фильтров — черновик к дефолту; выдачу меняет только «Применить». */
    data object ResetDraftFilters : Sheet

    /** «Применить» в панели фильтров — черновик становится фильтрами выдачи. */
    data object ApplyDraftFilters : Sheet
}

sealed interface SearchSideEffect {
    /** ТВ: готовый запрос нашёл лучшее совпадение — фокус на него, а не на клавиатуру. */
    data object FocusBestMatch : SearchSideEffect
}
