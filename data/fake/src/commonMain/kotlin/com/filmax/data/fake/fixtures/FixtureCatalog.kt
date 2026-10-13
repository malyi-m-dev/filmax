package com.filmax.data.fake.fixtures

import com.filmax.data.fake.store.FakeCatalog

/**
 * Каталог мок-режима: 49 тайтлов — 27 фильмов (одно аниме), 17 сериалов (четыре аниме) и пять
 * документальных сериалов. Порядок здесь — «серверный» порядок при равных ключах сортировки.
 */
internal fun buildFixtureCatalog(): FakeCatalog {
    val titles = RUSSIAN_MOVIES + FOREIGN_MOVIES + FEATURED_SERIES + SERIES + ANIME + DOCUMENTARIES
    val entries = titles.map { it.toEntry() }
    return FakeCatalog(
        entries = entries,
        genres = Genres.ALL,
        countries = Countries.ALL.map { it.toDomain() },
        collections = buildCollections(entries),
    )
}
