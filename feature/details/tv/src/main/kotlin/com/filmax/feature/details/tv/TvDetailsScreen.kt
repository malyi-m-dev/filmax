package com.filmax.feature.details.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.navigation.Destination
import com.filmax.core.navigation.Navigator
import com.filmax.core.tv.designsystem.LocalFxColors
import com.filmax.core.tv.designsystem.TvSheetHost
import com.filmax.core.tv.designsystem.TvTextSheet
import com.filmax.feature.details.common.DetailsEvent
import com.filmax.feature.details.common.DetailsScreenModel
import com.filmax.feature.details.common.DetailsState
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * ТВ-детали тайтла. Фильм — вертикальная лента поверх кадра на весь экран: шапка с действиями,
 * состав, «Похожие», «О фильме». Сериал — две колонки: информация слева, панель «Эпизоды»
 * справа. Полное описание — в боковой панели поверх экрана, без смены маршрута.
 */
@Composable
fun TvDetailsScreen(
    modifier: Modifier = Modifier,
    screenModel: DetailsScreenModel = koinViewModel(),
    navigator: Navigator = koinInject(),
) {
    val state by screenModel.collectAsState()
    Box(modifier.fillMaxSize().background(LocalFxColors.current.background)) {
        val item = state.item
        when {
            state.loading -> CircularProgressIndicator(
                color = LocalFxColors.current.accent,
                modifier = Modifier.align(Alignment.Center),
            )
            item != null -> DetailsWithSheet(state, item, detailsActions(item, screenModel, navigator))
        }
    }
}

/**
 * Экран и боковая панель полного описания над ним; закрытие возвращает фокус на описание.
 * internal — UI-тест фокуса собирает экран из состояния, без Koin и ScreenModel.
 */
@Composable
internal fun DetailsWithSheet(state: DetailsState, item: Item, actions: DetailsActions) {
    val descriptionFocus = remember { FocusRequester() }
    TvSheetHost(
        open = state.descriptionOpen,
        onClose = actions.onCloseDescription,
        returnFocus = descriptionFocus,
        sheet = {
            TvTextSheet(
                overline = "Описание",
                title = item.title,
                meta = metaLine(item, state.series).ifBlank { null },
                text = item.plot,
                onClose = actions.onCloseDescription,
            )
        },
    ) {
        if (state.series != null) {
            TvSeriesDetails(state, item, actions, descriptionFocus)
        } else {
            TvMovieDetails(state, item, actions, descriptionFocus)
        }
    }
}

/** Действия экрана одним объектом — вложенные секции не тащат длинные списки параметров. */
internal data class DetailsActions(
    /** [season] ≤ 0 — фильм или сезон неизвестен; номер серии уникален только внутри сезона. */
    val onPlay: (season: Int, videoId: Int) -> Unit,
    val onToggleWatchLater: () -> Unit,
    /** null — у тайтла нет играбельного трейлера, кнопки нет. */
    val onTrailer: (() -> Unit)?,
    val onOpenItem: (Int) -> Unit,
    val onOpenPerson: (name: String, isDirector: Boolean) -> Unit,
    val onOpenDescription: () -> Unit,
    val onCloseDescription: () -> Unit,
    val onSelectSeason: (Int) -> Unit,
)

private fun detailsActions(item: Item, screenModel: DetailsScreenModel, navigator: Navigator): DetailsActions {
    // Трейлер — только если url играбельный http(s): kino.pub отдаёт прямой HLS.
    val trailerUrl = item.trailer?.url?.takeIf { it.startsWith("http") }
    return DetailsActions(
        onPlay = { season, videoId ->
            navigator.open(Destination.Player(itemId = item.id, videoId = videoId, season = season))
        },
        onToggleWatchLater = { screenModel.dispatch(DetailsEvent.ToggleFav) },
        onTrailer = trailerUrl?.let { url -> { navigator.open(Destination.Trailer(url = url, title = item.title)) } },
        onOpenItem = { id -> navigator.open(Destination.Details(id)) },
        onOpenPerson = { name, isDirector ->
            navigator.open(Destination.Filmography(name = name, isDirector = isDirector))
        },
        onOpenDescription = { screenModel.dispatch(DetailsEvent.OpenDescription) },
        onCloseDescription = { screenModel.dispatch(DetailsEvent.CloseDescription) },
        onSelectSeason = { number -> screenModel.dispatch(DetailsEvent.SelectSeason(number)) },
    )
}
