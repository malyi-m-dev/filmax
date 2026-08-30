// Диалоги закладок: создание папки, подтверждение удаления папки и тайтла из неё. Вынесены
// из TvLibraryScreen — экрану от них нужен один вызов TvBookmarkDialogHost.
package com.filmax.feature.library.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.filmax.core.tv.designsystem.TvAccent
import com.filmax.core.tv.designsystem.TvButton
import com.filmax.core.tv.designsystem.TvMetrics
import com.filmax.core.tv.designsystem.TvOnSurface
import com.filmax.core.tv.designsystem.TvOnSurfaceVariant
import com.filmax.core.tv.designsystem.TvSurfaceContainer
import com.filmax.core.tv.designsystem.TvSurfaceContainerHigh
import com.filmax.feature.library.common.LibraryEvent

/** Рисует активный диалог закладок и переводит подтверждение в события [LibraryScreenModel]. */
@Composable
internal fun TvBookmarkDialogHost(
    ui: TvBookmarkUi,
    openFolderId: Int?,
    dispatch: (LibraryEvent) -> Unit,
) {
    if (ui.creating) {
        TvCreateFolderDialog(
            onConfirm = { name ->
                dispatch(LibraryEvent.CreateFolder(name))
                ui.creating = false
            },
            onDismiss = { ui.creating = false },
        )
    }
    ui.folderToDelete?.let { folder ->
        TvConfirmDialog(
            title = "Удалить папку?",
            message = "«${folder.title}» и её список исчезнут. Тайтлы останутся в каталоге.",
            confirmLabel = "Удалить",
            onConfirm = {
                dispatch(LibraryEvent.DeleteFolder(folder.id))
                ui.folderToDelete = null
            },
            onDismiss = { ui.folderToDelete = null },
        )
    }
    ui.itemToRemove?.let { item ->
        TvConfirmDialog(
            title = "Убрать из папки?",
            message = "«${item.title}» исчезнет из папки, но останется в каталоге.",
            confirmLabel = "Убрать",
            onConfirm = {
                // openFolderId непустой, пока папка открыта; без него событие не шлём.
                openFolderId?.let { folderId ->
                    dispatch(LibraryEvent.RemoveItemFromFolder(item.id, folderId))
                }
                ui.itemToRemove = null
            },
            onDismiss = { ui.itemToRemove = null },
        )
    }
}

/**
 * Диалог создания папки. Поле берёт фокус сразу — по нажатию OK открывается системная экранная
 * клавиатура телевизора (ввод пультом). Пустое имя модель игнорирует, поэтому кнопку не блокируем.
 */
@Composable
private fun TvCreateFolderDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    val fieldFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { fieldFocus.requestFocus() }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .widthIn(max = DialogMaxWidth)
                .clip(TvMetrics.PanelShape)
                .background(TvSurfaceContainer)
                .padding(28.dp),
        ) {
            Text("Новая папка", style = MaterialTheme.typography.titleLarge, color = TvOnSurface)
            Spacer(Modifier.height(6.dp))
            Text(
                "Введите название пультом",
                style = MaterialTheme.typography.bodyMedium,
                color = TvOnSurfaceVariant,
            )
            Spacer(Modifier.height(18.dp))
            TvFolderNameField(value = name, onValueChange = { name = it }, focusRequester = fieldFocus)
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TvButton(text = "Создать", onClick = { onConfirm(name) })
                TvButton(text = "Отмена", onClick = onDismiss, primary = false)
            }
        }
    }
}

/** Поле имени папки: тёмная плашка с [BasicTextField] и плейсхолдером; системный IME вводит текст. */
@Composable
private fun TvFolderNameField(
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TvMetrics.ButtonShape)
            .background(TvSurfaceContainerHigh)
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        if (value.isEmpty()) {
            Text(
                "Название папки",
                style = MaterialTheme.typography.bodyLarge,
                color = TvOnSurfaceVariant,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = TvOnSurface),
            cursorBrush = SolidColor(TvAccent),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
        )
    }
}

/** Диалог подтверждения деструктива (удалить папку / убрать тайтл). Фокус — на действии. */
@Composable
private fun TvConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val confirmFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { confirmFocus.requestFocus() }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .widthIn(max = DialogMaxWidth)
                .clip(TvMetrics.PanelShape)
                .background(TvSurfaceContainer)
                .padding(28.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = TvOnSurface)
            Spacer(Modifier.height(10.dp))
            Text(message, style = MaterialTheme.typography.bodyLarge, color = TvOnSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TvButton(text = confirmLabel, onClick = onConfirm, focusRequester = confirmFocus)
                TvButton(text = "Отмена", onClick = onDismiss, primary = false)
            }
        }
    }
}
