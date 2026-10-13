package com.filmax.feature.search.common

import com.filmax.core.domain.catalog.CatalogFilters
import com.filmax.core.domain.catalog.CatalogRepository
import com.filmax.core.domain.catalog.SortOption
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemPage
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.common.getOrNull
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/** Что запрашивает витрина: типы (у «Все» и «Аниме» их несколько), жанр, фильтры и сортировка. */
internal data class CatalogQuery(
    val types: List<ItemType>,
    val genreId: Int?,
    val filters: CatalogFilters,
    val sort: SortOption,
)

/** Чем кончилась загрузка страницы витрины. */
internal sealed interface CatalogPage {
    data class Loaded(val items: List<Item>) : CatalogPage
    data class Failed(val message: String?) : CatalogPage

    /** Эта выдача уже в сетке — грузить нечего: закрытый поиск возвращает витрину как была. */
    data object Unchanged : CatalogPage

    /**
     * Пока страница шла, зритель выбрал другую выдачу. Страница никому не нужна: положи её модель
     * в сетку — под чипом «Сериалы» оказались бы фильмы.
     */
    data object Outdated : CatalogPage
}

/**
 * Постраничная загрузка витрины по нескольким типам сразу. `api/v1/items` отдаёт один тип за
 * запрос, а чип «Все» — это фильмы, сериалы и документальное вместе: каждая страница витрины —
 * по странице каждого типа, у которого они ещё не кончились, вперемешку.
 *
 * Пейджер помнит две выдачи: ту, что просили последней ([first]), и ту, чья первая страница
 * загружена ([loaded]). Ответ для любой другой — [CatalogPage.Outdated]: быстрые переключения
 * чипов запускают загрузки параллельно, и выиграть должна последняя, а не самая быстрая.
 */
internal class CatalogPager(private val catalog: CatalogRepository) {

    /** Выдача, чья первая страница загружена; null — ещё ничего или первая страница упала. */
    var loaded: CatalogQuery? = null
        private set

    /** Выдача, которую просили последней. */
    private var wanted: CatalogQuery? = null

    private var page = 0
    private var exhaustedTypes = setOf<ItemType>()

    /** Оценка размера выдачи по первым страницам типов; точная — у типов из одной страницы. */
    private var estimatedTotal = 0

    /** Все типы выдачи исчерпаны — дальше грузить нечего. */
    val endReached: Boolean
        get() = loaded?.types.orEmpty().all { it in exhaustedTypes }

    /** Первая страница выдачи [query]; прежние страницы забываются. Та же выдача — [CatalogPage.Unchanged]. */
    suspend fun first(query: CatalogQuery): CatalogPage {
        wanted = query
        if (query == loaded) return CatalogPage.Unchanged
        val fetched = fetch(query, query.types, number = 1)
        return when {
            query != wanted -> CatalogPage.Outdated
            fetched.allFailed() -> {
                loaded = null
                CatalogPage.Failed(fetched.errorMessage)
            }
            else -> {
                loaded = query
                page = 1
                exhaustedTypes = fetched.exhaustedTypes()
                estimatedTotal = fetched.pages.sumOf { (_, itemPage) -> itemPage.estimatedSize() }
                CatalogPage.Loaded(fetched.items())
            }
        }
    }

    /** Следующая страница загруженной выдачи; без [first] или после конца — пусто. */
    suspend fun next(): CatalogPage {
        val query = loaded
        val types = query?.types.orEmpty().filterNot { it in exhaustedTypes }
        if (query == null || types.isEmpty()) return CatalogPage.Loaded(emptyList())
        val fetched = fetch(query, types, number = page + 1)
        return when {
            query != wanted || query != loaded -> CatalogPage.Outdated
            fetched.allFailed() -> CatalogPage.Failed(fetched.errorMessage)
            else -> {
                page += 1
                exhaustedTypes = exhaustedTypes + fetched.exhaustedTypes()
                CatalogPage.Loaded(fetched.items())
            }
        }
    }

    /**
     * Сколько тайтлов в выдаче, если в сетке уже [shown]. Точно — когда выдача догружена до
     * конца; иначе оценка по первым страницам (последняя страница типа бывает неполной).
     */
    fun count(shown: Int): CatalogCount =
        if (endReached) CatalogCount(shown, exact = true) else CatalogCount(maxOf(estimatedTotal, shown), exact = false)

    /** Страница [number] для [types] параллельно — каждый тип отдельным запросом. */
    private suspend fun fetch(query: CatalogQuery, types: List<ItemType>, number: Int): FetchedPages =
        coroutineScope {
            types.map { type ->
                async { type to catalog.getItems(type, query.genreId, query.filters, query.sort, number) }
            }.awaitAll()
        }.let(::FetchedPages)
}

/**
 * Ответы типов на одну страницу. Ошибка — только когда не ответил НИ один тип: частичная выдача
 * лучше пустой сетки.
 */
private class FetchedPages(private val results: List<Pair<ItemType, RequestResult<ItemPage>>>) {
    val pages: List<Pair<ItemType, ItemPage>> =
        results.mapNotNull { (type, result) -> result.getOrNull()?.let { type to it } }

    val errorMessage: String?
        get() = results.firstNotNullOfOrNull { (_, result) -> (result as? RequestResult.Error)?.message }

    fun allFailed(): Boolean = pages.isEmpty()

    /** Типы, у которых эта страница последняя. */
    fun exhaustedTypes(): Set<ItemType> = pages
        .filter { (_, itemPage) -> itemPage.items.isEmpty() || !itemPage.pagination.hasNextPage }
        .mapTo(HashSet()) { (type, _) -> type }

    fun items(): List<Item> = interleave(pages.map { (_, itemPage) -> itemPage.items })
}

/**
 * Сколько тайтлов у типа по его первой странице. Одна страница — ровно столько, сколько на ней;
 * больше — по числу страниц: kino.pub отдаёт в `total` страницы, а не тайтлы.
 */
private fun ItemPage.estimatedSize(): Int =
    if (pagination.hasNextPage) pagination.total * pagination.perPage else items.size

/**
 * Склейка выдачи нескольких типов по кругу (для чипа «Все»). Простой `flatten()` дал бы
 * 20 фильмов, потом 20 сериалов — до документального зритель не долистал бы никогда.
 */
private fun interleave(lists: List<List<Item>>): List<Item> {
    if (lists.size == 1) return lists.first()
    val depth = lists.maxOf { it.size }
    return (0 until depth).flatMap { index -> lists.mapNotNull { it.getOrNull(index) } }
}
