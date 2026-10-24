package com.filmax.feature.search.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxColors
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.FilmaxTheme
import com.filmax.core.designsystem.FilmaxType
import com.filmax.core.designsystem.radialWash
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.navigation.Destination
import com.filmax.core.navigation.Navigator
import com.filmax.core.ui.components.FilmaxEmptyState
import com.filmax.core.ui.components.FilmaxIconButton
import com.filmax.core.ui.components.FilmaxOverline
import com.filmax.core.ui.components.FilmaxPosterCard
import com.filmax.core.ui.components.FilmaxPosterSkeleton
import com.filmax.core.ui.components.posterMeta
import com.filmax.core.ui.components.ratingLabel
import com.filmax.core.ui.components.typeLabel
import com.filmax.feature.search.common.FilmographyScreenModel
import com.filmax.feature.search.common.FilmographyState
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/** Скелетонов, пока грузятся работы: два ряда. */
private const val SKELETON_COUNT = 6

/**
 * Фильмография человека — сетка его работ. Открывается из деталей по тапу на актёра или
 * режиссёра; тип запроса (роли или постановки) уже выбран маршрутом, экран лишь рисует выдачу.
 */
@Composable
fun FilmographyScreen(
    modifier: Modifier = Modifier,
    screenModel: FilmographyScreenModel = koinViewModel(),
    navigator: Navigator = koinInject(),
) {
    val state by screenModel.collectAsState()
    val palette = FilmaxTheme.palette
    Box(
        modifier
            .fillMaxSize()
            .background(palette.background)
            .radialWash(palette.wash, centerX = 0.3f, centerY = 0f, radiusX = 1f, radiusY = 0.35f),
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(FilmaxMetrics.POSTER_COLUMNS),
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(
                start = FilmaxMetrics.GridPadding,
                end = FilmaxMetrics.GridPadding,
                top = 8.dp,
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(FilmaxMetrics.CardGap),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            // Шапка едет вместе с сеткой: имя — заголовок страницы, а не панели над ней.
            item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
                FilmographyHeader(heading = state.heading, onBack = navigator::back)
            }
            filmographyItems(state = state, onOpenItem = { navigator.open(Destination.Details(it)) })
        }
    }
}

@Composable
private fun FilmographyHeader(heading: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
        FilmaxIconButton(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", onClick = onBack)
        FilmaxOverline("Фильмография", modifier = Modifier.padding(top = 18.dp, start = 4.dp))
        Text(
            heading,
            style = FilmaxType.screenTitle,
            color = FilmaxColors.Text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp, start = 4.dp),
        )
    }
}

/** Тело сетки: скелетон загрузки, пустое состояние или постеры работ. */
private fun LazyGridScope.filmographyItems(state: FilmographyState, onOpenItem: (Int) -> Unit) {
    when {
        state.loading && state.items.isEmpty() -> items(
            SKELETON_COUNT,
            key = { "skeleton-$it" }
        ) { FilmaxPosterSkeleton() }
        state.items.isEmpty() -> item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
            // Сбой сети — не «ничего не найдено»: сетка пуста по обеим причинам, а причины разные.
            FilmaxEmptyState(
                icon = if (state.error == null) Icons.Filled.SearchOff else Icons.Filled.CloudOff,
                title = if (state.error == null) "Ничего не найдено" else "Не удалось загрузить",
                subtitle = state.error,
                modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
            )
        }
    }
    items(state.items, key = { it.id }) { item -> FilmographyPoster(item = item, onClick = { onOpenItem(item.id) }) }
}

@Composable
private fun FilmographyPoster(item: Item, onClick: () -> Unit) {
    FilmaxPosterCard(
        title = item.title,
        posterUrl = item.posters.medium.ifEmpty { item.posters.big },
        onClick = onClick,
        rating = ratingLabel(item.rating.external),
        meta = posterMeta(typeLabel(item.type), item.year),
    )
}
