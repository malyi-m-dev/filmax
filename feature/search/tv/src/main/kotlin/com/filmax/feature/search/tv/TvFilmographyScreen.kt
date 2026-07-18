package com.filmax.feature.search.tv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.tv.designsystem.TvMetrics
import com.filmax.core.tv.designsystem.TvOnSurface
import com.filmax.core.tv.designsystem.TvOnSurfaceVariant
import com.filmax.core.tv.designsystem.TvPosterCard
import com.filmax.core.tv.designsystem.TvSurface
import com.filmax.core.tv.designsystem.TvSurfaceContainer
import com.filmax.core.tv.designsystem.TvSurfaceContainerHighest
import com.filmax.core.tv.designsystem.posterMeta
import com.filmax.core.tv.designsystem.ratingLabel
import com.filmax.core.ui.components.PosterImage
import com.filmax.feature.search.common.FilmographyScreenModel
import com.filmax.feature.search.common.FilmographyState
import org.koin.androidx.compose.koinViewModel

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
    onBack: () -> Unit,
    onOpenItem: (Int) -> Unit,
    modifier: Modifier = Modifier,
    screenModel: FilmographyScreenModel = koinViewModel(),
) {
    val state by screenModel.collectAsState()

    BackHandler { onBack() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TvSurface)
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

        state.items.isEmpty() -> FilmographyEmpty(error = state.error)

        else -> FilmographyGrid(items = state.items, onOpenItem = onOpenItem)
    }
}

@Composable
private fun FilmographyGrid(items: List<Item>, onOpenItem: (Int) -> Unit) {
    val firstCardFocus = remember { FocusRequester() }
    // Стартовый фокус на первой карточке: без него первое нажатие пульта уходит в никуда.
    // runCatching — на первом кадре узел ещё может быть не привязан к FocusRequester.
    LaunchedEffect(Unit) { runCatching { firstCardFocus.requestFocus() } }

    LazyVerticalGrid(
        columns = GridCells.Fixed(GRID_COLUMNS),
        modifier = Modifier.fillMaxSize(),
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
        itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
            FilmographyPoster(
                item = item,
                focusRequester = if (index == 0) firstCardFocus else null,
                onClick = { onOpenItem(item.id) },
            )
        }
    }
}

@Composable
private fun FilmographyPoster(
    item: Item,
    focusRequester: FocusRequester?,
    onClick: () -> Unit,
) {
    TvPosterCard(
        title = item.title,
        meta = posterMeta(itemTypeLabel(item.type), item.year),
        posterUrl = item.posters.medium.ifEmpty { item.posters.big },
        onClick = onClick,
        rating = ratingLabel(item.rating.external),
        focusRequester = focusRequester,
    ) { url, posterModifier ->
        PosterImage(
            url = url,
            contentDescription = item.title,
            modifier = posterModifier,
            shape = TvMetrics.PosterShape,
            // Плейсхолдер-градиент по умолчанию цветной; в монохроме под постером — поверхность.
            accentColor = TvSurfaceContainer,
        )
    }
}

/** Сбой сети — не «ничего не найдено»: сетка пуста по обеим причинам, а причины разные. */
@Composable
private fun FilmographyEmpty(error: String?) {
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
            text = if (error == null) "Ничего не найдено" else "Не удалось загрузить",
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

/** Подпись под карточкой: тип по-русски. `serial`/`docuserial` из API зрителю не показываем. */
private fun itemTypeLabel(type: ItemType): String = when (type) {
    ItemType.MOVIE -> "Фильм"
    ItemType.SERIES -> "Сериал"
    ItemType.ANIME -> "Аниме"
    ItemType.DOCUMENTARY -> "Документальный"
    ItemType.TV -> "ТВ"
}
