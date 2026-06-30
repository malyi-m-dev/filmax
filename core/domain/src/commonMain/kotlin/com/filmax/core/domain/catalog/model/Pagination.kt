package com.filmax.core.domain.catalog.model

data class Pagination(
    /** Всего страниц (kino.pub отдаёт в `pagination.total` именно число страниц). */
    val total: Int,
    /** Текущая страница (1-based). */
    val current: Int,
    /** Элементов на странице (`pagination.perpage`). */
    val perPage: Int,
) {
    val hasNextPage: Boolean get() = current < total
}

data class ItemPage(
    val items: List<Item>,
    val pagination: Pagination,
)
