// Экран деталей — один вертикальный поток и для фильма, и для сериала: кадр с заголовком и
// плитками действий, описание, серии (сериал), актёры, факты и «Похожие»; поверх — стеклянные
// кнопки сверху и плавающая панель «Смотреть» снизу. Здесь — состояние экрана и порядок секций;
// кадр и заголовок — в DetailsBackdrop, секции — в DetailsSections, серии — в DetailsEpisodes,
// шторки описания и папок — в DetailsSheets. Куда ведут кнопки, решает DetailsScreenModel, а
// переходит экран: модель отвечает эффектом, корень исполняет его навигатором.

package com.filmax.feature.details.mobile

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.FilmaxTheme
import com.filmax.core.designsystem.glassSource
import com.filmax.core.designsystem.rememberGlassState
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.navigation.Destination
import com.filmax.core.navigation.Navigator
import com.filmax.core.ui.components.FilmaxErrorModal
import com.filmax.feature.details.common.DetailsEvent
import com.filmax.feature.details.common.DetailsScreenModel
import com.filmax.feature.details.common.DetailsSideEffect
import com.filmax.feature.details.common.DetailsState
import com.filmax.feature.details.common.FolderPickerEvent
import com.filmax.feature.details.common.FolderPickerScreenModel
import com.filmax.feature.details.common.FolderPickerState
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * Мобильные Детали поверх общих [DetailsScreenModel] и [FolderPickerScreenModel] (itemId берётся
 * из маршрута через SavedStateHandle).
 */
@Composable
fun DetailsScreen(
    modifier: Modifier = Modifier,
    screenModel: DetailsScreenModel = koinViewModel(),
    folders: FolderPickerScreenModel = koinViewModel(),
    navigator: Navigator = koinInject(),
) {
    val state by screenModel.collectAsState()
    val folderState by folders.collectAsState()
    val appError by screenModel.collectErrorAsState()
    val context = LocalContext.current
    screenModel.collectSideEffect { effect ->
        when (effect) {
            DetailsSideEffect.Back -> navigator.back()
            is DetailsSideEffect.OpenDetails -> navigator.open(Destination.Details(effect.itemId))
            is DetailsSideEffect.OpenFilmography ->
                navigator.open(Destination.Filmography(effect.name, effect.isDirector))
            is DetailsSideEffect.OpenPlayer ->
                navigator.open(Destination.Player(effect.itemId, effect.videoId, effect.season))
            is DetailsSideEffect.OpenTvPicker ->
                navigator.open(Destination.TvPicker(effect.itemId, effect.videoId, effect.season))
            is DetailsSideEffect.OpenTrailer -> navigator.open(Destination.Trailer(effect.url, effect.title))
            is DetailsSideEffect.Share -> shareText(context, effect.text)
        }
    }
    val item = state.item

    Box(modifier = modifier.fillMaxSize().background(FilmaxTheme.palette.background)) {
        when {
            state.loading -> CircularProgressIndicator(
                color = FilmaxTheme.palette.accent,
                modifier = Modifier.align(Alignment.Center),
            )

            item != null -> {
                DetailsContent(item, state, folderState, screenModel::dispatch, folders::dispatch)
                DetailsSheets(item, state, folderState, screenModel::dispatch, folders::dispatch)
            }
        }

        appError?.let { error ->
            FilmaxErrorModal(
                error = error,
                onDismiss = screenModel::dismissError,
                onPrimary = screenModel::retry,
            )
        }
    }
}

/**
 * Лента деталей и стекло поверх неё. Лента — ленивая: у сериала в сезоне бывает за двадцать серий,
 * и строить их все разом ради первого экрана незачем.
 */
@Composable
private fun DetailsContent(
    item: Item,
    state: DetailsState,
    folderState: FolderPickerState,
    onEvent: (DetailsEvent) -> Unit,
    onFolders: (FolderPickerEvent) -> Unit,
) {
    val glass = rememberGlassState()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val listState = rememberLazyListState()
    // Стекло под кнопками — когда начало текста ушло под статус-бар, а не когда уехала вся шапка.
    val collapseAfter = with(LocalDensity.current) { FilmaxMetrics.SeriesContentTop.toPx() }
    val collapsed by remember(listState) {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > collapseAfter }
    }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().glassSource(glass),
            // Последняя секция останавливается над плавающей панелью «Смотреть».
            contentPadding = PaddingValues(
                bottom = bottomInset + FilmaxMetrics.ActionPanelHeight + PanelBottom + 24.dp
            ),
        ) {
            item(key = "head") {
                DetailsHead(
                    item = item,
                    state = state,
                    inAnyFolder = folderState.inAnyFolder,
                    onEvent = onEvent,
                    onFolders = onFolders,
                )
            }
            detailsSections(item = item, state = state, onEvent = onEvent)
        }
        DetailsTopBar(title = item.title, collapsed = collapsed, glass = glass, onEvent = onEvent)
        DetailsPlayPanel(
            play = state.play,
            glass = glass,
            onEvent = onEvent,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

private fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    runCatching {
        context.startActivity(Intent.createChooser(intent, "Поделиться"))
    }
}

/** Отступ плавающей панели от низа — над системной панелью. */
internal val PanelBottom = 20.dp
