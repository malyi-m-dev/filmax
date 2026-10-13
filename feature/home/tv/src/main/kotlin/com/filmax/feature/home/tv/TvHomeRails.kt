// Ряды ТВ-Главной: «Продолжить», постерные ряды, подборки и новинки. Состав и порядок задаёт
// HomeScreenModel — здесь только то, какой карточкой рисуется каждый тип ряда.
package com.filmax.feature.home.tv

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.filmax.core.ui.components.continueMeta
import com.filmax.core.ui.components.isHighRating
import com.filmax.core.ui.components.posterMeta
import com.filmax.core.ui.components.posterUrl
import com.filmax.core.ui.components.ratingLabel
import com.filmax.core.ui.components.typeLabel
import com.filmax.feature.home.common.HomeRow
import com.filmax.feature.home.common.HomeRowId

/** Действия рядов: открыть тайтл, играть с места, открыть подборку, догрузить ряд. */
internal data class RailActions(
    val onOpenItem: (Int) -> Unit,
    val onPlay: (itemId: Int, season: Int, videoId: Int) -> Unit,
    val onOpenCollection: (id: Int, title: String) -> Unit,
    val onLoadMore: (HomeRowId) -> Unit,
)

/**
 * Ряды ленты по порядку модели; пустой ряд пропускается — заголовок без карточек хуже его
 * отсутствия. Номер ряда — его место в модели (с 1, hero — 0): ряды выше ряда в фокусе приглушены.
 */
internal fun LazyListScope.homeRails(rows: List<HomeRow>, actions: RailActions, focus: FeedItemFocus) {
    rows.forEachIndexed { index, row ->
        if (row.isEmpty) return@forEachIndexed
        item(key = row.id.name, contentType = row::class) {
            Box(Modifier.feedRow(index + 1, focus.feed)) { HomeRail(row, actions, focus.screen) }
        }
    }
}

@Composable
private fun HomeRail(row: HomeRow, actions: RailActions, focus: TvScreenFocus) {
    when {
        row is HomeRow.Continue -> ContinueRail(row, actions, focus)
        row is HomeRow.Titles && row.id == HomeRowId.FRESH -> FreshRail(row, actions, focus)
        row is HomeRow.Titles -> PosterRail(row, actions, focus)
        row is HomeRow.Collections -> CollectionsRail(row, actions, focus)
    }
}

@Composable
private fun ContinueRail(row: HomeRow.Continue, actions: RailActions, focus: TvScreenFocus) {
    TvRail(title = row.id.heading.title, count = row.entries.size) {
        itemsIndexed(row.entries, key = { _, entry -> entry.itemId }) { _, entry ->
            ContinueCard(entry, actions, focus.item(returnKey(row.id, entry.itemId)))
        }
    }
}

@Composable
private fun ContinueCard(entry: WatchHistory, actions: RailActions, modifier: Modifier) {
    TvProgressCard(
        title = entry.title,
        meta = continueMeta(entry.progress),
        // Карточка 16:9 — берём кадр, а не вертикальный постер: тот обрезался бы по центру.
        posterUrl = entry.wideOrPoster,
        progress = entry.progress?.fraction ?: 0f,
        // Ряд продолжения ведёт сразу в плеер — на недосмотренную серию (номер и сезон из истории).
        onClick = {
            actions.onPlay(entry.itemId, entry.progress?.season ?: NO_SEASON, entry.progress?.videoId ?: NO_VIDEO_ID)
        },
        modifier = modifier,
    )
}

@Composable
private fun PosterRail(row: HomeRow.Titles, actions: RailActions, focus: TvScreenFocus) {
    val items = row.paging.items
    val ranked = row.id == HomeRowId.TRENDING
    TvRail(title = row.id.heading.title, subtitle = row.id.heading.subtitle) {
        itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
            LoadMoreAt(index, items.lastIndex) { actions.onLoadMore(row.id) }
            TvPosterCard(
                title = item.title,
                meta = posterMeta(typeLabel(item.type), item.year),
                posterUrl = item.posters.medium.ifEmpty { item.posters.big },
                onClick = { actions.onOpenItem(item.id) },
                rating = ratingLabel(item.rating.kinopoisk),
                ratingHighlighted = isHighRating(item.rating.kinopoisk),
                rank = (index + 1).takeIf { ranked && index < RANKED_COUNT },
                modifier = focus.item(returnKey(row.id, item.id)),
            )
        }
    }
}

@Composable
private fun FreshRail(row: HomeRow.Titles, actions: RailActions, focus: TvScreenFocus) {
    TvRail(title = row.id.heading.title) {
        itemsIndexed(row.paging.items, key = { _, item -> item.id }) { _, item ->
            FreshCard(item, actions, focus.item(returnKey(row.id, item.id)))
        }
    }
}

@Composable
private fun FreshCard(item: Item, actions: RailActions, modifier: Modifier) {
    TvProgressCard(
        title = item.title,
        meta = posterMeta(typeLabel(item.type), item.year),
        posterUrl = item.posters.wide ?: item.posters.big,
        progress = null,
        onClick = { actions.onOpenItem(item.id) },
        modifier = modifier,
        size = TvCardSize.Wide,
        badge = freshBadge(item.type),
    )
}

@Composable
private fun CollectionsRail(row: HomeRow.Collections, actions: RailActions, focus: TvScreenFocus) {
    val collections = row.paging.items
    TvRail(title = row.id.heading.title, subtitle = row.id.heading.subtitle) {
        itemsIndexed(collections, key = { _, collection -> collection.id }) { index, collection ->
            LoadMoreAt(index, collections.lastIndex) { actions.onLoadMore(row.id) }
            CollectionCard(collection, row, index, actions, focus.item(returnKey(row.id, collection.id)))
        }
    }
}

@Composable
private fun CollectionCard(
    collection: Collection,
    row: HomeRow.Collections,
    index: Int,
    actions: RailActions,
    modifier: Modifier
) {
    val preview = row.previews[collection.id]
    TvCollectionCard(
        title = collection.title,
        caption = collectionCaption(preview?.count),
        // Пока превью не догрузилось — обложка самой подборки.
        posters = preview?.posters ?: listOfNotNull(collection.posterUrl()),
        tint = TvCollectionTints[index % TvCollectionTints.size],
        onClick = { actions.onOpenCollection(collection.id, collection.title) },
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

/** `PlayerRoute.videoId` для фильма/неизвестной серии — плеер возьмёт первый трек. */
internal const val NO_VIDEO_ID = -1

/** `PlayerRoute.season` для фильма/неизвестного сезона. */
internal const val NO_SEASON = -1
