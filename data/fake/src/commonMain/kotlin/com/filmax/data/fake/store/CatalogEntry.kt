package com.filmax.data.fake.store

import com.filmax.core.domain.catalog.model.Collection
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.person.CastMember
import com.filmax.data.fake.FakeIds

/**
 * Тайтл глазами «сервера»: доменный [Item] целиком — с сезонами и сериями, но без прогресса
 * (он живёт в [WatchStore]) — плюс то, чего в домене нет, но по чему сервер фильтрует и
 * сортирует: просмотры, страна по id, наличие 4K, актёры с ролями и фото.
 */
internal data class CatalogEntry(
    val item: Item,
    val views: Int,
    val countryId: Int,
    val has4k: Boolean,
    val cast: List<CastMember>,
) {
    val id: Int get() = item.id

    /** Аниме у kino.pub — жанр поверх фильма или сериала, а не отдельный тип; каталог приложения считает так же. */
    val isAnime: Boolean get() = item.genres.any { it.id == FakeIds.ANIME_GENRE }

    /** Сериальный формат — всё, кроме фильма: сериалы, аниме-сериалы, док. сериалы. */
    val isEpisodic: Boolean get() = item.type != ItemType.MOVIE

    /** Тип в смысле фильтра каталога: «Аниме» — по жанру, остальные — по полю type. */
    fun isOfType(type: ItemType): Boolean = if (type == ItemType.ANIME) isAnime else item.type == type
}

/** Подборка и её состав по id тайтлов — в порядке, в котором их отдаёт «сервер». */
internal data class FakeCollection(
    val collection: Collection,
    val itemIds: List<Int>,
)
