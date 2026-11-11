// Доменные понятия каталога → параметры API kino.pub. Раньше эти строки жили в домене
// (`apiValue`, `path`, id жанров), и второй источник получал их в контрактах как чужой формат.
package com.filmax.data.catalog

import com.filmax.core.domain.catalog.CatalogShortcut
import com.filmax.core.domain.catalog.CatalogSort
import com.filmax.core.domain.catalog.ReferenceGenre
import com.filmax.core.domain.catalog.SortOption
import com.filmax.core.domain.catalog.model.ItemType

/** Значение `type` kino.pub (`api/v1/types`). */
val ItemType.kinopubValue: String
    get() = when (this) {
        ItemType.MOVIE -> "movie"
        ItemType.SERIES -> "serial"
        ItemType.ANIME -> "anime"
        ItemType.DOCUMENTARY -> "docuserial"
        ItemType.DOCUMENTARY_MOVIE -> "documovie"
        ItemType.TV_SHOW -> "tvshow"
        ItemType.CONCERT -> "concert"
        ItemType.TV -> "tv"
    }

/**
 * Тип по значению kino.pub. Незнакомый — фильм: `3d` у kino.pub это фильмы в 3D, а `4k` пуст (4K —
 * фильтр `quality`, а не тип).
 */
fun kinopubItemType(value: String?): ItemType =
    ItemType.entries.firstOrNull { it.kinopubValue == value } ?: ItemType.MOVIE

/** Имя поля kino.pub для параметра `sort`. */
val CatalogSort.kinopubField: String
    get() = when (this) {
        CatalogSort.UPDATED -> "updated"
        CatalogSort.CREATED -> "created"
        CatalogSort.RATING -> "rating"
        CatalogSort.VIEWS -> "views"
        CatalogSort.YEAR -> "year"
        CatalogSort.KINOPOISK_RATING -> "kinopoisk_rating"
        CatalogSort.IMDB_RATING -> "imdb_rating"
    }

/**
 * Параметр `sort` с направлением. kino.pub: `sort=-field` — по УБЫВАНИЮ, `sort=field` — по
 * возрастанию (проверено живым API: `-rating` отдаёт топ, `rating` — худшее). Раньше знак
 * трактовался наоборот, и «лучшее сверху» на деле показывало хвост каталога.
 */
val SortOption.kinopubSort: String
    get() = if (ascending) field.kinopubField else "-${field.kinopubField}"

/**
 * Путь готовой выборки — `api/v1/items/<path>`. Выборки `new` в API нет (404 «Страница не
 * найдена») — свежие поступления отдаёт `fresh`.
 */
val CatalogShortcut.kinopubPath: String
    get() = when (this) {
        CatalogShortcut.HOT -> "hot"
        CatalogShortcut.POPULAR -> "popular"
        CatalogShortcut.FRESH -> "fresh"
    }

/** Опорные жанры — id справочника kino.pub (`api/v1/genres`), общие для фильмов и сериалов. */
val ReferenceGenre.kinopubId: Int
    get() = when (this) {
        ReferenceGenre.COMEDY -> 1
        ReferenceGenre.SCI_FI -> 4
        ReferenceGenre.CARTOON -> 23
        // Аниме у kino.pub — жанр поверх фильмов и сериалов, а не тип.
        ReferenceGenre.ANIME -> 25
        ReferenceGenre.DORAMA -> 107
    }

/**
 * Типы kino.pub, чьи жанры — кино и сериалы. Остальные (`music`) приходят в справочнике тем же
 * списком и в киношный каталог не попадают.
 */
internal val VIDEO_GENRE_TYPES = setOf("movie", "serial", "anime", "docuserial", "documovie", "tvshow", "3d")
