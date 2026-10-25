// Ряды ТВ-Главной: «Продолжить», постерные ряды, подборки и новинки. Состав, порядок и заголовки
// задаёт HomeScreenModel — здесь только то, какой карточкой рисуется каждый тип ряда.
package com.filmax.feature.home.tv

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.ui.Modifier
import com.filmax.core.domain.catalog.model.Collection
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.watching.model.WatchHistory
import com.filmax.core.tv.designsystem.TvCardSize
import com.filmax.core.tv.designsystem.TvCollectionCard
import com.filmax.core.tv.designsystem.TvCollectionTints
import com.filmax.core.tv.designsystem.TvPosterCard
import com.filmax.core.tv.designsystem.TvProgressCard
import com.filmax.core.tv.designsystem.TvRail
import com.filmax.core.tv.designsystem.TvScreenFocus
import com.filmax.core.tv.designsystem.tvRailEntry
import com.filmax.core.ui.components.continueMeta
import com.filmax.core.ui.components.isHighRating
import com.filmax.core.ui.components.posterMeta
import com.filmax.core.ui.components.posterUrl
import com.filmax.core.ui.components.ratingLabel
import com.filmax.core.ui.components.typeLabel
import com.filmax.feature.home.common.HomeEvent
import com.filmax.feature.home.common.HomeRow
import com.filmax.feature.home.common.HomeRowId

/**
 * Ряды ленты по порядку модели; пустой ряд пропускается — заголовок без карточек хуже его
 * отсутствия. Номер ряда — его место в модели (с 1, hero — 0): ряды выше ряда в фокусе приглушены.
 */
internal fun LazyListScope.homeRails(
    rows: List<HomeRow>,
    onEvent: (HomeEvent) -> Unit,
    focus: TvScreenFocus,
    focusedRow: MutableIntState,
) {
    rows.forEachIndexed { index, row ->
        if (row.isEmpty) return@forEachIndexed
        item(key = row.id.name, contentType = row::class) {
            Box(Modifier.feedRow(index + 1, focusedRow)) { HomeRail(row, onEvent, focus) }
        }
    }
}

@Composable
private fun HomeRail(row: HomeRow, onEvent: (HomeEvent) -> Unit, focus: TvScreenFocus) {
    when {
        row is HomeRow.Continue -> ContinueRail(row, onEvent, focus)
        row is HomeRow.Titles && row.id == HomeRowId.FRESH -> FreshRail(row, onEvent, focus)
        row is HomeRow.Titles -> PosterRail(row, onEvent, focus)
        row is HomeRow.Collections -> CollectionsRail(row, onEvent, focus)
    }
}

@Composable
private fun ContinueRail(row: HomeRow.Continue, onEvent: (HomeEvent) -> Unit, focus: TvScreenFocus) {
    TvRail(title = row.title, count = row.entries.size) {
        itemsIndexed(row.entries, key = { _, entry -> entry.itemId }) { index, entry ->
            ContinueCard(entry, onEvent, focus.item(returnKey(row.id, entry.itemId)).tvRailEntry(index))
        }
    }
}

@Composable
private fun ContinueCard(entry: WatchHistory, onEvent: (HomeEvent) -> Unit, modifier: Modifier) {
    TvProgressCard(
        title = entry.title,
        meta = continueMeta(entry.progress),
        // Карточка 16:9 — берём кадр, а не вертикальный постер: тот обрезался бы по центру.
        posterUrl = entry.wideOrPoster,
        progress = entry.progress?.fraction ?: 0f,
        onClick = { onEvent(HomeEvent.ContinueWatching(entry)) },
        modifier = modifier,
    )
}

@Composable
private fun PosterRail(row: HomeRow.Titles, onEvent: (HomeEvent) -> Unit, focus: TvScreenFocus) {
    val items = row.paging.items
    val ranked = row.id == HomeRowId.TRENDING
    TvRail(title = row.title, subtitle = row.subtitle) {
        itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
            LoadMoreAt(index, items.lastIndex) { onEvent(HomeEvent.LoadMoreRow(row.id)) }
            TvPosterCard(
                title = item.title,
                meta = posterMeta(typeLabel(item.type), item.year),
                posterUrl = item.posters.medium.ifEmpty { item.posters.big },
                onClick = { onEvent(HomeEvent.OpenItem(item.id)) },
                rating = ratingLabel(item.rating.kinopoisk),
                ratingHighlighted = isHighRating(item.rating.kinopoisk),
                rank = (index + 1).takeIf { ranked && index < RANKED_COUNT },
                modifier = focus.item(returnKey(row.id, item.id)).tvRailEntry(index),
            )
        }
    }
}

@Composable
private fun FreshRail(row: HomeRow.Titles, onEvent: (HomeEvent) -> Unit, focus: TvScreenFocus) {
    TvRail(title = row.title) {
        itemsIndexed(row.paging.items, key = { _, item -> item.id }) { index, item ->
            FreshCard(item, onEvent, focus.item(returnKey(row.id, item.id)).tvRailEntry(index))
        }
    }
}

@Composable
private fun FreshCard(item: Item, onEvent: (HomeEvent) -> Unit, modifier: Modifier) {
    TvProgressCard(
        title = item.title,
        meta = posterMeta(typeLabel(item.type), item.year),
        posterUrl = item.posters.wide ?: item.posters.big,
        progress = null,
        onClick = { onEvent(HomeEvent.OpenItem(item.id)) },
        modifier = modifier,
        size = TvCardSize.Wide,
        badge = freshBadge(item.type),
    )
}

@Composable
private fun CollectionsRail(row: HomeRow.Collections, onEvent: (HomeEvent) -> Unit, focus: TvScreenFocus) {
    val collections = row.paging.items
    TvRail(title = row.title, subtitle = row.subtitle) {
        itemsIndexed(collections, key = { _, collection -> collection.id }) { index, collection ->
            LoadMoreAt(index, collections.lastIndex) { onEvent(HomeEvent.LoadMoreRow(row.id)) }
            val modifier = focus.item(returnKey(row.id, collection.id)).tvRailEntry(index)
            CollectionCard(collection, row, index, onEvent, modifier)
        }
    }
}

@Composable
private fun CollectionCard(
    collection: Collection,
    row: HomeRow.Collections,
    index: Int,
    onEvent: (HomeEvent) -> Unit,
    modifier: Modifier
) {
    val preview = row.previews[collection.id]
    TvCollectionCard(
        title = collection.title,
        caption = collectionCaption(preview?.count),
        // Пока превью не догрузилось — обложка самой подборки.
        posters = preview?.posters ?: listOfNotNull(collection.posterUrl()),
        tint = TvCollectionTints[index % TvCollectionTints.size],
        onClick = { onEvent(HomeEvent.OpenCollection(collection)) },
        modifier = modifier,
    )
}

/**
 * Хвостовая карточка скомпонована — зритель долистал ряд почти до конца, просим следующую
 * страницу. Ленивый ряд композит только видимое (+префетч), поэтому это дешёвый триггер без
 * слежения за скроллом; повторные вызовы гасит идемпотентность модели.
 */
@Composable
private fun LoadMoreAt(index: Int, lastIndex: Int, onLoadMore: () -> Unit) {
    if (index == lastIndex) LaunchedEffect(lastIndex) { onLoadMore() }
}

/**
 * Ключ возврата фокуса: «ряд:id». Ряд в префиксе обязателен — один тайтл встречается сразу
 * в нескольких рядах, а ключ должен быть уникален в пределах экрана.
 */
private fun returnKey(row: HomeRowId, itemId: Int): String = "$row:$itemId"
