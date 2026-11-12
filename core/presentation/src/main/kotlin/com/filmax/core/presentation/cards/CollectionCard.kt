package com.filmax.core.presentation.cards

import com.filmax.core.domain.catalog.model.Collection
import com.filmax.core.presentation.format.filmsLabel
import com.filmax.core.presentation.format.posterUrl
import com.filmax.core.presentation.format.titlesLabel

/**
 * Карточка подборки: постеры веера и счётчик. Счётчик есть, только если он точный — kino.pub отдаёт
 * число страниц, а не тайтлов. Телефон считает «тайтлы», ТВ — «фильмы», как в своих макетах.
 */
data class CollectionCard(
    val id: Int,
    val title: String,
    /** Постеры первой страницы, а пока их нет — обложка подборки. */
    val posters: List<String>,
    /** «12 тайтлов»; null — число неизвестно. */
    val titlesCount: String?,
    /** «12 фильмов»; null — число неизвестно. */
    val filmsCount: String?,
)

/** Карточка из подборки и её превью ([posters] и точное число тайтлов [count], если уже пришли). */
fun Collection.toCollectionCard(posters: List<String>? = null, count: Int? = null): CollectionCard = CollectionCard(
    id = id,
    title = title,
    posters = posters?.takeIf { it.isNotEmpty() } ?: listOfNotNull(posterUrl()),
    titlesCount = count?.let(::titlesLabel),
    filmsCount = count?.let(::filmsLabel),
)
