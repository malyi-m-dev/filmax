// Ряды ленты Главной: «Продолжить просмотр», «Топ-10 недели», постерные ряды и «Подборки» с
// догрузкой страниц. Состав и порядок рядов задаёт модель, ленту собирает HomeScreen.

package com.filmax.feature.home.mobile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.ui.components.FilmaxCollectionCard
import com.filmax.core.ui.components.FilmaxContinueCard
import com.filmax.core.ui.components.FilmaxPosterCard
import com.filmax.core.ui.components.FilmaxSectionHeader
import com.filmax.core.ui.components.FilmaxTopTenCard
import com.filmax.core.ui.components.collectionHue
import com.filmax.core.ui.components.continueMeta
import com.filmax.core.ui.components.posterMeta
import com.filmax.core.ui.components.posterUrl
import com.filmax.core.ui.components.ratingLabel
import com.filmax.core.ui.components.titlesLabel
import com.filmax.core.ui.components.typeLabel
import com.filmax.feature.home.common.HomeEvent
import com.filmax.feature.home.common.HomeRow
import com.filmax.feature.home.common.HomeRowId
import com.filmax.feature.home.common.HomeState

/** Сколько мест в «Топ-10» — первые тайтлы ряда «В тренде» по просмотрам. */
private const val TOP_COUNT = 10

/** Лента — это [HomeState.rows]: экран идёт по ним и рисует, состав и порядок задаёт модель. */
internal fun LazyListScope.homeRows(state: HomeState, actions: HomeActions) {
    val onEvent = actions.onEvent
    state.rows.forEach { row ->
        if (row.isEmpty) return@forEach
        when (row) {
            is HomeRow.Continue -> continueRow(row, onPlay = actions.onPlay)
            is HomeRow.Titles -> titlesRow(row, actions.onOpenItem, onEvent)
            is HomeRow.Collections -> collectionsRow(row, actions.onOpenCollection, onEvent)
        }
    }
}

/**
 * Заголовки рядов живут в экране, а не в модели: у ТВ для того же ряда своя формулировка.
 * Тренды здесь не названы — у них свой ряд «Топ-10 недели».
 */
private val HomeRowId.title: String
    get() = when (this) {
        HomeRowId.CONTINUE -> "Продолжить просмотр"
        HomeRowId.TRENDING -> "Топ-10 недели"
        // Заголовок честный: forYou — это топ сериалов по рейтингу, персонализации в фиде нет.
        HomeRowId.FOR_YOU -> "Сериалы с высоким рейтингом"
        HomeRowId.COLLECTIONS -> "Подборки"
        HomeRowId.FRESH -> "Новинки"
        HomeRowId.POPULAR_SERIES -> "Популярные сериалы"
        HomeRowId.CARTOONS -> "Мультфильмы"
        HomeRowId.COMEDIES -> "Комедии"
        HomeRowId.SCI_FI -> "Фантастика"
        HomeRowId.ANIME -> "Аниме"
        HomeRowId.DORAMAS -> "Дорамы"
        HomeRowId.DOCUMENTARY -> "Документальные фильмы"
        HomeRowId.TV_SHOWS -> "ТВ-шоу"
        HomeRowId.CONCERTS -> "Концерты"
    }

/** «В тренде» — это просмотры недели: первые десять и есть топ, ряд постеров для них не нужен. */
private fun LazyListScope.titlesRow(row: HomeRow.Titles, onOpenItem: (Int) -> Unit, onEvent: (HomeEvent) -> Unit) {
    if (row.id == HomeRowId.TRENDING) {
        topTenRow(row, onOpenItem)
    } else {
        posterRow(row, onOpenItem, onEvent)
    }
}

private fun LazyListScope.continueRow(
    row: HomeRow.Continue,
    onPlay: (itemId: Int, season: Int, videoId: Int) -> Unit,
) {
    item(key = row.id.name) {
        TitledRow(title = row.id.title, count = row.entries.size) {
            items(row.entries, key = { it.itemId }) { entry ->
                // Ряд продолжения ведёт сразу в плеер, минуя детали: недосмотренный эпизод —
                // videoId+сезон из истории, позицию внутри трека восстановит PlayerScreenModel.
                FilmaxContinueCard(
                    title = entry.title,
                    meta = continueMeta(entry.progress),
                    imageUrl = entry.wideOrPoster,
                    progress = entry.progress?.fraction ?: 0f,
                    onClick = {
                        onPlay(
                            entry.itemId,
                            entry.progress?.season ?: NO_SEASON,
                            entry.progress?.videoId ?: NO_VIDEO_ID,
                        )
                    },
                    modifier = Modifier.width(FilmaxMetrics.ContinueWidth),
                )
            }
        }
    }
}

private fun LazyListScope.topTenRow(row: HomeRow.Titles, onOpenItem: (Int) -> Unit) {
    val top = row.paging.items.take(TOP_COUNT)
    item(key = row.id.name) {
        TitledRow(title = row.id.title) {
            itemsIndexed(top, key = { _, rowItem -> rowItem.id }) { index, rowItem ->
                FilmaxTopTenCard(
                    rank = index + 1,
                    title = rowItem.title,
                    posterUrl = rowItem.posters.medium,
                    onClick = { onOpenItem(rowItem.id) },
                )
            }
        }
    }
}

private fun LazyListScope.posterRow(
    row: HomeRow.Titles,
    onOpenItem: (Int) -> Unit,
    onEvent: (HomeEvent) -> Unit,
) {
    val rowItems = row.paging.items
    item(key = row.id.name) {
        TitledRow(title = row.id.title) {
            itemsIndexed(rowItems, key = { _, rowItem -> rowItem.id }) { index, rowItem ->
                // Хвостовая карточка скомпонована — ряд долистан почти до конца: просим
                // следующую страницу. Ленивый ряд композит только видимое (+префетч), так что
                // триггер дешёвый; повторные вызовы гасит идемпотентность модели.
                if (index == rowItems.lastIndex) {
                    LaunchedEffect(rowItems.size) { onEvent(HomeEvent.LoadMoreRow(row.id)) }
                }
                FilmaxPosterCard(
                    title = rowItem.title,
                    posterUrl = rowItem.posters.medium,
                    onClick = { onOpenItem(rowItem.id) },
                    rating = ratingLabel(rowItem.rating.kinopoisk),
                    meta = posterMeta(typeLabel(rowItem.type), rowItem.year),
                    modifier = Modifier.width(FilmaxMetrics.PosterWidth),
                )
            }
        }
    }
}

private fun LazyListScope.collectionsRow(
    row: HomeRow.Collections,
    onOpenCollection: (id: Int, title: String) -> Unit,
    onEvent: (HomeEvent) -> Unit,
) {
    val collections = row.paging.items
    item(key = row.id.name) {
        TitledRow(title = row.id.title) {
            itemsIndexed(collections, key = { _, collection -> collection.id }) { index, collection ->
                // Хвостовая карточка скомпонована — просим следующую страницу (как у постер-рядов).
                if (index == collections.lastIndex) {
                    LaunchedEffect(collections.size) { onEvent(HomeEvent.LoadMoreRow(row.id)) }
                }
                // Число тайтлов — только точное (превью знает его у подборок в одну страницу).
                val preview = row.previews[collection.id]
                FilmaxCollectionCard(
                    title = collection.title,
                    hue = collectionHue(collection.id),
                    onClick = { onOpenCollection(collection.id, collection.title) },
                    subtitle = preview?.count?.let(::titlesLabel),
                    posterUrl = preview?.posters?.firstOrNull() ?: collection.posterUrl(),
                )
            }
        }
    }
}

/** Заголовок ряда и сам ряд: карточки листаются пальцем, края ряда — на полях экрана. */
@Composable
private fun TitledRow(title: String, count: Int? = null, content: LazyListScope.() -> Unit) {
    Column {
        FilmaxSectionHeader(title = title, count = count)
        LazyRow(
            contentPadding = PaddingValues(horizontal = FilmaxMetrics.ScreenPadding),
            horizontalArrangement = Arrangement.spacedBy(FilmaxMetrics.CardGap),
            content = content,
        )
    }
}
