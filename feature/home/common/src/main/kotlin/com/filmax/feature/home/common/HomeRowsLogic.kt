// Чистые правила ленты Главной: смена hero в карусели, превью подборок, тематические ряды. Вынесены
// из HomeScreenModel, чтобы модель только оркестрировала загрузку, а правила проверялись отдельно.
package com.filmax.feature.home.common

import com.filmax.core.domain.catalog.CatalogRepository
import com.filmax.core.domain.catalog.model.Collection
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.common.RequestResult

/** Карусель hero — к следующему тайтлу по кругу; один тайтл не листается. */
fun HomeState.withNextHero(): HomeState =
    if (heroes.size < 2) this else copy(heroIndex = (heroIndex + 1) % heroes.size)

/** Постеров веером на карточке подборки. */
private const val PREVIEW_POSTERS = 2

/**
 * Превью подборки по её первой странице: два постера и число тайтлов — только если подборка
 * умещается в одну страницу (kino.pub отдаёт число страниц, а не тайтлов). null — не загрузилось.
 */
suspend fun CatalogRepository.collectionPreview(collectionId: Int): CollectionPreview? {
    val page = (getCollectionItems(collectionId) as? RequestResult.Success)?.data ?: return null
    return CollectionPreview(
        posters = page.items.take(PREVIEW_POSTERS).map { it.posters.medium.ifBlank { it.posters.small } },
        count = page.items.size.takeUnless { page.pagination.hasNextPage },
    )
}

/** Ряд подборок с ещё одним превью; другие ряды не меняются. */
internal fun HomeRow.withPreview(collectionId: Int, preview: CollectionPreview): HomeRow =
    if (this is HomeRow.Collections) copy(previews = previews + (collectionId to preview)) else this

/**
 * Тематические ряды новой ленты в порядке [THEME_SOURCES]. Уже пришедший ряд остаётся как был:
 * повторная загрузка Главной не прячет его до ответа и не сбрасывает пролистанное. Остальные —
 * пустые, ждут первой страницы.
 */
internal fun HomeState.themeRows(): List<HomeRow> = THEME_SOURCES.keys.map { id ->
    rows.firstOrNull { it.id == id } ?: HomeRow.Titles(id, RowPaging())
}

/** Тематические ряды, которым нечего показать и которые никто не грузит: первый заход или сбой. */
internal fun HomeState.unloadedThemeRows(): List<HomeRow.Titles> = rows
    .filterIsInstance<HomeRow.Titles>()
    .filter { it.id in THEME_SOURCES && it.isEmpty && !it.paging.loadingMore }

/** Ряд тайтлов [id] с изменённой пагинацией; другие ряды не меняются. */
internal fun HomeRow.withTitlesPaging(id: HomeRowId, transform: (RowPaging<Item>) -> RowPaging<Item>): HomeRow =
    if (this is HomeRow.Titles && this.id == id) copy(paging = transform(paging)) else this

/** Ряд подборок с изменённой пагинацией; другие ряды не меняются. */
internal fun HomeRow.withCollectionsPaging(transform: (RowPaging<Collection>) -> RowPaging<Collection>): HomeRow =
    if (this is HomeRow.Collections) copy(paging = transform(paging)) else this
