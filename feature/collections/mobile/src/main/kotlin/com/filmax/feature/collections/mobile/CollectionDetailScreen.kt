// Подборка: модель и экран из состояния, сетка тайтлов с догрузкой. Шапка её оттенка (обложка, веер
// постеров) и стеклянная полоса, которая встаёт сверху, когда шапка уехала, — в CollectionHeader.

package com.filmax.feature.collections.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.FilmaxTheme
import com.filmax.core.designsystem.glassSource
import com.filmax.core.designsystem.rememberGlassState
import com.filmax.core.domain.error.AppError
import com.filmax.core.navigation.Destination
import com.filmax.core.navigation.Navigator
import com.filmax.core.presentation.cards.TitleCard
import com.filmax.core.ui.components.FilmaxEmptyState
import com.filmax.core.ui.components.FilmaxErrorModal
import com.filmax.core.ui.components.FilmaxPosterCard
import com.filmax.core.ui.components.FilmaxPosterSkeleton
import com.filmax.core.ui.components.collectionHue
import com.filmax.core.ui.preview.PhonePreviews
import com.filmax.feature.collections.common.CollectionDetailEvent
import com.filmax.feature.collections.common.CollectionDetailScreenModel
import com.filmax.feature.collections.common.CollectionDetailState
import com.filmax.feature.collections.common.CollectionSamples
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/** За сколько карточек до конца сетки просим следующую страницу (три ряда). */
private const val LOAD_MORE_TAIL = 9
private const val SKELETON_COUNT = 9

/**
 * Одна подборка — сетка её тайтлов под шапкой её оттенка. «Смотреть подряд» начинает с первого
 * тайтла подборки ([CollectionDetailState.startItemId]). Название и id — из маршрута, через модель.
 */
@Composable
fun CollectionDetailScreen(
    modifier: Modifier = Modifier,
    screenModel: CollectionDetailScreenModel = koinViewModel(),
    navigator: Navigator = koinInject(),
) {
    val state by screenModel.collectAsState()
    val appError by screenModel.collectErrorAsState()
    val actions = remember(screenModel, navigator) { collectionActions(screenModel, navigator) }
    CollectionDetailScreen(state = state, actions = actions, modifier = modifier, error = appError)
}

/** Подборка из состояния — без Koin и навигатора: её рисуют превью. */
@Composable
internal fun CollectionDetailScreen(
    state: CollectionDetailState,
    actions: CollectionActions,
    modifier: Modifier = Modifier,
    error: AppError? = null,
) {
    val hue = collectionHue(state.collectionId)
    val glass = rememberGlassState()
    val gridState = rememberLazyGridState()
    val collapsed by remember { derivedStateOf { gridState.firstVisibleItemIndex > 0 } }

    Box(modifier.fillMaxSize().background(FilmaxTheme.palette.background)) {
        CollectionGrid(
            hue = hue,
            state = state,
            gridState = gridState,
            actions = actions,
            modifier = Modifier.glassSource(glass),
        )
        CollectionTopBar(title = state.title, hue = hue, collapsed = collapsed, glass = glass, onBack = actions.onBack)
        error?.let {
            FilmaxErrorModal(error = it, onDismiss = actions.onDismissError, onPrimary = actions.onRetry)
        }
    }
}

/** Переходы, догрузка и ошибка подборки одним объектом. */
internal data class CollectionActions(
    val onOpenItem: (Int) -> Unit,
    val onPlay: (Int) -> Unit,
    val onLoadMore: () -> Unit,
    val onBack: () -> Unit,
    val onDismissError: () -> Unit,
    val onRetry: () -> Unit,
) {
    companion object {
        /** Ничего не делает — для превью. */
        val None = CollectionActions(
            onOpenItem = {},
            onPlay = {},
            onLoadMore = {},
            onBack = {},
            onDismissError = {},
            onRetry = {},
        )
    }
}

private fun collectionActions(screenModel: CollectionDetailScreenModel, navigator: Navigator) = CollectionActions(
    onOpenItem = { navigator.open(Destination.Details(it)) },
    onPlay = { navigator.open(Destination.Player(itemId = it)) },
    onLoadMore = { screenModel.dispatch(CollectionDetailEvent.LoadMore) },
    onBack = navigator::back,
    onDismissError = screenModel::dismissError,
    onRetry = screenModel::retry,
)

@Composable
private fun CollectionGrid(
    hue: Float,
    state: CollectionDetailState,
    gridState: LazyGridState,
    actions: CollectionActions,
    modifier: Modifier = Modifier,
) {
    // Догрузка при подходе к хвосту сетки; повторные вызовы гасит идемпотентность модели.
    val loadMore by remember {
        derivedStateOf {
            val info = gridState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && lastVisible >= info.totalItemsCount - LOAD_MORE_TAIL
        }
    }
    LaunchedEffect(loadMore, state.page) { if (loadMore) actions.onLoadMore() }
    LazyVerticalGrid(
        columns = GridCells.Fixed(FilmaxMetrics.POSTER_COLUMNS),
        state = gridState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = FilmaxMetrics.GridPadding,
            end = FilmaxMetrics.GridPadding,
            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(FilmaxMetrics.CardGap),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
            val onPlay = state.startItemId?.let { id -> { actions.onPlay(id) } }
            CollectionHeader(hue = hue, state = state, onPlay = onPlay)
        }
        when {
            state.loading && state.items.isEmpty() -> items(
                SKELETON_COUNT,
                key = { "skeleton-$it" }
            ) { FilmaxPosterSkeleton() }
            state.items.isEmpty() -> item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                FilmaxEmptyState(
                    icon = if (state.error == null) Icons.Filled.VideoLibrary else Icons.Filled.CloudOff,
                    title = if (state.error == null) "В подборке пусто" else "Не удалось загрузить",
                    subtitle = state.error,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                )
            }
        }
        items(
            state.items,
            key = { it.id }
        ) { card -> CollectionPoster(card, onClick = { actions.onOpenItem(card.id) }) }
        if (state.loadingMore) items(FilmaxMetrics.POSTER_COLUMNS, key = { "more-$it" }) { FilmaxPosterSkeleton() }
    }
}

@Composable
private fun CollectionPoster(card: TitleCard, onClick: () -> Unit) {
    FilmaxPosterCard(
        title = card.title,
        posterUrl = card.posterUrl,
        onClick = onClick,
        rating = card.rating,
        meta = card.typeAndYear,
    )
}

private class CollectionStatePreviews : PreviewParameterProvider<CollectionDetailState> {
    override val values = CollectionSamples.all.asSequence()
}

@PhonePreviews
@Composable
private fun CollectionDetailScreenPreview(
    @PreviewParameter(CollectionStatePreviews::class) state: CollectionDetailState,
) {
    FilmaxTheme { CollectionDetailScreen(state = state, actions = CollectionActions.None) }
}
