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
import com.filmax.feature.library.common.MineState

@Composable
internal fun MineSheets(
    state: MineState,
    confirm: MineConfirm?,
    onEvent: (MineEvent) -> Unit,
    onDismissConfirm: () -> Unit,
) {
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
    confirm?.let { asked ->
        FilmaxConfirmSheet(
            title = "Убрать «${asked.title}»?",
            message = when (asked) {
                is MineConfirm.Watchlist -> "Тайтл пропадёт из «Буду смотреть». Добавить его обратно можно в карточке."
                is MineConfirm.History -> "kino.pub забудет просмотр тайтла целиком — и в истории, и в «Продолжить»."
            },
            confirmLabel = "Убрать",
            onConfirm = {
                onEvent(
                    when (asked) {
                        is MineConfirm.Watchlist -> MineEvent.RemoveFromWatchlist(asked.itemId)
                        is MineConfirm.History -> MineEvent.RemoveFromContinue(asked.itemId)
                    },
                )
            },
            onDismiss = onDismissConfirm,
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
