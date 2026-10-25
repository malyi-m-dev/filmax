// Ряды ленты Главной: «Продолжить просмотр», топ-10 «В тренде», постерные ряды и «Подборки» с
// догрузкой страниц. Состав, порядок и заголовки рядов задаёт модель, ленту собирает HomeScreen.

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

/** Сколько мест в топе «В тренде» — первые тайтлы ряда по просмотрам. */
private const val TOP_COUNT = 10

/** Лента — это [HomeState.rows]: экран идёт по ним и рисует, состав и порядок задаёт модель. */
internal fun LazyListScope.homeRows(state: HomeState, onEvent: (HomeEvent) -> Unit) {
    state.rows.forEach { row ->
        if (row.isEmpty) return@forEach
        when (row) {
            is HomeRow.Continue -> continueRow(row, onEvent)
            is HomeRow.Titles -> titlesRow(row, onEvent)
            is HomeRow.Collections -> collectionsRow(row, onEvent)
        }
    }
}

/** «В тренде» — это просмотры недели: первые десять и есть топ, ряд постеров для них не нужен. */
private fun LazyListScope.titlesRow(row: HomeRow.Titles, onEvent: (HomeEvent) -> Unit) {
    if (row.id == HomeRowId.TRENDING) {
        topTenRow(row, onEvent)
    } else {
        posterRow(row, onEvent)
    }
}

private fun LazyListScope.continueRow(row: HomeRow.Continue, onEvent: (HomeEvent) -> Unit) {
    item(key = row.id.name) {
        TitledRow(title = row.title, count = row.entries.size) {
            items(row.entries, key = { it.itemId }) { entry ->
                FilmaxContinueCard(
                    title = entry.title,
                    meta = continueMeta(entry.progress),
                    imageUrl = entry.wideOrPoster,
                    progress = entry.progress?.fraction ?: 0f,
                    onClick = { onEvent(HomeEvent.ContinueWatching(entry)) },
                    modifier = Modifier.width(FilmaxMetrics.ContinueWidth),
                )
            }
        }
    }
}

private fun LazyListScope.topTenRow(row: HomeRow.Titles, onEvent: (HomeEvent) -> Unit) {
    val top = row.paging.items.take(TOP_COUNT)
    item(key = row.id.name) {
        TitledRow(title = row.title) {
            itemsIndexed(top, key = { _, rowItem -> rowItem.id }) { index, rowItem ->
                FilmaxTopTenCard(
                    rank = index + 1,
                    title = rowItem.title,
                    posterUrl = rowItem.posters.medium,
                    onClick = { onEvent(HomeEvent.OpenItem(rowItem.id)) },
                )
            }
        }
    }
}

private fun LazyListScope.posterRow(row: HomeRow.Titles, onEvent: (HomeEvent) -> Unit) {
    val rowItems = row.paging.items
    item(key = row.id.name) {
        TitledRow(title = row.title) {
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
                    onClick = { onEvent(HomeEvent.OpenItem(rowItem.id)) },
                    rating = ratingLabel(rowItem.rating.kinopoisk),
                    meta = posterMeta(typeLabel(rowItem.type), rowItem.year),
                    modifier = Modifier.width(FilmaxMetrics.PosterWidth),
                )
            }
        }
    }
}

private fun LazyListScope.collectionsRow(row: HomeRow.Collections, onEvent: (HomeEvent) -> Unit) {
    val collections = row.paging.items
    item(key = row.id.name) {
        TitledRow(title = row.title) {
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
                    onClick = { onEvent(HomeEvent.OpenCollection(collection)) },
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
