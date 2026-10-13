package com.filmax.data.fake.store

import com.filmax.core.domain.catalog.model.Pagination

/** Размер страницы — как у kino.pub по умолчанию. */
internal const val PAGE_SIZE = 20

/** Нумерация страниц kino.pub — с единицы. */
private const val FIRST_PAGE = 1

/** Срез [page]-й страницы (с единицы); за последней страницей — пустой список, как у сервера. */
internal fun <T> List<T>.pageOf(page: Int, perPage: Int = PAGE_SIZE): List<T> {
    val current = page.coerceAtLeast(FIRST_PAGE)
    return drop((current - FIRST_PAGE) * perPage).take(perPage)
}

/**
 * Честная пагинация: `total` у kino.pub — число страниц, а не элементов. От неё зависит
 * `hasNextPage`, поэтому догрузка в каталоге, папках и подборках останавливается там же,
 * где остановилась бы на живом API.
 */
internal fun paginationOf(count: Int, page: Int, perPage: Int = PAGE_SIZE): Pagination =
    Pagination(
        total = (count + perPage - 1) / perPage,
        current = page.coerceAtLeast(FIRST_PAGE),
        perPage = perPage,
    )
