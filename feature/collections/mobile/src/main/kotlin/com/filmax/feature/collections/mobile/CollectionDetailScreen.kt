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
import androidx.compose.foundation.lazy.grid.LazyGridScope
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
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.presentation.labels.posterMeta
import com.filmax.core.presentation.labels.ratingLabel
import com.filmax.core.presentation.labels.titlesLabel
import com.filmax.core.presentation.labels.typeLabel
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
import com.filmax.feature.collections.common.CollectionDetailEvent
import com.filmax.feature.collections.common.CollectionDetailScreenModel
import com.filmax.feature.collections.common.CollectionDetailState
import org.koin.androidx.compose.koinViewModel

/** За сколько карточек до конца сетки просим следующую страницу (три ряда). */
private const val LOAD_MORE_TAIL = 9
private const val SKELETON_COUNT = 9

/**
 * Одна подборка — сетка её тайтлов под шапкой её оттенка. Название и id подборки модель берёт из
 * маршрута, оттенок шапки — дизайн-система по id.
 */
@Composable
fun CollectionDetailScreen(modifier: Modifier = Modifier, screenModel: CollectionDetailScreenModel = koinViewModel()) {
    val state by screenModel.collectAsState()
    val appError by screenModel.collectErrorAsState()
    val onEvent: (CollectionDetailEvent) -> Unit = screenModel::dispatch
    val hue = collectionHue(state.collectionId)
    val glass = rememberGlassState()
    val gridState = rememberLazyGridState()
    // Шапка уехала за верх — её место занимает стеклянная полоса. Решение по прокрутке — дело вёрстки.
    val collapsed by remember { derivedStateOf { gridState.firstVisibleItemIndex > 0 } }

    Box(modifier.fillMaxSize().background(FilmaxTheme.palette.background)) {
        CollectionGrid(
            state = state,
            hue = hue,
            gridState = gridState,
            onEvent = onEvent,
            modifier = Modifier.glassSource(glass),
        )
        CollectionTopBar(title = state.title, hue = hue, collapsed = collapsed, glass = glass, onEvent = onEvent)
        appError?.let { error ->
            FilmaxErrorModal(error = error, onDismiss = screenModel::dismissError, onPrimary = screenModel::retry)
        }
    }
}

/** Сетка подборки: шапка первым элементом, тайтлы, скелетоны и догрузка с хвоста. */
@Composable
private fun CollectionGrid(
    state: CollectionDetailState,
    hue: Float,
    gridState: LazyGridState,
    onEvent: (CollectionDetailEvent) -> Unit,
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
    LaunchedEffect(loadMore, state.page) { if (loadMore) onEvent(CollectionDetailEvent.LoadMore) }
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
            CollectionHeader(state = state, hue = hue, onEvent = onEvent)
        }
        gridBody(state = state, onEvent = onEvent)
    }
}

/** Тело сетки: скелетон первой загрузки, пустое состояние или постеры и скелетоны догрузки. */
private fun LazyGridScope.gridBody(state: CollectionDetailState, onEvent: (CollectionDetailEvent) -> Unit) {
    when {
        state.loading && state.items.isEmpty() -> items(SKELETON_COUNT, key = { "skeleton-$it" }) {
            FilmaxPosterSkeleton()
        }
        state.items.isEmpty() -> item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
            FilmaxEmptyState(
                icon = if (state.error == null) Icons.Filled.VideoLibrary else Icons.Filled.CloudOff,
                title = if (state.error == null) "В подборке пусто" else "Не удалось загрузить",
                subtitle = state.error,
                modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
            )
        }
    }
    items(state.items, key = { it.id }) { item ->
        CollectionPoster(item, onClick = { onEvent(CollectionDetailEvent.OpenItem(item.id)) })
    }
    if (state.loadingMore) items(FilmaxMetrics.POSTER_COLUMNS, key = { "more-$it" }) { FilmaxPosterSkeleton() }
}

/**
 * Шапка: обложка, размытая в цвет фона, поверх — градиент оттенка подборки и веер первых постеров.
 * Обложки нет — остаётся градиент, веер держит шапку сам. Кнопки нет, пока смотреть нечего.
 */
@Composable
private fun CollectionHeader(state: CollectionDetailState, hue: Float, onEvent: (CollectionDetailEvent) -> Unit) {
    Box(Modifier.fillMaxWidth().bleed(FilmaxMetrics.GridPadding)) {
        CollectionBackdrop(cover = state.cover, hue = hue)
        Column(
            Modifier.fillMaxWidth().statusBarsPadding().padding(top = 64.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            FilmaxPosterFan(
                posters = state.items.map { it.posters.medium },
                centerWidth = 120.dp,
                sideWidth = 104.dp,
                angle = 9f,
                step = 74.dp,
                modifier = Modifier.height(FanHeight),
            )
            CollectionHeading(title = state.title, hue = hue, count = state.titlesCount)
            if (state.startItemId != null) {
                FilmaxButton(
                    text = "Смотреть подряд",
                    onClick = { onEvent(CollectionDetailEvent.PlayAll) },
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
            shape = RectangleShape,
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
private fun CollectionHeading(title: String, hue: Float, count: Int?) {
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
    count?.let {
        Text(
            titlesLabel(it),
            style = FilmaxType.meta,
            color = FilmaxColors.TextSecondary,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

/** «Назад» поверх шапки; шапка уехала — стеклянная полоса оттенка подборки с её названием. */
@Composable
private fun CollectionTopBar(
    title: String,
    hue: Float,
    collapsed: Boolean,
    glass: GlassState,
    onEvent: (CollectionDetailEvent) -> Unit,
) {
    Box(Modifier.fillMaxWidth()) {
        AnimatedVisibility(visible = collapsed, enter = fadeIn(), exit = fadeOut()) {
            GlassPanel(state = glass, modifier = Modifier.fillMaxWidth(), border = Color.Transparent) {
                Box(
                    Modifier
                        .matchParentSize()
                        .background(oklch(lightness = 0.42f, chroma = 0.18f, hue = hue, alpha = 0.22f)),
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(FilmaxMetrics.HeaderRowHeight)
                        .padding(start = 68.dp, end = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        title,
                        style = FilmaxType.playerTitle,
                        color = FilmaxColors.Text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        FilmaxIconButton(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Назад",
            onClick = { onEvent(CollectionDetailEvent.Back) },
            modifier = Modifier.statusBarsPadding().padding(start = 16.dp, top = 8.dp),
        )
    }
}

@Composable
private fun CollectionPoster(item: Item, onClick: () -> Unit) {
    FilmaxPosterCard(
        title = item.title,
        posterUrl = item.posters.medium.ifEmpty { item.posters.big },
        onClick = onClick,
        rating = ratingLabel(item.rating.external),
        meta = posterMeta(typeLabel(item.type), item.year),
    )
}

private val CoverBlur = 40.dp
private val FanHeight = 196.dp
private const val BACKDROP_BOTTOM_ALPHA = 0.96f
