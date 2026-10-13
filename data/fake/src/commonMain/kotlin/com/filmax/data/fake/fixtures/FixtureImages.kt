package com.filmax.data.fake.fixtures

import com.filmax.core.domain.catalog.model.Posters

// picsum.photos отдаёт одну и ту же картинку на один seed — без ключей и квот. Seed строится из
// id, поэтому постер тайтла не меняется от запуска к запуску и совпадает на всех экранах.

private data class ImageSize(val width: Int, val height: Int)

private val POSTER_SMALL = ImageSize(width = 200, height = 300)
private val POSTER_MEDIUM = ImageSize(width = 400, height = 600)
private val POSTER_BIG = ImageSize(width = 800, height = 1200)
private val WIDE = ImageSize(width = 1280, height = 720)
private val STILL = ImageSize(width = 640, height = 360)
private val COVER_SMALL = ImageSize(width = 300, height = 200)
private val COVER_MEDIUM = ImageSize(width = 600, height = 400)
private val COVER_BIG = ImageSize(width = 900, height = 600)
private val PERSON_PHOTO = ImageSize(width = 300, height = 450)
private val AVATAR = ImageSize(width = 200, height = 200)

private fun picsum(seed: String, size: ImageSize): String =
    "https://picsum.photos/seed/$seed/${size.width}/${size.height}"

/** Постеры 2:3 одного кадра в трёх размерах и отдельный широкий кадр 16:9. */
internal fun titlePosters(itemId: Int): Posters = Posters(
    small = picsum("filmax-$itemId", POSTER_SMALL),
    medium = picsum("filmax-$itemId", POSTER_MEDIUM),
    big = picsum("filmax-$itemId", POSTER_BIG),
    wide = picsum("filmax-wide-$itemId", WIDE),
)

/** Обложка подборки — горизонтальная, своя для каждой подборки. */
internal fun collectionPosters(collectionId: Int): Posters = Posters(
    small = picsum("filmax-collection-$collectionId", COVER_SMALL),
    medium = picsum("filmax-collection-$collectionId", COVER_MEDIUM),
    big = picsum("filmax-collection-$collectionId", COVER_BIG),
    wide = picsum("filmax-collection-wide-$collectionId", WIDE),
)

/** Кадр серии 16:9; у фильма — кадр его единственного видео. */
internal fun stillUrl(itemId: Int, season: Int, episode: Int): String =
    picsum("filmax-still-$itemId-$season-$episode", STILL)

internal fun personPhotoUrl(personId: Int): String = picsum("filmax-person-$personId", PERSON_PHOTO)

internal fun avatarUrl(): String = picsum("filmax-avatar", AVATAR)
