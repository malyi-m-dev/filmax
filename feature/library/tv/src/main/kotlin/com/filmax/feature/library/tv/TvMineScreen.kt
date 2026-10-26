package com.filmax.feature.library.tv

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.filmax.core.navigation.Destination
import com.filmax.core.navigation.Navigator
import com.filmax.core.tv.designsystem.LocalFxColors
import com.filmax.core.tv.designsystem.TvConfirmDialog
import com.filmax.core.tv.designsystem.TvConfirmSpec
import com.filmax.core.tv.designsystem.TvFolderEditor
import com.filmax.core.tv.designsystem.TvFolderEditorActions
import com.filmax.core.tv.designsystem.TvSheetHost
import com.filmax.core.tv.designsystem.rememberTvScreenFocus
import com.filmax.core.tv.designsystem.tvSectionBackground
import com.filmax.feature.library.common.MineEvent
import com.filmax.feature.library.common.MineScreenModel
import com.filmax.feature.library.common.MineSideEffect
import com.filmax.feature.library.common.MineState
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * ТВ-раздел «Моё» (экраны 08, 08a–08d): сегменты «Продолжить · Буду смотреть · Папки · История».
 * Логика, состояние и переходы — в [MineScreenModel]; экран рисует и отдаёт события. Вернулись на
 * экран (из папки, деталей, плеера) — модель перечитывает историю и папки: там могло поменяться всё.
 */
@Composable
fun TvMineScreen(
    modifier: Modifier = Modifier,
    screenModel: MineScreenModel = koinViewModel(),
    navigator: Navigator = koinInject(),
) {
    val state by screenModel.collectAsState()
    screenModel.collectSideEffect { effect ->
        when (effect) {
            is MineSideEffect.OpenDetails -> navigator.open(Destination.Details(effect.itemId))
            is MineSideEffect.OpenFolder -> navigator.open(Destination.BookmarkFolder(effect.folderId))
            MineSideEffect.OpenSearch -> navigator.open(Destination.Search)
            is MineSideEffect.OpenPlayer ->
                navigator.open(Destination.Player(effect.itemId, effect.videoId, effect.season))
        }
    }
    LifecycleResumeEffect(screenModel) {
        screenModel.dispatch(MineEvent.Refresh)
        onPauseOrDispose { }
    }
    MineContent(state = state, onEvent = screenModel::dispatch, modifier = modifier)
}

/**
 * Фокус-цели, к которым он возвращается после панели и диалога: «Создать папку» (закрыли
 * создание) и действие сегмента («Очистить историю»).
 */
internal data class MineFocus(val createFolder: FocusRequester, val segmentAction: FocusRequester)

/** Экран из состояния — без Koin и навигатора: так его собирают UI-тесты фокуса. */
@Composable
internal fun MineContent(state: MineState, onEvent: (MineEvent) -> Unit, modifier: Modifier = Modifier) {
    val focus = remember { MineFocus(createFolder = FocusRequester(), segmentAction = FocusRequester()) }
    Box(modifier.fillMaxSize().tvSectionBackground()) {
        TvSheetHost(
            open = state.draft != null,
            onClose = { onEvent(MineEvent.CancelFolder) },
            returnFocus = focus.createFolder,
            sheet = { state.draft?.let { draft -> TvFolderEditor(draft.toEditorState(), editorActions(onEvent)) } },
        ) {
            if (state.loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = LocalFxColors.current.accent)
                }
            } else {
                MineBody(state, onEvent, focus)
            }
        }
        TvConfirmDialog(
            spec = ClearHistorySpec.takeIf { state.confirmClearHistory },
            onConfirm = { onEvent(MineEvent.ConfirmClearHistory) },
            onDismiss = { onEvent(MineEvent.DismissClearHistory) },
            returnFocus = focus.segmentAction,
        )
    }
}

/**
 * Шапка и сетка — один контейнер фокуса: первый вход — на первую карточку сегмента, а в пустом
 * сегменте — на сами сегменты; возврат из деталей или плеера — на ту карточку, с которой ушли.
 * Память фокуса создаётся после загрузки, когда уже известно, какая карточка первая.
 */
@Composable
private fun MineBody(state: MineState, onEvent: (MineEvent) -> Unit, focus: MineFocus) {
    val screenFocus = rememberTvScreenFocus(startAt = startKey(state))
    Column(Modifier.fillMaxSize().then(screenFocus.containerModifier)) {
        MineHeader(state, onEvent, focus)
        MineSegmentContent(state, onEvent, focus, screenFocus)
    }
}

private val ClearHistorySpec = TvConfirmSpec(
    title = "Очистить историю?",
    message = "Вся история просмотров и места, где вы остановились, удалятся на всех устройствах.",
    confirmLabel = "Очистить",
)

/** Редактор создаёт папку в «Моё»: «Создать» и «Отмена» — события этой модели. */
private fun editorActions(onEvent: (MineEvent) -> Unit): TvFolderEditorActions = folderEditorActions(
    edit = { input -> onEvent(MineEvent.EditFolder(input)) },
    onConfirm = { onEvent(MineEvent.SubmitFolder) },
    onCancel = { onEvent(MineEvent.CancelFolder) },
)
