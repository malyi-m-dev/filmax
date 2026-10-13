package com.filmax.data.fake.fixtures

import com.filmax.core.domain.catalog.model.Collection
import com.filmax.data.fake.FakeIds
import com.filmax.data.fake.store.CatalogEntry
import com.filmax.data.fake.store.FakeCollection

private data class CollectionSpec(
    val id: Int,
    val title: String,
    val description: String?,
    val itemIds: List<Int>,
)

/**
 * Подборки, составленные «редактором». Одна без описания, одна с названием в две строки — для
 * карточек и шапки экрана подборки.
 */
private val CURATED: List<CollectionSpec> = listOf(
    CollectionSpec(
        id = 102,
        title = "Сериалы на один вечер",
        description = "Мини-сериалы и короткие сезоны: начать после ужина и успеть до полуночи.",
        itemIds = listOf(FakeIds.SHORT_SERIES, 2006, 2013, 3002, 4001, 4004),
    ),
    CollectionSpec(
        id = 103,
        title = "Атмосферные детективы",
        description = "Туман, маленькие города и следователи, которым самим есть что скрывать.",
        itemIds = listOf(FakeIds.MEDIUM_SERIES, 1011, 2004, 1014, 2013, 1009, 2008, 2006, 1017),
    ),
    CollectionSpec(
        id = 104,
        title = "Для всей семьи",
        description = "Можно смотреть с детьми — и самим не заскучать.",
        itemIds = listOf(FakeIds.HERO_MOVIE, 1020, 3005, 3004, 1015, 4001),
    ),
    CollectionSpec(
        id = 105,
        title = "Аниме для новичков",
        description = null,
        itemIds = listOf(FakeIds.ANIME_SERIES, 3002, 3005, 3003, 3004),
    ),
    CollectionSpec(
        id = 106,
        title = "Север зовёт",
        description = "Полярная ночь, льды и люди, которые остаются там, откуда все уезжают.",
        itemIds = listOf(FakeIds.MEDIUM_SERIES, 1002, 1015, 1021, 4001, 2013, 1024, 2007),
    ),
    CollectionSpec(
        id = 107,
        title = "Фантастика ближнего прицела: будущее, которое может наступить завтра утром",
        description = "Космос, искусственный интеллект и технологии на расстоянии вытянутой руки.",
        itemIds = listOf(1012, FakeIds.MOVIE_IN_PROGRESS, 2007, 1023, 3003, 4004),
    ),
    CollectionSpec(
        id = 108,
        title = "Природа и наука",
        description = "Документальные сериалы, после которых хочется в экспедицию.",
        itemIds = listOf(FakeIds.DOCUMENTARY_SERIES, 4001, 4004, 4003, 4005),
    ),
)

/**
 * Восемь подборок. Первая — «Зрители рекомендуют»: все тайтлы с оценкой, от лучшего, — их больше
 * сорока, поэтому у неё три страницы и на ней видно догрузку.
 */
internal fun buildCollections(entries: List<CatalogEntry>): List<FakeCollection> {
    val recommended = CollectionSpec(
        id = FakeIds.LARGE_COLLECTION,
        title = "Зрители рекомендуют",
        description = "Всё, что зрители успели оценить, — от лучшего к просто хорошему.",
        itemIds = entries
            .filter { it.item.rating.external != null }
            .sortedByDescending { it.item.rating.external }
            .map { it.id },
    )
    return (listOf(recommended) + CURATED).map { spec ->
        FakeCollection(
            collection = Collection(
                id = spec.id,
                title = spec.title,
                description = spec.description,
                posters = collectionPosters(spec.id),
            ),
            itemIds = spec.itemIds,
        )
    }
}
