package com.filmax.core.domain.catalog

import com.filmax.core.domain.catalog.model.Collection
import com.filmax.core.domain.catalog.model.CollectionPage
import com.filmax.core.domain.catalog.model.Country
import com.filmax.core.domain.catalog.model.Genre
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemPage
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.common.RequestResult

// Каталог kino.pub большой: детали, похожее, жанры, страны, подборки, готовые выборки,
// фильтруемая выдача. Это один связный контракт, дробить его на несколько интерфейсов незачем.
@Suppress("TooManyFunctions")
interface CatalogRepository {

    suspend fun getItems(
        type: ItemType,
        sort: CatalogSort = CatalogSort.UPDATED,
        page: Int = 1,
    ): RequestResult<ItemPage>

    suspend fun getItemsByGenre(
        type: ItemType,
        genreId: Int,
        sort: CatalogSort = CatalogSort.UPDATED,
        page: Int = 1,
    ): RequestResult<ItemPage>

    /**
     * Каталог с полным набором фильтров и сортировкой с направлением. Отдельная перегрузка, а не
     * замена [getItems]/[getItemsByGenre]: короткие сигнатуры дёргают главный экран и iOS-слой,
     * которым фильтры не нужны, — ломать их ради витрины каталога незачем.
     *
     * [genreId] = null — без фильтра по жанру (в отличие от [getItemsByGenre], где жанр обязателен).
     */
    suspend fun getItems(
        type: ItemType,
        genreId: Int?,
        filters: CatalogFilters,
        sort: SortOption,
        page: Int = 1,
    ): RequestResult<ItemPage>

    /** Готовая выборка kino.pub ([CatalogShortcut]) одного типа — постранично, как и выдача каталога. */
    suspend fun getShortcutItems(shortcut: CatalogShortcut, type: ItemType, page: Int = 1): RequestResult<ItemPage>

    suspend fun getItemDetails(id: Int): RequestResult<Item>

    suspend fun getSimilarItems(id: Int): RequestResult<List<Item>>

    suspend fun getGenres(): RequestResult<List<Genre>>

    /** Страны производства для фильтра каталога (api/v1/countries). */
    suspend fun getCountries(): RequestResult<List<Country>>

    suspend fun getCollections(page: Int = 1): RequestResult<List<Collection>>

    suspend fun getCollectionItems(collectionId: Int, page: Int = 1): RequestResult<CollectionPage>
}

/**
 * Готовая выборка kino.pub — `api/v1/items/<path>`. Что в неё попадает и в каком порядке, решает
 * сервер: жанра и сортировки она не принимает, только тип. Выборки `new` в API нет (404 «Страница
 * не найдена») — свежие поступления отдаёт [FRESH].
 */
enum class CatalogShortcut(val path: String) {
    /** То, что смотрят сейчас. */
    HOT("hot"),

    /** Популярное. */
    POPULAR("popular"),

    /** Свежие поступления. */
    FRESH("fresh"),
}

/**
 * Поле сортировки каталога. [apiValue] — имя поля kino.pub для параметра `sort`; направление
 * (по возрастанию/убыванию) задаётся отдельно в [SortOption], а не удвоением значений enum.
 */
enum class CatalogSort(val apiValue: String) {
    UPDATED("updated"),
    CREATED("created"),
    RATING("rating"),
    VIEWS("views"),
    YEAR("year"),
    KINOPOISK_RATING("kinopoisk_rating"),
    IMDB_RATING("imdb_rating"),
}

/**
 * Сортировка с направлением. kino.pub: `sort=-field` — по УБЫВАНИЮ, `sort=field` — по
 * возрастанию (проверено живым API: `-rating` отдаёт топ, `rating` — худшее). Раньше знак
 * трактовался наоборот, и «лучшее сверху» на деле показывало хвост каталога.
 * Дефолт — убывание: и «свежие», и «высокий рейтинг» читаются сверху вниз.
 */
data class SortOption(
    val field: CatalogSort,
    val ascending: Boolean = false,
) {
    // this.field, а не field: внутри геттера `field` — это ключевое слово backing-field, а не
    // конструкторный параметр. Без `this` компилятор считает apiValue свойством с полем и падает.
    val apiValue: String get() = if (ascending) this.field.apiValue else "-${this.field.apiValue}"
}
