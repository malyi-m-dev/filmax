// Экран деталей — один вертикальный поток и для фильма, и для сериала: кадр с заголовком и
// плитками действий, описание, серии (сериал), актёры, факты и «Похожие»; поверх — стеклянные
// кнопки сверху и плавающая панель «Смотреть» снизу. Здесь — состояние экрана и порядок секций;
// кадр и заголовок — в DetailsBackdrop, секции — в DetailsSections, серии — в DetailsEpisodes,
// шторки описания и папок — в DetailsSheets.

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
import com.filmax.feature.details.common.DetailsState
import com.filmax.feature.details.common.FolderPickerEvent
import com.filmax.feature.details.common.FolderPickerScreenModel
import com.filmax.feature.details.common.FolderPickerState
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/** Действия экрана — группой, чтобы не раздувать списки параметров вложенных секций. */
internal data class DetailsActions(
    val onBack: () -> Unit,
    /** [season] ≤ 0 — фильм/сезон неизвестен; номер видео уникален только внутри сезона. */
    val onPlay: (season: Int, videoId: Int) -> Unit,
    /** То же, но на телевизоре: выбор ТВ в сети и пульт. */
    val onPlayOnTv: (season: Int, videoId: Int) -> Unit,
    val onShare: () -> Unit,
    val onOpenItem: (Int) -> Unit,
    /** Тап по актёру/режиссёру — открыть его фильмографию (isDirector различает запрос к API). */
    val onOpenPerson: (name: String, isDirector: Boolean) -> Unit,
    /** Играть трейлер: прямой HLS-url и заголовок «Трейлер · Название». */
    val onPlayTrailer: (url: String, title: String) -> Unit,
    val onDetails: (DetailsEvent) -> Unit,
    val onFolders: (FolderPickerEvent) -> Unit,
)

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
    val item = state.item

    Box(modifier = modifier.fillMaxSize().background(FilmaxTheme.palette.background)) {
        when {
            state.loading -> CircularProgressIndicator(
                color = FilmaxTheme.palette.accent,
                modifier = Modifier.align(Alignment.Center),
            )

            item != null -> {
                val actions = remember(item.id, navigator, screenModel, folders) {
                    navigator.detailsActions(
                        item = item,
                        onShare = { shareItem(context, item) },
                        onDetails = screenModel::dispatch,
                        onFolders = folders::dispatch,
                    )
                }
                DetailsContent(item = item, state = state, folderState = folderState, actions = actions)
                DetailsSheets(item = item, state = state, folderState = folderState, actions = actions)
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
private fun DetailsContent(item: Item, state: DetailsState, folderState: FolderPickerState, actions: DetailsActions) {
    val glass = rememberGlassState()
    val target = playTarget(item, state.series, state.seasonEpisodes)
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
                    actions = actions,
                )
            }
            detailsSections(item = item, state = state, actions = actions)
        }
        DetailsTopBar(
            title = item.title,
            collapsed = collapsed,
            glass = glass,
            onBack = actions.onBack,
            onShare = actions.onShare,
        )
        DetailsPlayPanel(
            label = playLabel(item, state.series, target),
            onPlay = target?.let { { actions.onPlay(it.season, it.videoId) } },
            onPlayOnTv = target?.let { { actions.onPlayOnTv(it.season, it.videoId) } },
            glass = glass,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/** Куда ведут детали — из [Navigator]: граф про эти переходы больше ничего не знает. */
private fun Navigator.detailsActions(
    item: Item,
    onShare: () -> Unit,
    onDetails: (DetailsEvent) -> Unit,
    onFolders: (FolderPickerEvent) -> Unit,
) = DetailsActions(
    onBack = ::back,
    // Второй аргумент — НОМЕР серии, а не её id: тем же числом kino.pub принимает и отдаёт прогресс.
    onPlay = { season, videoId -> open(Destination.Player(itemId = item.id, videoId = videoId, season = season)) },
    // На ТВ — с прогресса kino.pub: телефон эту серию сейчас не играет.
    onPlayOnTv = { season, videoId ->
        open(Destination.TvPicker(itemId = item.id, videoId = videoId, season = season))
    },
    onShare = onShare,
    onOpenItem = { itemId -> open(Destination.Details(itemId)) },
    onOpenPerson = { name, isDirector -> open(Destination.Filmography(name = name, isDirector = isDirector)) },
    onPlayTrailer = { url, title -> open(Destination.Trailer(url = url, title = title)) },
    onDetails = onDetails,
    onFolders = onFolders,
)

private fun shareItem(context: Context, item: Item) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "${item.title} (${item.year}) — смотри в Filmax")
    }
    runCatching {
        context.startActivity(Intent.createChooser(intent, "Поделиться"))
    }
}

/** Отступ плавающей панели от низа — над системной панелью. */
internal val PanelBottom = 20.dp
