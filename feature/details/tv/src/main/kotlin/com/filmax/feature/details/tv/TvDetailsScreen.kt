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
import com.filmax.feature.details.common.FolderPickerEvent
import com.filmax.feature.details.common.FolderPickerScreenModel
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
    pickerModel: FolderPickerScreenModel = koinViewModel(),
    navigator: Navigator = koinInject(),
) {
    val state by screenModel.collectAsState()
    val picker by pickerModel.collectAsState()
    val folderButtonFocus = remember { FocusRequester() }
    Box(modifier.fillMaxSize().background(LocalFxColors.current.background)) {
        val item = state.item
        when {
            state.loading -> CircularProgressIndicator(
                color = LocalFxColors.current.accent,
                modifier = Modifier.align(Alignment.Center),
            )
            item != null -> DetailsWithSheet(
                state = state,
                item = item,
                actions = detailsActions(item, screenModel, navigator),
                folders = DetailsFolders(picker, pickerModel::dispatch, folderButtonFocus),
            )
        }
    }
}

/**
 * Экран и две боковые панели над ним: полное описание и «В папку». Каждая при закрытии
 * возвращает фокус на то, что её открыло, — потому и хоста два: у каждого своя точка возврата.
 * internal — UI-тест фокуса собирает экран из состояния, без Koin и ScreenModel.
 */
@Composable
internal fun DetailsWithSheet(
    state: DetailsState,
    item: Item,
    actions: DetailsActions,
    folders: DetailsFolders = DetailsFolders.Unused,
) {
    TvSheetHost(
        open = folders.state.open,
        onClose = { folders.onEvent(FolderPickerEvent.Close) },
        returnFocus = folders.buttonFocus,
        sheet = { FolderPickerSheet(item, folders, WatchlistMark(state.isFav, actions.onToggleWatchLater)) },
    ) {
        DescriptionSheet(state, item, actions, folders)
    }
}

@Composable
private fun DescriptionSheet(state: DetailsState, item: Item, actions: DetailsActions, folders: DetailsFolders) {
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
        val focus = DetailsSheetFocus(description = descriptionFocus, folders = folders)
        if (state.series != null) {
            TvSeriesDetails(state, item, actions, focus)
        } else {
            TvMovieDetails(state, item, actions, focus)
        }
    }
}

/**
 * Что нужно экрану от панелей: фокус описания (на него возвращается панель текста) и «В папку» —
 * состояние кнопки, её фокус и открытие панели.
 */
internal data class DetailsSheetFocus(val description: FocusRequester, val folders: DetailsFolders)

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
