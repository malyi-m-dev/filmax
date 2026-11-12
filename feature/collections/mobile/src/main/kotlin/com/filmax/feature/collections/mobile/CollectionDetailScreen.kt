// Подборка: шапка её оттенка с размытой обложкой и веером постеров, сетка тайтлов с догрузкой и
// стеклянная полоса, которая встаёт сверху, когда шапка уехала.

package com.filmax.feature.collections.mobile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxColors
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.FilmaxTheme
import com.filmax.core.designsystem.FilmaxType
import com.filmax.core.designsystem.GlassPanel
import com.filmax.core.designsystem.GlassState
import com.filmax.core.designsystem.glassSource
import com.filmax.core.designsystem.oklch
import com.filmax.core.designsystem.rememberGlassState
import com.filmax.core.domain.error.AppError
import com.filmax.core.navigation.Destination
import com.filmax.core.navigation.Navigator
import com.filmax.core.presentation.cards.TitleCard
import com.filmax.core.ui.components.FilmaxButton
import com.filmax.core.ui.components.FilmaxButtonSize
import com.filmax.core.ui.components.FilmaxEmptyState
import com.filmax.core.ui.components.FilmaxErrorModal
import com.filmax.core.ui.components.FilmaxIconButton
import com.filmax.core.ui.components.FilmaxOverline
import com.filmax.core.ui.components.FilmaxPosterCard
import com.filmax.core.ui.components.FilmaxPosterFan
import com.filmax.core.ui.components.FilmaxPosterSkeleton
import com.filmax.core.ui.components.PosterImage
import com.filmax.core.ui.components.bleed
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

/**
 * Шапка: обложка, размытая в цвет фона, поверх — градиент оттенка подборки и веер первых постеров.
 * Обложки нет — остаётся градиент, веер держит шапку сам.
 */
@Composable
private fun CollectionHeader(hue: Float, state: CollectionDetailState, onPlay: (() -> Unit)?) {
    Box(Modifier.fillMaxWidth().bleed(FilmaxMetrics.GridPadding)) {
        CollectionBackdrop(cover = state.cover, hue = hue)
        Column(
            Modifier.fillMaxWidth().statusBarsPadding().padding(top = 64.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            FilmaxPosterFan(
                posters = state.fanPosters,
                centerWidth = 120.dp,
                sideWidth = 104.dp,
                angle = 9f,
                step = 74.dp,
                modifier = Modifier.height(FanHeight),
            )
            CollectionHeading(title = state.title, hue = hue, caption = state.titlesCaption)
            onPlay?.let {
                FilmaxButton(
                    text = "Смотреть подряд",
                    onClick = it,
                    icon = Icons.Filled.PlayArrow,
                    size = FilmaxButtonSize.Compact,
                    modifier = Modifier.padding(top = 14.dp),
                )
            }
        }
    }
}

/** Фон шапки: обложка, размытая до пятна, под градиентом оттенка подборки в фон экрана. */
@Composable
private fun BoxScope.CollectionBackdrop(cover: String?, hue: Float) {
    val background = FilmaxTheme.palette.background
    cover?.let {
        PosterImage(
            url = it,
            contentDescription = null,
            modifier = Modifier.matchParentSize().blur(CoverBlur),
            shape = RectangleShape
        )
    }
    Box(
        Modifier.matchParentSize().background(
            Brush.verticalGradient(
                listOf(
                    oklch(lightness = 0.42f, chroma = 0.18f, hue = hue, alpha = 0.55f),
                    background.copy(alpha = BACKDROP_BOTTOM_ALPHA),
                ),
            ),
        ),
    )
}

/** Надзаголовок оттенком подборки, название и — когда известно точно — число тайтлов. */
@Composable
private fun CollectionHeading(title: String, hue: Float, caption: String?) {
    FilmaxOverline(
        "Подборка редакции",
        color = oklch(lightness = 0.85f, chroma = 0.1f, hue = hue),
        modifier = Modifier.padding(top = 20.dp),
    )
    Text(
        title,
        style = FilmaxType.collectionTitle,
        color = FilmaxColors.Text,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(top = 8.dp, start = 24.dp, end = 24.dp),
    )
    caption?.let {
        Text(
            it,
            style = FilmaxType.meta,
            color = FilmaxColors.TextSecondary,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

/** «Назад» поверх шапки; шапка уехала — стеклянная полоса оттенка подборки с её названием. */
@Composable
private fun CollectionTopBar(title: String, hue: Float, collapsed: Boolean, glass: GlassState, onBack: () -> Unit) {
    Box(Modifier.fillMaxWidth()) {
        AnimatedVisibility(visible = collapsed, enter = fadeIn(), exit = fadeOut()) {
            GlassPanel(state = glass, modifier = Modifier.fillMaxWidth(), border = Color.Transparent) {
                Box(
                    Modifier
                        .matchParentSize()
                        .background(oklch(lightness = 0.42f, chroma = 0.18f, hue = hue, alpha = 0.22f)),
                )
                Row(
                    Modifier.fillMaxWidth().statusBarsPadding().height(
                        FilmaxMetrics.HeaderRowHeight
                    ).padding(start = 68.dp, end = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        title,
                        style = FilmaxType.playerTitle,
                        color = FilmaxColors.Text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        FilmaxIconButton(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Назад",
            onClick = onBack,
            modifier = Modifier.statusBarsPadding().padding(start = 16.dp, top = 8.dp),
        )
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

private val CoverBlur = 40.dp
private val FanHeight = 196.dp
private const val BACKDROP_BOTTOM_ALPHA = 0.96f

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
