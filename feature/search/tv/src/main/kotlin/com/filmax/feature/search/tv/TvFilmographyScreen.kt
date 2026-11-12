package com.filmax.feature.search.tv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.filmax.core.navigation.Destination
import com.filmax.core.navigation.Navigator
import com.filmax.core.presentation.cards.TitleCard
import com.filmax.core.tv.designsystem.FilmaxTvTheme
import com.filmax.core.tv.designsystem.TvDimGroup
import com.filmax.core.tv.designsystem.TvMetrics
import com.filmax.core.tv.designsystem.TvOnSurface
import com.filmax.core.tv.designsystem.TvOnSurfaceVariant
import com.filmax.core.tv.designsystem.TvPosterCard
import com.filmax.core.tv.designsystem.TvScreenFocus
import com.filmax.core.tv.designsystem.TvSurfaceContainerHighest
import com.filmax.core.tv.designsystem.preview.TvPreviews
import com.filmax.core.tv.designsystem.rememberTvScreenFocus
import com.filmax.feature.search.common.FilmographySamples
import com.filmax.feature.search.common.FilmographyScreenModel
import com.filmax.feature.search.common.FilmographyState
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/** Сетка постеров: 4×190dp + 3×18dp зазора ложатся между safe area, как в TV-Каталоге. */
private const val GRID_COLUMNS = 4

/**
 * TV-«Фильмография» — сетка работ одного человека. Открывается из деталей по актёру или режиссёру;
 * тип запроса (роли или постановки) уже выбран маршрутом, экран лишь рисует выдачу поверх общего
 * [FilmographyScreenModel] (имя и признак режиссёра берутся из маршрута через SavedStateHandle).
 *
 * Своей кнопки «назад» нет — как и у других push-экранов TV: «Назад» это кнопка пульта, её
 * перехватывает [BackHandler] (тот же приём, что в TV-трейлере).
 */
@Composable
fun TvFilmographyScreen(
    modifier: Modifier = Modifier,
    screenModel: FilmographyScreenModel = koinViewModel(),
    navigator: Navigator = koinInject(),
) {
    val state by screenModel.collectAsState()
    BackHandler { navigator.back() }
    TvFilmographyScreen(
        state = state,
        onOpenItem = { itemId -> navigator.open(Destination.Details(itemId)) },
        modifier = modifier,
    )
}

/** Фильмография из состояния — без Koin и навигатора: её рисуют превью. */
@Composable
internal fun TvFilmographyScreen(state: FilmographyState, onOpenItem: (Int) -> Unit, modifier: Modifier = Modifier) {
    // Фон — у окна (FilmaxTvTheme): своя сплошная заливка экрана — лишний слой цвета на каждом кадре.
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = TvMetrics.SafeVertical),
    ) {
        FilmographyHeading(heading = state.heading)
        FilmographyBody(state = state, onOpenItem = onOpenItem)
    }
}

/** Шапка: имя человека и подпись-раздел. Имя приезжает в состоянии сразу, до ответа сети. */
@Composable
private fun FilmographyHeading(heading: String) {
    Column(Modifier.padding(horizontal = TvMetrics.SafeHorizontal)) {
        Text(
            text = heading,
            style = MaterialTheme.typography.displaySmall,
            color = TvOnSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "Фильмография",
            style = MaterialTheme.typography.bodyMedium,
            color = TvOnSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp, bottom = 18.dp),
        )
    }
}

/** Тело экрана: индикатор загрузки, пустое состояние или сетка постеров. */
@Composable
private fun FilmographyBody(state: FilmographyState, onOpenItem: (Int) -> Unit) {
    when {
        state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = TvOnSurface)
        }

        state.items.isEmpty() -> FilmographyEmpty(title = state.emptyTitle, error = state.error)

        else -> FilmographyGrid(items = state.items, onOpenItem = onOpenItem)
    }
}

@Composable
private fun FilmographyGrid(items: List<TitleCard>, onOpenItem: (Int) -> Unit) {
    val focus = rememberTvScreenFocus()

    // В сетке только постеры — фокус в ней всегда на постере, и соседи приглушаются.
    TvDimGroup { FilmographyPosters(items, focus, onOpenItem) }
}

@Composable
private fun FilmographyPosters(items: List<TitleCard>, focus: TvScreenFocus, onOpenItem: (Int) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(GRID_COLUMNS),
        modifier = Modifier.fillMaxSize().then(focus.containerModifier),
        // Запас сверху/снизу под рамку фокуса крайних карточек — иначе клип сетки её срезает.
        contentPadding = PaddingValues(
            start = TvMetrics.SafeHorizontal,
            end = TvMetrics.SafeHorizontal,
            top = TvMetrics.FocusInset,
            bottom = TvMetrics.SafeVertical + TvMetrics.FocusInset,
        ),
        horizontalArrangement = Arrangement.spacedBy(TvMetrics.CardGap),
        verticalArrangement = Arrangement.spacedBy(TvMetrics.CardGap),
    ) {
        items(items, key = { card -> card.id }) { card ->
            FilmographyPoster(
                card = card,
                modifier = focus.item("filmography:${card.id}"),
                onClick = { onOpenItem(card.id) },
            )
        }
    }
}

@Composable
private fun FilmographyPoster(card: TitleCard, modifier: Modifier, onClick: () -> Unit) {
    TvPosterCard(
        title = card.title,
        meta = card.typeAndYear,
        posterUrl = card.posterUrl,
        onClick = onClick,
        modifier = modifier,
        rating = card.rating,
        ratingHighlighted = card.ratingHigh,
    )
}

/** Сбой сети — не «ничего не найдено»: сетка пуста по обеим причинам, а причины разные. */
@Composable
private fun FilmographyEmpty(title: String, error: String?) {
    Column(
        modifier = Modifier.fillMaxSize().padding(bottom = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = if (error == null) Icons.Filled.SearchOff else Icons.Filled.CloudOff,
            contentDescription = null,
            tint = TvSurfaceContainerHighest,
            modifier = Modifier.size(38.dp),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TvOnSurface,
            modifier = Modifier.padding(top = 12.dp),
        )
        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = TvOnSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

private class FilmographyStatePreviews : PreviewParameterProvider<FilmographyState> {
    override val values = FilmographySamples.all.asSequence()
}

@TvPreviews
@Composable
private fun TvFilmographyScreenPreview(@PreviewParameter(FilmographyStatePreviews::class) state: FilmographyState) {
    FilmaxTvTheme { TvFilmographyScreen(state = state, onOpenItem = {}) }
}
