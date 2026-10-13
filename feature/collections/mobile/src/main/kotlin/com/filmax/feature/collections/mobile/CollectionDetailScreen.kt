package com.filmax.feature.collections.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.navigation.Destination
import com.filmax.core.navigation.Navigator
import com.filmax.core.ui.components.FilmaxEmptyState
import com.filmax.core.ui.components.FilmaxErrorModal
import com.filmax.core.ui.components.FilmaxPosterCard
import com.filmax.core.ui.components.posterMeta
import com.filmax.core.ui.components.ratingLabel
import com.filmax.core.ui.components.typeLabel
import com.filmax.feature.collections.common.CollectionDetailEvent
import com.filmax.feature.collections.common.CollectionDetailScreenModel
import com.filmax.feature.collections.common.CollectionDetailState
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/** Та же сетка 3×98dp постера, что в каталоге и фильмографии: единая раскладка на кадр 360dp. */
private const val GRID_COLUMNS = 3

/** За сколько карточек до конца сетки просим следующую страницу (три ряда). */
private const val LOAD_MORE_TAIL = 9

/**
 * Одна подборка — сетка её тайтлов. Раскладка повторяет фильмографию: шапка «назад + заголовок»
 * едет вместе с сеткой, под постером — название, тип · год и рейтинг, как во всём приложении.
 */
@Composable
fun CollectionDetailScreen(
    title: String,
    modifier: Modifier = Modifier,
    screenModel: CollectionDetailScreenModel = koinViewModel(),
    navigator: Navigator = koinInject(),
) {
    val state by screenModel.collectAsState()
    val appError by screenModel.collectErrorAsState()

    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        CollectionDetailContent(
            title = title,
            state = state,
            onBack = navigator::back,
            onOpenItem = { itemId -> navigator.open(Destination.Details(itemId)) },
            onLoadMore = { screenModel.dispatch(CollectionDetailEvent.LoadMore) },
        )

        appError?.let { error ->
            FilmaxErrorModal(
                error = error,
                onDismiss = screenModel::dismissError,
                onPrimary = screenModel::retry,
            )
        }
    }
}

@Composable
private fun CollectionDetailContent(
    title: String,
    state: CollectionDetailState,
    onBack: () -> Unit,
    onOpenItem: (Int) -> Unit,
    onLoadMore: () -> Unit,
) {
    val gridState = rememberLazyGridState()
    // Догрузка при подходе к хвосту сетки. derivedStateOf пересчитывается без рекомпозиции;
    // повторные вызовы гасит идемпотентность модели.
    val loadMore by remember {
        derivedStateOf {
            val info = gridState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && lastVisible >= info.totalItemsCount - LOAD_MORE_TAIL
        }
    }
    LaunchedEffect(loadMore, state.page) { if (loadMore) onLoadMore() }

    LazyVerticalGrid(
        columns = GridCells.Fixed(GRID_COLUMNS),
        state = gridState,
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(
            start = FilmaxMetrics.ScreenPadding,
            end = FilmaxMetrics.ScreenPadding,
            top = 6.dp,
            bottom = FilmaxMetrics.ListBottomPadding,
        ),
        horizontalArrangement = Arrangement.spacedBy(FilmaxMetrics.CardGap),
        verticalArrangement = Arrangement.spacedBy(FilmaxMetrics.CardGap),
    ) {
        // Шапка — часть ленты, а не прибитая панель: длинное название подборки в две строки
        // иначе съедало бы постоянную полосу над сеткой.
        item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
            CollectionDetailHeader(title = title, onBack = onBack)
        }
        collectionItems(state = state, onOpenItem = onOpenItem)
        if (state.loadingMore) {
            item(key = "loading-more", span = { GridItemSpan(maxLineSpan) }) { CollectionLoadingMore() }
        }
    }
}

@Composable
private fun CollectionDetailHeader(title: String, onBack: () -> Unit) {
    Column {
        BackButton(onClick = onBack, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(12.dp))
        Text(
            title,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            "Подборка",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        Spacer(Modifier.height(10.dp))
    }
}

/** Тот же монохромный кружок «назад», что в фильмографии: на плоском фоне голая стрелка теряется. */
@Composable
private fun BackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(FilmaxMetrics.BackButtonSize)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Назад",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** Тело сетки: индикатор загрузки, пустое состояние или постеры подборки. */
private fun LazyGridScope.collectionItems(state: CollectionDetailState, onOpenItem: (Int) -> Unit) {
    when {
        state.loading && state.items.isEmpty() ->
            item(key = "loading", span = { GridItemSpan(maxLineSpan) }) { CollectionLoading() }

        state.items.isEmpty() -> item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
            CollectionEmpty(error = state.error)
        }
    }
    items(state.items, key = { it.id }) { item ->
        CollectionPoster(item = item, onClick = { onOpenItem(item.id) })
    }
}

@Composable
private fun CollectionLoadingMore() {
    Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
            strokeWidth = 2.dp,
        )
    }
}

@Composable
private fun CollectionLoading() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

/** Сбой сети — не «подборка пуста»: сетка пуста по обеим причинам, а причины разные. */
@Composable
private fun CollectionEmpty(error: String?) {
    FilmaxEmptyState(
        icon = if (error == null) Icons.Filled.VideoLibrary else Icons.Filled.CloudOff,
        title = if (error == null) "В подборке пока пусто" else "Не удалось загрузить",
        subtitle = error,
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
    )
}

@Composable
private fun CollectionPoster(item: Item, onClick: () -> Unit) {
    FilmaxPosterCard(
        title = item.title,
        posterUrl = item.posters.medium.ifEmpty { item.posters.big },
        onClick = onClick,
        width = FilmaxMetrics.GridPosterWidth,
        height = FilmaxMetrics.GridPosterHeight,
        rating = ratingLabel(item.rating.external),
        meta = posterMeta(typeLabel(item.type), item.year),
    )
}
