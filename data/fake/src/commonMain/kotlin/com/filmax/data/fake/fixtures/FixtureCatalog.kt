package com.filmax.data.fake.fixtures

import com.filmax.data.fake.store.FakeCatalog

/**
 * Каталог мок-режима: 58 тайтлов — 29 фильмов (одно аниме, три мультфильма), 18 сериалов (четыре
 * аниме, две дорамы), пять документальных сериалов и по два документальных фильма, ТВ-шоу и
 * концерта. Порядок здесь — «серверный» порядок при равных ключах сортировки.
 */
internal fun buildFixtureCatalog(): FakeCatalog {
    val titles = RUSSIAN_MOVIES + FOREIGN_MOVIES + FEATURED_SERIES + SERIES + ANIME + DOCUMENTARIES +
        CARTOONS + DORAMAS + DOCUMENTARY_FILMS + TV_SHOWS + CONCERTS
    val entries = titles.map { it.toEntry() }
    return FakeCatalog(
        entries = entries,
        genres = Genres.ALL,
        countries = Countries.ALL.map { it.toDomain() },
        collections = buildCollections(entries),
    )
}
