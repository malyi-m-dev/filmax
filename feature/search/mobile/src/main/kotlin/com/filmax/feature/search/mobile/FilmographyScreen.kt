package com.filmax.feature.search.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.ui.components.FilmaxEmptyState
import com.filmax.core.ui.components.FilmaxPosterCard
import com.filmax.core.ui.components.posterMeta
import com.filmax.core.ui.components.ratingLabel
import com.filmax.feature.search.common.FilmographyScreenModel
import com.filmax.feature.search.common.FilmographyState
import org.koin.androidx.compose.koinViewModel

/** Та же сетка 3×98dp постера, что и в каталоге: единая раскладка на кадр 360dp. */
private const val GRID_COLUMNS = 3

/**
 * Фильмография человека — сетка его работ. Открывается из деталей по тапу на актёра или
 * режиссёра; тип запроса (роли или постановки) уже выбран маршрутом, экран лишь рисует выдачу.
 */
@Composable
fun FilmographyScreen(
    onBack: () -> Unit,
    onOpenItem: (Int) -> Unit,
    modifier: Modifier = Modifier,
    screenModel: FilmographyScreenModel = koinViewModel(),
) {
    val state by screenModel.collectAsState()
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        FilmographyContent(state = state, onBack = onBack, onOpenItem = onOpenItem)
    }
}

@Composable
private fun FilmographyContent(
    state: FilmographyState,
    onBack: () -> Unit,
    onOpenItem: (Int) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(GRID_COLUMNS),
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(
            start = FilmaxMetrics.ScreenPadding,
            end = FilmaxMetrics.ScreenPadding,
            top = 6.dp,
            bottom = FilmaxMetrics.ScreenPadding,
        ),
        horizontalArrangement = Arrangement.spacedBy(FilmaxMetrics.CardGap),
        verticalArrangement = Arrangement.spacedBy(FilmaxMetrics.CardGap),
    ) {
        // Шапка едет вместе с сеткой: отдельной панели «назад + имя» макет фильмографии не знает.
        item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
            FilmographyHeader(heading = state.heading, onBack = onBack)
        }
        filmographyItems(state = state, onOpenItem = onOpenItem)
    }
}

@Composable
private fun FilmographyHeader(heading: String, onBack: () -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackButton(onClick = onBack)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            heading,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(10.dp))
    }
}

/**
 * Кнопка «назад» из деталей, но на плоском фоне: там подложка — полупрозрачный surface поверх
 * кадра, здесь кадра нет, поэтому берём surfaceContainerHigh — тот же монохромный кружок остаётся
 * виден.
 */
@Composable
private fun BackButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
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

/** Тело сетки: индикатор загрузки, пустое состояние или постеры работ. */
private fun LazyGridScope.filmographyItems(state: FilmographyState, onOpenItem: (Int) -> Unit) {
    when {
        state.loading && state.items.isEmpty() ->
            item(key = "loading", span = { GridItemSpan(maxLineSpan) }) { FilmographyLoading() }

        state.items.isEmpty() -> item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
            FilmographyEmpty(error = state.error)
        }
    }
    items(state.items, key = { it.id }) { item ->
        FilmographyPoster(item = item, onClick = { onOpenItem(item.id) })
    }
}

@Composable
private fun FilmographyLoading() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

/** Сбой сети — не «ничего не найдено»: сетка пуста по обеим причинам, а причины разные. */
@Composable
private fun FilmographyEmpty(error: String?) {
    FilmaxEmptyState(
        icon = if (error == null) Icons.Filled.SearchOff else Icons.Filled.CloudOff,
        title = if (error == null) "Ничего не найдено" else "Не удалось загрузить",
        subtitle = error,
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
    )
}

@Composable
private fun FilmographyPoster(item: Item, onClick: () -> Unit) {
    FilmaxPosterCard(
        title = item.title,
        posterUrl = item.posters.medium.ifEmpty { item.posters.big },
        onClick = onClick,
        width = FilmaxMetrics.GridPosterWidth,
        height = FilmaxMetrics.GridPosterHeight,
        rating = ratingLabel(item.rating.external),
        meta = posterMeta(itemTypeLabel(item.type), item.year),
    )
}

/** Подпись под карточкой: тип по-русски. `serial`/`docuserial` из API зрителю не показываем. */
private fun itemTypeLabel(type: ItemType): String = when (type) {
    ItemType.MOVIE -> "Фильм"
    ItemType.SERIES -> "Сериал"
    ItemType.ANIME -> "Аниме"
    ItemType.DOCUMENTARY -> "Документальный"
    ItemType.TV -> "ТВ"
}
