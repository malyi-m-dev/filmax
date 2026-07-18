package com.filmax.data.catalog.remote

import com.filmax.data.catalog.remote.dto.CollectionItemsDto
import com.filmax.data.catalog.remote.dto.CollectionsResponseDto
import com.filmax.data.catalog.remote.dto.CountriesResponseDto
import com.filmax.data.catalog.remote.dto.GenresResponseDto
import com.filmax.data.catalog.remote.dto.ItemsResponseDto
import com.filmax.data.catalog.remote.dto.MovieInfoDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

/**
 * Параметры запроса витрины `api/v1/items`. Собраны в data-класс, потому что список аргументов
 * (тип, жанр, сортировка, страница, страна, качество, флаг завершённости, диапазоны) упёрся бы
 * в порог LongParameterList. [conditions] — уже готовые строки условий вида `year>=2020`.
 */
internal data class ItemsQuery(
    val type: String,
    val sort: String,
    val page: Int,
    val genreId: Int? = null,
    val countryId: Int? = null,
    val quality: Int? = null,
    val finished: Int? = null,
    val conditions: List<String> = emptyList(),
)

internal class CatalogApi(private val client: HttpClient) {

    suspend fun getItemDetails(id: Int): MovieInfoDto =
        client.get("api/v1/items/$id").body()

    suspend fun getItems(type: String, sort: String, page: Int): ItemsResponseDto =
        client.get("api/v1/items") {
            parameter("type", type)
            parameter("sort", sort)
            parameter("page", page)
        }.body()

    suspend fun getItemsByGenre(type: String, genreId: Int, sort: String, page: Int): ItemsResponseDto =
        client.get("api/v1/items") {
            parameter("type", type)
            parameter("genre", genreId)
            parameter("sort", sort)
            parameter("page", page)
        }.body()

    suspend fun getFilteredItems(query: ItemsQuery): ItemsResponseDto =
        client.get("api/v1/items") {
            parameter("type", query.type)
            query.genreId?.let { parameter("genre", it) }
            parameter("sort", query.sort)
            parameter("page", query.page)
            query.countryId?.let { parameter("country", it) }
            query.quality?.let { parameter("quality", it) }
            query.finished?.let { parameter("finished", it) }
            // conditions[] — повторяемый query-параметр. Ktor `parameter()` вызывает append(),
            // который НЕ схлопывает одинаковые ключи, поэтому в URL реально уходит
            // conditions[]=year>=2020&conditions[]=year<=2024 (скобки/операторы percent-энкодятся,
            // PHP на стороне kino.pub декодирует `%5B%5D` обратно в массив).
            query.conditions.forEach { condition -> parameter("conditions[]", condition) }
        }.body()

    suspend fun getItemsByShortcut(shortcut: String, type: String, page: Int): ItemsResponseDto =
        client.get("api/v1/items/$shortcut") {
            parameter("type", type)
            parameter("page", page)
        }.body()

    suspend fun getSimilarItems(id: Int): ItemsResponseDto =
        client.get("api/v1/items/similar") {
            parameter("id", id)
        }.body()

    suspend fun getGenres(): GenresResponseDto =
        client.get("api/v1/genres").body()

    suspend fun getCountries(): CountriesResponseDto =
        client.get("api/v1/countries").body()

    suspend fun getCollections(sort: String? = null, page: Int): CollectionsResponseDto =
        client.get("api/v1/collections") {
            sort?.let { parameter("sort", it) }
            parameter("page", page)
        }.body()

    suspend fun getCollectionItems(id: Int, page: Int): CollectionItemsDto =
        client.get("api/v1/collections/view") {
            parameter("id", id)
            parameter("page", page)
        }.body()
}
