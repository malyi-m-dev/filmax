// Шторки «Моё»: новая папка (черновик в модели), «Очистить историю?» и «Убрать тайтл?».

package com.filmax.feature.library.mobile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.filmax.core.presentation.folders.FolderDraft
import com.filmax.core.presentation.folders.FolderInput
import com.filmax.core.ui.components.FilmaxConfirmSheet
import com.filmax.core.ui.components.FilmaxFolderEditor
import com.filmax.core.ui.components.FilmaxSheet
import com.filmax.core.ui.components.FolderEditorActions
import com.filmax.feature.library.common.MineEvent
import com.filmax.feature.library.common.MineRemoval
import com.filmax.feature.library.common.MineState

@Composable
internal fun MineSheets(state: MineState, onEvent: (MineEvent) -> Unit) {
    state.draft?.let { draft -> CreateFolderSheet(draft = draft, onEvent = onEvent) }
    if (state.confirmClearHistory) {
        FilmaxConfirmSheet(
            title = "Очистить историю?",
            message = "Вся история просмотров и «Продолжить» удалятся с аккаунта kino.pub — вернуть их будет нельзя.",
            confirmLabel = "Очистить",
            onConfirm = { onEvent(MineEvent.ConfirmClearHistory) },
            onDismiss = { onEvent(MineEvent.DismissClearHistory) },
        )
    }
    state.confirmRemoval?.let { removal ->
        FilmaxConfirmSheet(
            title = "Убрать «${removal.title}»?",
            message = when (removal) {
                is MineRemoval.Watchlist -> "Тайтл пропадёт из «Буду смотреть». Добавить его обратно можно в карточке."
                is MineRemoval.History -> "kino.pub забудет просмотр тайтла целиком — и в истории, и в «Продолжить»."
            },
            confirmLabel = "Убрать",
            onConfirm = { onEvent(MineEvent.ConfirmRemoval) },
            onDismiss = { onEvent(MineEvent.DismissRemoval) },
        )
    }
}

@Composable
private fun CreateFolderSheet(draft: FolderDraft, onEvent: (MineEvent) -> Unit) {
    val actions = remember(onEvent) {
        FolderEditorActions(
            onTitleChange = { onEvent(MineEvent.EditFolder(FolderInput.Text(it))) },
            onTintChange = { onEvent(MineEvent.EditFolder(FolderInput.Tint(it))) },
            onSubmit = { onEvent(MineEvent.SubmitFolder) },
        )
    }
    FilmaxSheet(
        onDismiss = { onEvent(MineEvent.CancelFolder) },
        title = "Новая папка",
        subtitle = "Цвет виден только на этом устройстве"
    ) {
        FilmaxFolderEditor(
            title = draft.title,
            tint = draft.tint,
            submitLabel = "Создать",
            actions = actions,
            canSubmit = draft.canSubmit
        )
    }
}
