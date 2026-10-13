// Диалоги закладок: новая папка, подтверждение удаления папки и тайтла из неё. Экрану от
// них нужен один вызов BookmarkDialogHost; состояние диалогов (BookmarkDialogs) — в
// LibraryScreen.

package com.filmax.feature.library.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.ShapeButton
import com.filmax.feature.library.common.LibraryEvent
import com.filmax.feature.library.common.LibraryScreenModel

/** Рисует активный диалог закладок и переводит подтверждение в события [LibraryScreenModel]. */
@Composable
internal fun BookmarkDialogHost(
    dialogs: BookmarkDialogs,
    openFolderId: Int?,
    dispatch: (LibraryEvent) -> Unit,
) {
    if (dialogs.creating) {
        CreateFolderDialog(
            onDismiss = { dialogs.creating = false },
            onConfirm = { name ->
                dispatch(LibraryEvent.CreateFolder(name))
                dialogs.creating = false
            },
        )
    }
    dialogs.folderToDelete?.let { folder ->
        ConfirmActionDialog(
            title = "Удалить папку?",
            message = "«${folder.title}» и её список исчезнут. Сами тайтлы останутся в каталоге.",
            confirmLabel = "Удалить",
            onConfirm = {
                dispatch(LibraryEvent.DeleteFolder(folder.id))
                dialogs.folderToDelete = null
            },
            onDismiss = { dialogs.folderToDelete = null },
        )
    }
    dialogs.itemToRemove?.let { item ->
        ConfirmActionDialog(
            title = "Убрать из папки?",
            message = "«${item.title}» исчезнет из этой папки, но останется в каталоге.",
            confirmLabel = "Убрать",
            onConfirm = {
                // openFolderId непустой, пока папка открыта; на всякий случай не шлём событие без него.
                openFolderId?.let { folderId ->
                    dispatch(LibraryEvent.RemoveItemFromFolder(item.id, folderId))
                }
                dialogs.itemToRemove = null
            },
            onDismiss = { dialogs.itemToRemove = null },
        )
    }
}

/** Диалог ввода имени новой папки. Поле получает фокус сразу — клавиатура открывается без лишнего тапа. */
@Composable
private fun CreateFolderDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) {
                Text("Создать")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
        title = { Text("Новая папка", style = MaterialTheme.typography.titleLarge) },
        text = {
            FolderNameField(value = name, onValueChange = { name = it }, focusRequester = focusRequester)
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
    )
}

/** Поле имени папки в стиле поиска: pill-контейнер с [BasicTextField] и плейсхолдером. */
@Composable
private fun FolderNameField(
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(FilmaxMetrics.SearchFieldHeight)
            .clip(ShapeButton)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    "Название папки",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )
        }
    }
}

/** Диалог подтверждения деструктивного действия (удалить папку / убрать тайтл). */
@Composable
private fun ConfirmActionDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
    )
}
