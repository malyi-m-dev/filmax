// Ряды ленты Главной: «Продолжить», постерные ряды и «Подборки» с догрузкой страниц, плюс
// заголовок ряда. Состав и порядок рядов задаёт модель, ленту собирает HomeScreen.

package com.filmax.feature.home.mobile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.ui.components.FilmaxPosterCard
import com.filmax.core.ui.components.FilmaxProgressCard
import com.filmax.core.ui.components.continueMeta
import com.filmax.core.ui.components.posterUrl
import com.filmax.core.ui.components.ratingLabel
import com.filmax.feature.home.common.HomeEvent
import com.filmax.feature.home.common.HomeRow
import com.filmax.feature.home.common.HomeRowId
import com.filmax.feature.home.common.HomeState

/** Лента — это [HomeState.rows]: экран идёт по ним и рисует, состав и порядок задаёт модель. */
internal fun LazyListScope.homeRows(state: HomeState, actions: HomeActions, onEvent: (HomeEvent) -> Unit) {
    state.rows.forEach { row ->
        if (row.isEmpty) return@forEach
        when (row) {
            is HomeRow.Continue -> continueRow(row, onPlay = actions.onPlay)
            is HomeRow.Titles -> posterRow(row, actions.onOpenItem, onEvent)
            is HomeRow.Collections -> collectionsRow(row, actions.onOpenCollection, onEvent)
        }
    }
}

/**
 * Заголовки рядов живут в экране, а не в модели: у ТВ для того же ряда своя формулировка
 * («Продолжить просмотр» на десяти футах читается лучше, чем «Продолжить»).
 */
private val HomeRowId.title: String
    get() = when (this) {
        HomeRowId.CONTINUE -> "Продолжить"
        HomeRowId.TRENDING -> "В тренде"
        // Заголовок честный: forYou — это топ сериалов по рейтингу, персонализации в фиде нет.
        HomeRowId.FOR_YOU -> "Сериалы с высоким рейтингом"
        HomeRowId.COLLECTIONS -> "Подборки"
    }

private fun LazyListScope.continueRow(
    row: HomeRow.Continue,
    onPlay: (itemId: Int, season: Int, videoId: Int) -> Unit,
) {
    item(key = row.id.name) {
        TitledRow(title = row.id.title) {
            items(row.entries, key = { it.itemId }) { entry ->
                // Ряд продолжения ведёт сразу в плеер, минуя детали: недосмотренный эпизод —
                // videoId+сезон из истории, позицию внутри трека восстановит PlayerScreenModel.
                FilmaxProgressCard(
                    title = entry.title,
                    meta = continueMeta(entry.progress),
                    posterUrl = entry.wideOrPoster,
                    progress = entry.progress?.fraction ?: 0f,
                    onClick = {
                        onPlay(
                            entry.itemId,
                            entry.progress?.season ?: NO_SEASON,
                            entry.progress?.videoId ?: NO_VIDEO_ID,
                        )
                    },
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
    // Отсев пустых ссылок. Полностью «подборки без картинки» он не убирает: kino.pub отдаёт
    // адрес постера всегда, даже когда файла нет (967 → /selection/medium/967.jpg = 404), и
    // такая карточка остаётся градиентной заглушкой с подписью.
    val withPoster = row.paging.items.mapNotNull { collection ->
        collection.posterUrl()?.let { poster -> collection to poster }
    }
    if (withPoster.isEmpty()) return
    item(key = row.id.name) {
        TitledRow(title = row.id.title) {
            itemsIndexed(withPoster, key = { _, entry -> entry.first.id }) { index, (collection, poster) ->
                // Хвостовая карточка скомпонована — просим следующую страницу (как у постер-рядов).
                if (index == withPoster.lastIndex) {
                    LaunchedEffect(withPoster.size) { onEvent(HomeEvent.LoadMoreRow(row.id)) }
                }
                FilmaxPosterCard(
                    title = collection.title,
                    posterUrl = poster,
                    onClick = { onOpenCollection(collection.id, collection.title) },
                )
            }
        }
    }
}

/** Заголовок ряда без стрелки: ряд листается пальцем, а вести ей было некуда. */
@Composable
private fun TitledRow(title: String, content: LazyListScope.() -> Unit) {
    Column(Modifier.padding(top = FilmaxMetrics.RowGap)) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = FilmaxMetrics.ScreenPadding),
        )
        LazyRow(
            modifier = Modifier.padding(top = 12.dp),
            contentPadding = PaddingValues(horizontal = FilmaxMetrics.ScreenPadding),
            horizontalArrangement = Arrangement.spacedBy(FilmaxMetrics.CardGap),
            content = content,
        )
    }
}
