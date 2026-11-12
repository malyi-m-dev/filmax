// Ряды ТВ-Главной: «Продолжить», постерные ряды, подборки и новинки. Состав и порядок задаёт
// HomeScreenModel — здесь только то, какой карточкой рисуется каждый тип ряда.
package com.filmax.feature.home.tv

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.filmax.core.domain.catalog.model.VideoRef
import com.filmax.core.presentation.cards.CollectionCard
import com.filmax.core.presentation.cards.TitleCard
import com.filmax.core.presentation.cards.WatchCard
import com.filmax.core.tv.designsystem.TvCardSize
import com.filmax.core.tv.designsystem.TvCollectionCard
import com.filmax.core.tv.designsystem.TvCollectionTints
import com.filmax.core.tv.designsystem.TvPosterCard
import com.filmax.core.tv.designsystem.TvProgressCard
import com.filmax.core.tv.designsystem.TvRail
import com.filmax.core.tv.designsystem.TvScreenFocus
import com.filmax.core.tv.designsystem.tvRailEntry
import com.filmax.feature.home.common.HomeRow
import com.filmax.feature.home.common.HomeRowId

/** Действия рядов: открыть тайтл, играть с места, открыть подборку, догрузить ряд. */
internal data class RailActions(
    val onOpenItem: (Int) -> Unit,
    val onPlay: (itemId: Int, video: VideoRef?) -> Unit,
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
        itemsIndexed(row.entries, key = { _, card -> card.itemId }) { index, card ->
            ContinueCard(card, actions, focus.item(returnKey(row.id, card.itemId)).tvRailEntry(index))
        }
    }
}

@Composable
private fun ContinueCard(card: WatchCard, actions: RailActions, modifier: Modifier) {
    TvProgressCard(
        title = card.title,
        meta = card.continueMeta,
        // Карточка 16:9 — кадр, а не вертикальный постер: тот обрезался бы по центру.
        posterUrl = card.imageUrl,
        progress = card.progress,
        // Ряд продолжения ведёт сразу в плеер — на недосмотренную серию (номер и сезон из истории).
        onClick = { actions.onPlay(card.itemId, card.video) },
        modifier = modifier,
    )
}

@Composable
private fun PosterRail(row: HomeRow.Titles, actions: RailActions, focus: TvScreenFocus) {
    val cards = row.paging.items
    val ranked = row.id == HomeRowId.TRENDING
    TvRail(title = row.id.heading.title, subtitle = row.id.heading.subtitle) {
        itemsIndexed(cards, key = { _, card -> card.id }) { index, card ->
            LoadMoreAt(index, cards.lastIndex) { actions.onLoadMore(row.id) }
            TvPosterCard(
                title = card.title,
                meta = card.typeAndYear,
                posterUrl = card.posterUrl,
                onClick = { actions.onOpenItem(card.id) },
                rating = card.rating,
                ratingHighlighted = card.ratingHigh,
                rank = (index + 1).takeIf { ranked && index < RANKED_COUNT },
                modifier = focus.item(returnKey(row.id, card.id)).tvRailEntry(index),
            )
        }
    }
}

@Composable
private fun FreshRail(row: HomeRow.Titles, actions: RailActions, focus: TvScreenFocus) {
    TvRail(title = row.id.heading.title) {
        itemsIndexed(row.paging.items, key = { _, card -> card.id }) { index, card ->
            FreshCard(card, actions, focus.item(returnKey(row.id, card.id)).tvRailEntry(index))
        }
    }
}

@Composable
private fun FreshCard(card: TitleCard, actions: RailActions, modifier: Modifier) {
    TvProgressCard(
        title = card.title,
        meta = card.typeAndYear,
        posterUrl = card.widePosterUrl,
        progress = null,
        onClick = { actions.onOpenItem(card.id) },
        modifier = modifier,
        size = TvCardSize.Wide,
        badge = card.freshBadge,
    )
}

@Composable
private fun CollectionsRail(row: HomeRow.Collections, actions: RailActions, focus: TvScreenFocus) {
    val cards = row.cards
    TvRail(title = row.id.heading.title, subtitle = row.id.heading.subtitle) {
        itemsIndexed(cards, key = { _, card -> card.id }) { index, card ->
            LoadMoreAt(index, cards.lastIndex) { actions.onLoadMore(row.id) }
            CollectionRailCard(card, index, actions, focus.item(returnKey(row.id, card.id)).tvRailEntry(index))
        }
    }
}

@Composable
private fun CollectionRailCard(card: CollectionCard, index: Int, actions: RailActions, modifier: Modifier) {
    TvCollectionCard(
        title = card.title,
        caption = card.filmsCount,
        // Пока превью не догрузилось — обложка самой подборки (так собирает карточку модель).
        posters = card.posters,
        tint = TvCollectionTints[index % TvCollectionTints.size],
        onClick = { actions.onOpenCollection(card.id, card.title) },
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
