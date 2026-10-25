package com.filmax.feature.details.tv

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
import com.filmax.core.tv.designsystem.LocalFxColors
import com.filmax.core.tv.designsystem.TvSheetHost
import com.filmax.core.tv.designsystem.TvTextSheet
import com.filmax.feature.details.common.DetailsEvent
import com.filmax.feature.details.common.DetailsScreenModel
import com.filmax.feature.details.common.DetailsState
import com.filmax.feature.details.common.FolderPickerEvent
import com.filmax.feature.details.common.FolderPickerScreenModel
import org.koin.androidx.compose.koinViewModel

/**
 * ТВ-детали тайтла. Фильм — вертикальная лента поверх кадра на весь экран: шапка с действиями,
 * состав, «Похожие», «О фильме». Сериал — две колонки: информация слева, панель «Эпизоды»
 * справа. Полное описание — в боковой панели поверх экрана, без смены маршрута. Куда ведут
 * кнопки и карточки, решает [DetailsScreenModel]: экран шлёт события.
 */
@Composable
fun TvDetailsScreen(
    modifier: Modifier = Modifier,
    screenModel: DetailsScreenModel = koinViewModel(),
    pickerModel: FolderPickerScreenModel = koinViewModel(),
) {
    val state by screenModel.collectAsState()
    val picker by pickerModel.collectAsState()
    val folderButtonFocus = remember { FocusRequester() }
    // Фон — у окна (FilmaxTvTheme): своя заливка экрана закрашивала бы каждый пиксель лишний раз.
    Box(modifier.fillMaxSize()) {
        val item = state.item
        when {
            state.loading -> CircularProgressIndicator(
                color = LocalFxColors.current.accent,
                modifier = Modifier.align(Alignment.Center),
            )
            item != null -> DetailsWithSheet(
                state = state,
                item = item,
                onEvent = screenModel::dispatch,
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
    onEvent: (DetailsEvent) -> Unit,
    folders: DetailsFolders = DetailsFolders.Unused,
) {
    TvSheetHost(
        open = folders.state.open,
        onClose = { folders.onEvent(FolderPickerEvent.Close) },
        returnFocus = folders.buttonFocus,
        sheet = { FolderPickerSheet(item, folders, WatchlistMark(state.isFav) { onEvent(DetailsEvent.ToggleFav) }) },
    ) {
        DescriptionSheet(state, item, onEvent, folders)
    }
}

@Composable
private fun DescriptionSheet(
    state: DetailsState,
    item: Item,
    onEvent: (DetailsEvent) -> Unit,
    folders: DetailsFolders,
) {
    val descriptionFocus = remember { FocusRequester() }
    TvSheetHost(
        open = state.descriptionOpen,
        onClose = { onEvent(DetailsEvent.CloseDescription) },
        returnFocus = descriptionFocus,
        sheet = {
            TvTextSheet(
                overline = "Описание",
                title = item.title,
                meta = state.heading.sheetMeta,
                text = item.plot,
                onClose = { onEvent(DetailsEvent.CloseDescription) },
            )
        },
    ) {
        val focus = DetailsSheetFocus(description = descriptionFocus, folders = folders)
        if (state.series != null) {
            TvSeriesDetails(state, item, onEvent, focus)
        } else {
            TvMovieDetails(state, item, onEvent, focus)
        }
    }
}

/**
 * Что нужно экрану от панелей: фокус описания (на него возвращается панель текста) и «В папку» —
 * состояние кнопки, её фокус и открытие панели.
 */
internal data class DetailsSheetFocus(val description: FocusRequester, val folders: DetailsFolders)
