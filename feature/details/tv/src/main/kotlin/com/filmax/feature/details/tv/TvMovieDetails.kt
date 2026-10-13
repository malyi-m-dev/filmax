// Детали фильма: лента поверх кадра на весь экран — шапка с действиями, состав, «Похожие»,
// «О фильме». Фон темнеет по мере прокрутки, чтобы ряды ниже первого экрана читались.
@file:OptIn(ExperimentalFoundationApi::class)

package com.filmax.feature.details.tv

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.tv.designsystem.FxType
import com.filmax.core.tv.designsystem.LocalFxColors
import com.filmax.core.tv.designsystem.PivotBringIntoViewSpec
import com.filmax.core.tv.designsystem.TvBackHint
import com.filmax.core.tv.designsystem.TvBackdrop
import com.filmax.core.tv.designsystem.TvButton
import com.filmax.core.tv.designsystem.TvButtonStyle
import com.filmax.core.tv.designsystem.TvMetrics
import com.filmax.core.tv.designsystem.TvPosterCard
import com.filmax.core.tv.designsystem.TvRail
import com.filmax.core.tv.designsystem.TvScreenFocus
import com.filmax.core.tv.designsystem.bringIntoViewAsWhole
import com.filmax.core.tv.designsystem.rememberTvScreenFocus
import com.filmax.core.ui.components.isHighRating
import com.filmax.core.ui.components.posterMeta
import com.filmax.core.ui.components.ratingLabel
import com.filmax.core.ui.components.typeLabel
import com.filmax.feature.details.common.DetailsFact
import com.filmax.feature.details.common.DetailsState
import com.filmax.feature.details.common.FolderPickerEvent
import com.filmax.feature.details.common.detailsFacts

/** Ключ кнопки «Смотреть» — стартовая цель фокуса деталей. */
internal const val PLAY_KEY = "details:play"

/** Ряд с фокусом встаёт заголовком на эту высоту от верха экрана. */
private val RowPivot = 78.dp
private val HeadingWidth = 520.dp
private val HeadingTop = 24.dp

/** Фон гаснет к 0.9 за первые 400dp прокрутки. */
private val DimDistance = 400.dp
private const val MAX_DIM = 0.9f

@Composable
internal fun TvMovieDetails(
    state: DetailsState,
    item: Item,
    actions: DetailsActions,
    sheets: DetailsSheetFocus,
) {
    val listState = rememberLazyListState()
    val focus = rememberTvScreenFocus(startAt = PLAY_KEY)
    val density = LocalDensity.current
    val spec = remember(density) { PivotBringIntoViewSpec(with(density) { RowPivot.toPx() }) }
    val dimDistance = with(density) { DimDistance.toPx() }
    Box(Modifier.fillMaxSize()) {
        TvBackdrop(item.posters.wide ?: item.posters.big, dim = { scrolledFraction(listState, dimDistance) * MAX_DIM })
        CompositionLocalProvider(LocalBringIntoViewSpec provides spec) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().then(focus.containerModifier),
                contentPadding = PaddingValues(bottom = TvMetrics.SafeVertical),
                verticalArrangement = Arrangement.spacedBy(TvMetrics.RowGap - TvMetrics.FocusInset),
            ) {
                item(key = "heading") { MovieHeading(state, item, actions, sheets, focus) }
                movieSections(state, item, actions, focus)
            }
        }
    }
}

/** Насколько прокручена лента — 0 у шапки, 1 через [distance] пикселей. */
private fun scrolledFraction(listState: LazyListState, distance: Float): Float =
    if (listState.firstVisibleItemIndex > 0) {
        1f
    } else {
        (listState.firstVisibleItemScrollOffset / distance).coerceIn(
            0f,
            1f
        )
    }

@Composable
private fun MovieHeading(
    state: DetailsState,
    item: Item,
    actions: DetailsActions,
    sheets: DetailsSheetFocus,
    focus: TvScreenFocus,
) {
    Column(
        Modifier.padding(start = TvMetrics.SafeHorizontal, top = HeadingTop).bringIntoViewAsWhole(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        TvBackHint("Назад")
        DetailsHeading(
            item,
            null,
            actions.onOpenDescription,
            sheets.description,
            Modifier.width(HeadingWidth).padding(top = 18.dp)
        )
        MovieButtons(state.isFav, actions, sheets.folders, focus)
    }
}

/** «Смотреть», «Буду смотреть» / «В списке», «В папку», «Трейлер» — если он есть. */
@Composable
private fun MovieButtons(
    inWatchLater: Boolean,
    actions: DetailsActions,
    folders: DetailsFolders,
    focus: TvScreenFocus
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TvButton(
            "Смотреть",
            // Фильм играется целиком, без выбора дорожки: плееру нужны videoId и сезон = -1.
            onClick = { actions.onPlay(NO_SEASON, MOVIE_VIDEO_ID) },
            leadingIcon = Icons.Rounded.PlayArrow,
            modifier = focus.item(PLAY_KEY),
        )
        TvButton(
            if (inWatchLater) "В списке" else "Буду смотреть",
            onClick = actions.onToggleWatchLater,
            style = TvButtonStyle.Secondary,
            leadingIcon = if (inWatchLater) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
        )
        TvButton(
            "В папку",
            onClick = { folders.onEvent(FolderPickerEvent.Open) },
            style = TvButtonStyle.Secondary,
            leadingIcon = folderIcon(folders.state.inAnyFolder),
            focusRequester = folders.buttonFocus,
        )
        actions.onTrailer?.let { onTrailer ->
            TvButton("Трейлер", onClick = onTrailer, style = TvButtonStyle.Secondary, leadingIcon = Icons.Rounded.Movie)
        }
    }
}

/** Секции под шапкой; пустые выпадают целиком. */
private fun LazyListScope.movieSections(
    state: DetailsState,
    item: Item,
    actions: DetailsActions,
    focus: TvScreenFocus,
) {
    castRail(state, item, actions, focus)
    if (state.similar.isNotEmpty()) {
        item(key = "similar") { SimilarRail(state.similar, actions, focus) }
    }
    val facts = detailsFacts(item, null)
    if (facts.isNotEmpty()) item(key = "facts") { FactsGrid(facts) }
}

@Composable
private fun SimilarRail(similar: List<Item>, actions: DetailsActions, focus: TvScreenFocus) {
    TvRail(title = "Похожие", count = similar.size, contentStart = TvMetrics.SafeHorizontal) {
        items(similar, key = { it.id }) { similarItem ->
            TvPosterCard(
                title = similarItem.title,
                meta = posterMeta(typeLabel(similarItem.type), similarItem.year),
                posterUrl = similarItem.posters.medium.ifEmpty { similarItem.posters.big },
                onClick = { actions.onOpenItem(similarItem.id) },
                rating = ratingLabel(similarItem.rating.kinopoisk),
                ratingHighlighted = isHighRating(similarItem.rating.kinopoisk),
                modifier = focus.item("similar:${similarItem.id}"),
            )
        }
    }
}

private const val FACT_COLUMNS = 3

/** «О фильме» — факты в три колонки: подпись мелким надзаголовком, значение под ней. */
@Composable
private fun FactsGrid(facts: List<DetailsFact>) {
    val colors = LocalFxColors.current
    Column(
        Modifier.padding(start = TvMetrics.SafeHorizontal, end = TvMetrics.SectionEnd),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("О фильме", style = FxType.rowTitle, color = colors.text)
        facts.chunked(FACT_COLUMNS).forEach { row ->
            Row {
                row.forEach { fact ->
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(fact.label.uppercase(), style = FxType.overline, color = colors.textDim)
                        Text(fact.value, style = FxType.metaRow, color = colors.text)
                    }
                }
                repeat(FACT_COLUMNS - row.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

/** «Сезона нет» — фильм или сезон неизвестен (PlayerRoute.season = -1). */
internal const val NO_SEASON = -1

/** Фильм играется целиком, без выбора дорожки: плеер ждёт videoId = -1. */
private const val MOVIE_VIDEO_ID = -1
