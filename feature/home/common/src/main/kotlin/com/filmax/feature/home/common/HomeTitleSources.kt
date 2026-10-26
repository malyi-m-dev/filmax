// Откуда ряды тайтлов Главной берут страницы: тип, жанр и порядок выдачи — данными, а не ветками
// кода. Новый тематический ряд — строка в THEME_SOURCES и заголовок в ROW_HEADINGS.
package com.filmax.feature.home.common

import com.filmax.core.domain.catalog.CatalogFilters
import com.filmax.core.domain.catalog.CatalogGenres
import com.filmax.core.domain.catalog.CatalogRepository
import com.filmax.core.domain.catalog.CatalogShortcut
import com.filmax.core.domain.catalog.CatalogSort
import com.filmax.core.domain.catalog.SortOption
import com.filmax.core.domain.catalog.model.ItemPage
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.common.RequestResult

/**
 * Источник ряда тайтлов. Видов два, потому что у kino.pub два вида выдачи: каталог с жанром и
 * сортировкой — и готовые выборки, где порядок решает сервер.
 */
internal sealed interface TitleSource {

    /** Выдача каталога `api/v1/items`: тип, необязательный жанр и поле сортировки по убыванию. */
    data class Catalog(val type: ItemType, val sort: CatalogSort, val genreId: Int? = null) : TitleSource

    /** Готовая выборка kino.pub одного типа. */
    data class Shortcut(val type: ItemType, val shortcut: CatalogShortcut) : TitleSource
}

/** Страница [page] источника — единственное место, где вид источника превращается в запрос. */
internal suspend fun CatalogRepository.titlesPage(source: TitleSource, page: Int): RequestResult<ItemPage> =
    when (source) {
        is TitleSource.Catalog ->
            getItems(source.type, source.genreId, CatalogFilters(), SortOption(source.sort), page)
        is TitleSource.Shortcut -> getShortcutItems(source.shortcut, source.type, page)
    }

/** Ряды фида, которые листаются дальше: стартовую горстку даёт фид, следующие страницы — каталог. */
private val FEED_SOURCES: Map<HomeRowId, TitleSource> = mapOf(
    HomeRowId.TRENDING to TitleSource.Catalog(ItemType.MOVIE, CatalogSort.VIEWS),
    HomeRowId.FOR_YOU to TitleSource.Catalog(ItemType.SERIES, CatalogSort.RATING),
)

/**
 * Тематические ряды в порядке ленты — после «Новинок». Ниже фида они стоят потому, что и приходят
 * позже него: ряд, появившийся под тем, что зритель уже видит, ленту под фокусом не сдвигает, а
 * первый экран Главной остаётся прежним. Внутри — от массового к нишевому: сериалы, семейное и
 * жанровое кино, потом аниме и дорамы, в конце форматы не про игровое кино. На пульте каждый ряд —
 * ещё одно «вниз», и до нишевого спускается тот, кому оно нужно.
 *
 * Жанровые ряды — выдача каталога по просмотрам: жанр принимает именно она
 * (`items?type=movie&genre=23&sort=-views`), готовые выборки фильтруют только по типу.
 */
internal val THEME_SOURCES: Map<HomeRowId, TitleSource> = mapOf(
    HomeRowId.POPULAR_SERIES to TitleSource.Shortcut(ItemType.SERIES, CatalogShortcut.POPULAR),
    HomeRowId.CARTOONS to TitleSource.Catalog(ItemType.MOVIE, CatalogSort.VIEWS, CatalogGenres.CARTOON),
    HomeRowId.COMEDIES to TitleSource.Catalog(ItemType.MOVIE, CatalogSort.VIEWS, CatalogGenres.COMEDY),
    HomeRowId.SCI_FI to TitleSource.Catalog(ItemType.MOVIE, CatalogSort.VIEWS, CatalogGenres.SCI_FI),
    HomeRowId.ANIME to TitleSource.Catalog(ItemType.SERIES, CatalogSort.VIEWS, CatalogGenres.ANIME),
    HomeRowId.DORAMAS to TitleSource.Catalog(ItemType.SERIES, CatalogSort.VIEWS, CatalogGenres.DORAMA),
    HomeRowId.DOCUMENTARY to TitleSource.Shortcut(ItemType.DOCUMENTARY_MOVIE, CatalogShortcut.POPULAR),
    HomeRowId.TV_SHOWS to TitleSource.Shortcut(ItemType.TV_SHOW, CatalogShortcut.POPULAR),
    HomeRowId.CONCERTS to TitleSource.Shortcut(ItemType.CONCERT, CatalogShortcut.POPULAR),
)

/** Все ряды, которые листаются из каталога. «Новинок» здесь нет: это сводка двух выборок, листать её нечем. */
internal val TITLE_SOURCES: Map<HomeRowId, TitleSource> = FEED_SOURCES + THEME_SOURCES

/** Заголовок ряда и пояснение рядом с ним — одни на телефон и ТВ. */
internal data class RowHeading(val title: String, val subtitle: String? = null)

/**
 * Заголовки рядов тайтлов. Пояснения — там, где без них ряд читается шире, чем он есть: аниме
 * здесь только сериалы, а «дорама» знакома не каждому. У «Для вас» заголовок честный: это топ
 * сериалов по рейтингу, персонализации в фиде нет.
 */
internal val ROW_HEADINGS: Map<HomeRowId, RowHeading> = mapOf(
    HomeRowId.TRENDING to RowHeading("В тренде", "на этой неделе"),
    HomeRowId.FOR_YOU to RowHeading("Сериалы с высоким рейтингом", "КиноПоиск 8+"),
    HomeRowId.FRESH to RowHeading("Новинки"),
    HomeRowId.POPULAR_SERIES to RowHeading("Популярные сериалы"),
    HomeRowId.CARTOONS to RowHeading("Мультфильмы"),
    HomeRowId.COMEDIES to RowHeading("Комедии"),
    HomeRowId.SCI_FI to RowHeading("Фантастика"),
    HomeRowId.ANIME to RowHeading("Аниме", "сериалы"),
    HomeRowId.DORAMAS to RowHeading("Дорамы", "азиатские сериалы"),
    HomeRowId.DOCUMENTARY to RowHeading("Документальное кино"),
    HomeRowId.TV_SHOWS to RowHeading("ТВ-шоу"),
    HomeRowId.CONCERTS to RowHeading("Концерты"),
)
