package com.filmax.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxColors
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.FilmaxType
import com.filmax.core.domain.user.model.FOLDER_TITLE_MAX_LENGTH

/** Что делают с черновиком папки в редакторе: текст, цвет, сохранить. Передумать — «✕» шторки. */
data class FolderEditorActions(
    val onTitleChange: (String) -> Unit,
    val onTintChange: (Int) -> Unit,
    val onSubmit: () -> Unit,
)

/**
 * Редактор папки в шторке: название с системной клавиатуры (поле в фокусе сразу), цвет кружками и
 * кнопки над клавиатурой. Один на «Новую папку» в деталях, в «Моё» и «Переименовать» в папке —
 * различаются подпись [submitLabel] и [submitIcon].
 */
// Компонент дизайн-системы: параметры — его публичный API (см. FilmaxButton).
@Suppress("LongParameterList")
@Composable
fun FilmaxFolderEditor(
    title: String,
    tint: Int,
    submitLabel: String,
    actions: FolderEditorActions,
    modifier: Modifier = Modifier,
    submitIcon: ImageVector = Icons.Filled.CreateNewFolder,
    canSubmit: Boolean = title.isNotBlank(),
    error: String? = null,
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Column(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = FilmaxMetrics.ScreenPadding)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        FilmaxTextField(
            value = title,
            onValueChange = actions.onTitleChange,
            placeholder = "Например, «На выходные»",
            onDone = { if (canSubmit) actions.onSubmit() },
            label = "Название",
            maxLength = FOLDER_TITLE_MAX_LENGTH,
            focusRequester = focus,
        )
        Text("Цвет", style = FilmaxType.fieldLabel, color = FilmaxColors.TextDim)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FolderHues.forEachIndexed { index, hue ->
                FilmaxColorSwatch(
                    color = folderSwatch(hue),
                    selected = tint == index,
                    onClick = { actions.onTintChange(index) },
                    contentDescription = "Цвет ${index + 1}",
                )
            }
        }
        error?.let { Text(it, style = FilmaxType.meta, color = FilmaxColors.Error) }
        FilmaxButton(
            text = submitLabel,
            onClick = actions.onSubmit,
            icon = submitIcon,
            enabled = canSubmit,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
