// Мост между черновиком папки (FolderDraft в модели) и редактором дизайн-системы: что рисовать
// и какие события слать. Общий для «Моё» (создание) и экрана папки (переименование).
package com.filmax.feature.library.tv

import com.filmax.core.domain.user.model.FOLDER_TITLE_MAX_LENGTH
import com.filmax.core.presentation.folders.FolderDraft
import com.filmax.core.presentation.folders.FolderInput
import com.filmax.core.presentation.input.KeyboardLayout
import com.filmax.core.presentation.input.keyboardRows
import com.filmax.core.tv.designsystem.TvFolderEditorActions
import com.filmax.core.tv.designsystem.TvFolderEditorState
import com.filmax.core.tv.designsystem.TvKeyboardActions

/** Черновик папки → то, что рисует редактор в боковой панели. */
internal fun FolderDraft.toEditorState(): TvFolderEditorState = TvFolderEditorState(
    heading = if (isRename) "Переименовать" else "Новая папка",
    title = title,
    maxLength = FOLDER_TITLE_MAX_LENGTH,
    tintIndex = tint,
    keyboardRows = keyboardRows(layout),
    layouts = KeyboardLayout.entries.map { it.label },
    layoutIndex = layout.ordinal,
    confirmLabel = if (isRename) "Сохранить" else "Создать",
    canConfirm = canSubmit,
)

/** Нажатия редактора → события модели: черновик меняется только там. */
internal fun folderEditorActions(
    edit: (FolderInput) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
): TvFolderEditorActions = TvFolderEditorActions(
    keyboard = TvKeyboardActions(
        onKey = { key -> edit(FolderInput.Key(key)) },
        onSpace = { edit(FolderInput.Space) },
        onDelete = { edit(FolderInput.Erase) },
    ),
    onTint = { tint -> edit(FolderInput.Tint(tint)) },
    onLayout = { index -> edit(FolderInput.Layout(KeyboardLayout.entries[index])) },
    onConfirm = onConfirm,
    onCancel = onCancel,
)
