package com.filmax.core.domain.catalog.model

data class Pagination(
    val total: Int,
    val current: Int,
    val perPage: Int,
) {
    val hasNextPage: Boolean get() = current * perPage < total
}

data class ItemPage(
    val items: List<Item>,
    val pagination: Pagination,
)
