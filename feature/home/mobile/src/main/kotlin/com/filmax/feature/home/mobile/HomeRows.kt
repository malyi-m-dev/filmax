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
import com.filmax.core.domain.catalog.model.VideoRef
import com.filmax.core.ui.components.FilmaxCollectionCard
import com.filmax.core.ui.components.FilmaxContinueCard
import com.filmax.core.ui.components.FilmaxPosterCard
import com.filmax.core.ui.components.FilmaxSectionHeader
import com.filmax.core.ui.components.FilmaxTopTenCard
import com.filmax.core.ui.components.collectionHue
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
    onPlay: (itemId: Int, video: VideoRef?) -> Unit,
) {
    item(key = row.id.name) {
        TitledRow(title = row.id.title, count = row.entries.size) {
            items(row.entries, key = { it.itemId }) { card ->
                // Ряд продолжения ведёт сразу в плеер, минуя детали: недосмотренный эпизод — адрес
                // из истории, позицию внутри трека восстановит PlayerScreenModel.
                FilmaxContinueCard(
                    title = card.title,
                    meta = card.continueMeta,
                    imageUrl = card.imageUrl,
                    progress = card.progress,
                    onClick = { onPlay(card.itemId, card.video) },
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
            itemsIndexed(top, key = { _, card -> card.id }) { index, card ->
                FilmaxTopTenCard(
                    rank = index + 1,
                    title = card.title,
                    posterUrl = card.posterUrl,
                    onClick = { onOpenItem(card.id) },
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
    val cards = row.paging.items
    item(key = row.id.name) {
        TitledRow(title = row.id.title) {
            itemsIndexed(cards, key = { _, card -> card.id }) { index, card ->
                // Хвостовая карточка скомпонована — ряд долистан почти до конца: просим
                // следующую страницу. Ленивый ряд композит только видимое (+префетч), так что
                // триггер дешёвый; повторные вызовы гасит идемпотентность модели.
                if (index == cards.lastIndex) {
                    LaunchedEffect(cards.size) { onEvent(HomeEvent.LoadMoreRow(row.id)) }
                }
                FilmaxPosterCard(
                    title = card.title,
                    posterUrl = card.posterUrl,
                    onClick = { onOpenItem(card.id) },
                    rating = card.rating,
                    meta = card.typeAndYear,
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
    val cards = row.cards
    item(key = row.id.name) {
        TitledRow(title = row.id.title) {
            itemsIndexed(cards, key = { _, card -> card.id }) { index, card ->
                // Хвостовая карточка скомпонована — просим следующую страницу (как у постер-рядов).
                if (index == cards.lastIndex) {
                    LaunchedEffect(cards.size) { onEvent(HomeEvent.LoadMoreRow(row.id)) }
                }
                // Число тайтлов — только точное (превью знает его у подборок в одну страницу).
                FilmaxCollectionCard(
                    title = card.title,
                    hue = collectionHue(card.id),
                    onClick = { onOpenCollection(card.id, card.title) },
                    subtitle = card.titlesCount,
                    posterUrl = card.posters.firstOrNull(),
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
