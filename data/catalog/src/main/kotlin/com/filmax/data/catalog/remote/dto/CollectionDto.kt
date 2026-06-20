package com.filmax.data.catalog.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CollectionsResponseDto(
    val items: List<CollectionDto>,
    val pagination: PaginationDto? = null,
)

@Serializable
data class CollectionDto(
    val id: Int,
    val title: String,
    val description: String? = null,
    val posters: PostersDto? = null,
)

@Serializable
data class CollectionItemsDto(
    val collection: CollectionDto? = null,
    val items: List<ItemDto> = emptyList(),
    val pagination: PaginationDto? = null,
)

@Serializable
data class GenresResponseDto(
    val items: List<GenreDto>,
)
