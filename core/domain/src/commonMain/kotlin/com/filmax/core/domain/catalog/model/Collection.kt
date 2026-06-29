package com.filmax.core.domain.catalog.model

data class Collection(
    val id: Int,
    val title: String,
    val description: String?,
    val posters: Posters?,
)

data class CollectionPage(
    val collection: Collection?,
    val items: List<Item>,
    val pagination: Pagination,
)
