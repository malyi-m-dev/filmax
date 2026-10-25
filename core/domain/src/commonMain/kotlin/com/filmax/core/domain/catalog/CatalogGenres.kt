package com.filmax.core.domain.catalog

/**
 * Опорные жанры kino.pub (`api/v1/genres`): id общие для фильмов и сериалов. Здесь только те, на
 * которые код ссылается сам, — остальные экраны берут из справочника жанров.
 */
object CatalogGenres {
    const val COMEDY = 1
    const val SCI_FI = 4
    const val CARTOON = 23

    /** Аниме у kino.pub — жанр поверх фильмов и сериалов, а не тип (см. ItemType.ANIME). */
    const val ANIME = 25
    const val DORAMA = 107
}
