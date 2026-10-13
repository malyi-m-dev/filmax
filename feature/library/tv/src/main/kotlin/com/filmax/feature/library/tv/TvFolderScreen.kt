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
import com.filmax.core.domain.user.model.UserFolder
import com.filmax.core.navigation.Destination
import com.filmax.core.navigation.Navigator
import com.filmax.core.tv.designsystem.LocalFxColors
import com.filmax.core.tv.designsystem.TvConfirmDialog
import com.filmax.core.tv.designsystem.TvConfirmSpec
import com.filmax.core.tv.designsystem.TvErrorState
import com.filmax.core.tv.designsystem.TvFolderEditor
import com.filmax.core.tv.designsystem.TvSheetHost
import com.filmax.core.tv.designsystem.rememberTvScreenFocus
import com.filmax.core.tv.designsystem.tvFolderTint
import com.filmax.core.tv.designsystem.tvTintedBackground
import com.filmax.feature.library.common.FolderEvent
import com.filmax.feature.library.common.FolderScreenModel
import com.filmax.feature.library.common.FolderSideEffect
import com.filmax.feature.library.common.FolderState
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/** Шапка цвета папки уходит в фон на 230dp из 540 — до сетки постеров. */
private const val HEADER_TINT_HEIGHT = 230f / 540f

/**
 * Открытая папка (экран 08c): шапка цвета папки, «Смотреть подряд», «Переименовать», «Удалить» и
 * сетка постеров в шесть колонок; долгое «ОК» на постере убирает его из папки. Логика — в
 * [FolderScreenModel]; удалили папку — экран уходит назад в «Моё».
 */
@Composable
fun TvFolderScreen(
    modifier: Modifier = Modifier,
    screenModel: FolderScreenModel = koinViewModel(),
    navigator: Navigator = koinInject(),
) {
    val state by screenModel.collectAsState()
    screenModel.collectSideEffect { effect ->
        when (effect) {
            FolderSideEffect.Deleted -> navigator.back()
        }
    }
    FolderContent(
        state = state,
        actions = FolderActions(
            onEvent = screenModel::dispatch,
            onOpenItem = { itemId -> navigator.open(Destination.Details(itemId)) },
            onPlay = { itemId -> navigator.open(Destination.Player(itemId)) },
            onOpenCatalog = { navigator.open(Destination.Catalog) },
        ),
        modifier = modifier,
    )
}

/** Действия экрана папки: события модели и переходы. */
internal data class FolderActions(
    val onEvent: (FolderEvent) -> Unit,
    val onOpenItem: (Int) -> Unit,
    /** Тайтл целиком с начала: фильм или первая серия сериала (PlayerRoute без сезона и серии). */
    val onPlay: (Int) -> Unit,
    val onOpenCatalog: () -> Unit,
)

/** Куда вернуть фокус после панели переименования и диалога удаления. */
internal data class FolderFocus(val rename: FocusRequester, val delete: FocusRequester)

/** Экран из состояния — без Koin и навигатора: так его собирают UI-тесты фокуса. */
@Composable
internal fun FolderContent(state: FolderState, actions: FolderActions, modifier: Modifier = Modifier) {
    val folder = state.folder
    when {
        // Пока грузится первая страница, шапку не рисуем: фокус встаёт на первый постер, и
        // до его появления ставить его некуда.
        state.loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = LocalFxColors.current.accent)
        }
        folder != null -> OpenFolder(folder, state, actions, modifier)
        // Папки нет в списке: её удалили на другом устройстве или ссылка устарела.
        else -> TvErrorState(
            title = "Папка не найдена",
            message = "Возможно, её удалили на другом устройстве",
            modifier = modifier,
        )
    }
}

@Composable
private fun OpenFolder(folder: UserFolder, state: FolderState, actions: FolderActions, modifier: Modifier) {
    val focus = remember { FolderFocus(rename = FocusRequester(), delete = FocusRequester()) }
    Box(modifier.fillMaxSize().tvTintedBackground(tvFolderTint(folder.tint).card, HEADER_TINT_HEIGHT)) {
        TvSheetHost(
            open = state.draft != null,
            onClose = { actions.onEvent(FolderEvent.CancelRename) },
            returnFocus = focus.rename,
            sheet = { state.draft?.let { draft -> TvFolderEditor(draft.toEditorState(), renameActions(actions)) } },
        ) {
            FolderBody(folder, state, actions, focus)
        }
        TvConfirmDialog(
            spec = deleteSpec(folder).takeIf { state.confirmDelete },
            onConfirm = { actions.onEvent(FolderEvent.ConfirmDelete) },
            onDismiss = { actions.onEvent(FolderEvent.DismissDelete) },
            returnFocus = focus.delete,
        )
    }
}

/**
 * Шапка и сетка — один контейнер фокуса: первый вход — на первый постер, в пустой папке — на
 * первую кнопку экрана.
 */
@Composable
private fun FolderBody(folder: UserFolder, state: FolderState, actions: FolderActions, focus: FolderFocus) {
    val screenFocus = rememberTvScreenFocus(startAt = state.items.firstOrNull()?.let { folderItemKey(it.id) })
    Column(Modifier.fillMaxSize().then(screenFocus.containerModifier)) {
        FolderHeader(folder, state, actions, FolderHeaderFocus(focus, screenFocus))
        FolderGrid(state, actions, screenFocus)
    }
}

private fun deleteSpec(folder: UserFolder) = TvConfirmSpec(
    title = "Удалить папку «${folder.title}»?",
    message = "Папка удалится вместе со списком тайтлов. Сами тайтлы останутся в каталоге.",
    confirmLabel = "Удалить",
)

/** Редактор переименовывает папку: «Сохранить» и «Отмена» — события этой модели. */
private fun renameActions(actions: FolderActions) = folderEditorActions(
    edit = { input -> actions.onEvent(FolderEvent.EditFolder(input)) },
    onConfirm = { actions.onEvent(FolderEvent.SubmitRename) },
    onCancel = { actions.onEvent(FolderEvent.CancelRename) },
)
