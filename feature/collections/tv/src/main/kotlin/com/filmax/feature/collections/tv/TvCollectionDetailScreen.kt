package com.filmax.feature.collections.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.navigation.Destination
import com.filmax.core.navigation.Navigator
import com.filmax.core.tv.designsystem.TvMetrics
import com.filmax.core.tv.designsystem.TvPosterCard
import com.filmax.core.tv.designsystem.rememberTvScreenFocus
import com.filmax.core.ui.components.posterMeta
import com.filmax.core.ui.components.ratingLabel
import com.filmax.core.ui.components.typeLabel
import com.filmax.feature.collections.common.CollectionDetailEvent
import com.filmax.feature.collections.common.CollectionDetailScreenModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/** За сколько карточек до конца сетки просим следующую страницу. */
private const val LOAD_MORE_TAIL = 12

/**
 * TV-экран одной подборки: сетка постеров поверх общего [CollectionDetailScreenModel]
 * (itemId берётся из маршрута через SavedStateHandle).
 */
@Composable
fun TvCollectionDetailScreen(
    title: String,
    modifier: Modifier = Modifier,
    screenModel: CollectionDetailScreenModel = koinViewModel(),
    navigator: Navigator = koinInject(),
) {
    val onOpenItem: (Int) -> Unit = { itemId -> navigator.open(Destination.Details(itemId)) }
    val state by screenModel.collectAsState()
    val focus = rememberTvScreenFocus()
    val gridState = rememberLazyGridState()
    // Догрузка при подходе к хвосту сетки; повторные вызовы гасит идемпотентность модели.
    val loadMore by remember {
        derivedStateOf {
            val info = gridState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && lastVisible >= info.totalItemsCount - LOAD_MORE_TAIL
        }
    }
    LaunchedEffect(loadMore, state.page) {
        if (loadMore) screenModel.dispatch(CollectionDetailEvent.LoadMore)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = TvMetrics.ContentTop),
    ) {
        CollectionHeader(title)

        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

            // Колонки считаются от ширины карточки, а не задаются числом: карточка на ТВ одна на
            // всё приложение и имеет фиксированный размер, а фиксированное число колонок
            // растягивало ячейки и рвало ритм сетки.
            else -> LazyVerticalGrid(
                columns = GridCells.FixedSize(TvMetrics.PosterWidth),
                state = gridState,
                modifier = focus.containerModifier,
                horizontalArrangement = Arrangement.spacedBy(TvMetrics.CardGap),
                verticalArrangement = Arrangement.spacedBy(TvMetrics.CardGap),
                // Запас сверху/снизу под рамку фокуса крайних карточек — иначе клип сетки её срезает.
                contentPadding = PaddingValues(
                    start = TvMetrics.SafeHorizontal,
                    end = TvMetrics.SafeHorizontal,
                    top = TvMetrics.FocusInset,
                    bottom = TvMetrics.SafeVertical + TvMetrics.FocusInset,
                ),
            ) {
                items(state.items, key = { item -> item.id }) { item ->
                    CollectionPoster(
                        item = item,
                        modifier = focus.item("collection:${item.id}"),
                        onClick = { onOpenItem(item.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CollectionHeader(title: String) {
    Column(Modifier.padding(horizontal = TvMetrics.SafeHorizontal)) {
        Text(
            title,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            "Подборка",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp, bottom = 20.dp),
        )
    }
}

/** Карточка тайтла — общая для всего ТВ-приложения (ряды Главной, каталог, фильмография). */
@Composable
private fun CollectionPoster(item: Item, modifier: Modifier, onClick: () -> Unit) {
    TvPosterCard(
        title = item.title,
        meta = posterMeta(typeLabel(item.type), item.year),
        posterUrl = item.posters.medium.ifEmpty { item.posters.big },
        onClick = onClick,
        rating = ratingLabel(item.rating.external),
        modifier = modifier,
    )
}
