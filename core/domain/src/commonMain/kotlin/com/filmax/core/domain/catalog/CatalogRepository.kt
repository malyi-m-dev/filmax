package com.filmax.core.domain.catalog

import com.filmax.core.domain.catalog.model.Collection
import com.filmax.core.domain.catalog.model.CollectionPage
import com.filmax.core.domain.catalog.model.Country
import com.filmax.core.domain.catalog.model.Genre
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemPage
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.common.RequestResult

// Каталог большой: детали, похожее, жанры, страны, подборки, готовые выборки, фильтруемая выдача.
// Это один связный контракт, дробить его на несколько интерфейсов незачем.
@Suppress("TooManyFunctions")
interface CatalogRepository {

    suspend fun getItems(
        type: ItemType,
        sort: CatalogSort = CatalogSort.UPDATED,
        page: Int = 1,
    ): RequestResult<ItemPage>

    /**
     * Каталог с полным набором фильтров и сортировкой с направлением. Отдельная перегрузка, а не
     * замена короткой [getItems]: её дёргают Главная и iOS-слой, которым фильтры не нужны.
     *
     * [genre] = null — без фильтра по жанру.
     */
    suspend fun getItems(
        type: ItemType,
        genre: GenreFilter?,
        filters: CatalogFilters,
        sort: SortOption,
        page: Int = 1,
    ): RequestResult<ItemPage>

    /** Готовая выборка источника ([CatalogShortcut]) одного типа — постранично, как и выдача каталога. */
    suspend fun getShortcutItems(shortcut: CatalogShortcut, type: ItemType, page: Int = 1): RequestResult<ItemPage>

    suspend fun getItemDetails(id: Int): RequestResult<Item>

    suspend fun getSimilarItems(id: Int): RequestResult<List<Item>>

    suspend fun getGenres(): RequestResult<List<Genre>>

    /** Страны производства для фильтра каталога; пусто — источник по стране не фильтрует. */
    suspend fun getCountries(): RequestResult<List<Country>>

    suspend fun getCollections(page: Int = 1): RequestResult<List<Collection>>

    suspend fun getCollectionItems(collectionId: Int, page: Int = 1): RequestResult<CollectionPage>
}

/**
 * Готовая выборка источника. Что в неё попадает и в каком порядке, решает источник: жанра и
 * сортировки она не принимает, только тип.
 */
enum class CatalogShortcut {
    /** То, что смотрят сейчас: у домашнего сервера — свежее с широким фоном, для карусели. */
    HOT,

    /** Популярное. */
    POPULAR,

    /** Свежие поступления. */
    FRESH,
}

/** Поле сортировки каталога. Какие поля есть у источника — [com.filmax.core.domain.source.SourceCapabilities.sorts]. */
enum class CatalogSort {
    UPDATED,
    CREATED,
    RATING,
    VIEWS,
    YEAR,
    KINOPOISK_RATING,
    IMDB_RATING,
}

/** Сортировка с направлением. По умолчанию — по убыванию: и «свежие», и «высокий рейтинг» сверху. */
data class SortOption(
    val field: CatalogSort,
    val ascending: Boolean = false,
)

/** Жанр выдачи каталога. */
sealed interface GenreFilter {

    /** Жанр из справочника источника ([CatalogRepository.getGenres]). */
    data class Listed(val id: Int) : GenreFilter

    /** Опорный жанр: на него ссылаются экраны сами, а каждый источник находит его по-своему. */
    data class Reference(val genre: ReferenceGenre) : GenreFilter
}

/**
 * Жанры, на которые код ссылается сам: тематические ряды Главной и раздел «Аниме». У kino.pub
 * это id справочника, у домашнего сервера — имена жанров медиатеки.
 */
enum class ReferenceGenre { COMEDY, SCI_FI, CARTOON, ANIME, DORAMA }
