// Шторки деталей: полное описание с фактами и «В папку» — список папок с отметками и создание
// новой. Что открыто, решают модели (descriptionOpen, FolderPickerState.open/draft), экран лишь рисует.

package com.filmax.feature.details.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxColors
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.FilmaxTheme
import com.filmax.core.designsystem.FilmaxType
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.presentation.folders.FolderDraft
import com.filmax.core.presentation.folders.FolderInput
import com.filmax.core.ui.components.FilmaxButton
import com.filmax.core.ui.components.FilmaxCheckRow
import com.filmax.core.ui.components.FilmaxFolderEditor
import com.filmax.core.ui.components.FilmaxFolderIcon
import com.filmax.core.ui.components.FilmaxNewFolderRow
import com.filmax.core.ui.components.FilmaxSheet
import com.filmax.core.ui.components.FilmaxSheetScope
import com.filmax.core.ui.components.FolderEditorActions
import com.filmax.core.ui.components.folderHue
import com.filmax.core.ui.components.titlesLabel
import com.filmax.feature.details.common.DetailsEvent
import com.filmax.feature.details.common.DetailsState
import com.filmax.feature.details.common.FolderPickerEvent
import com.filmax.feature.details.common.FolderPickerState
import com.filmax.feature.details.common.detailsFacts

/** Оттенок системной «Буду смотреть» в списке папок — фиолетовый, как у первой папки. */
private const val WATCHLIST_HUE = 290f

@Composable
internal fun DetailsSheets(item: Item, state: DetailsState, folderState: FolderPickerState, actions: DetailsActions) {
    if (state.descriptionOpen) {
        DescriptionSheet(item = item, state = state, onDismiss = { actions.onDetails(DetailsEvent.CloseDescription) })
    }
    if (folderState.open) {
        FolderSheet(item = item, isFav = state.isFav, state = folderState, actions = actions)
    }
}

/** Полное описание: абзацы и под ними факты — то, что в ленте не поместилось. */
@Composable
private fun DescriptionSheet(item: Item, state: DetailsState, onDismiss: () -> Unit) {
    FilmaxSheet(
        onDismiss = onDismiss,
        title = if (state.series == null) "О фильме" else "О сериале",
        subtitle = listOfNotNull(item.title, item.year.takeIf { it > 0 }?.toString()).joinToString(" · "),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
        ) {
            item.plot.split('\n').map { it.trim() }.filter { it.isNotEmpty() }.forEach { paragraph ->
                Text(
                    paragraph,
                    style = FilmaxType.body,
                    color = FilmaxColors.TextSecondary,
                    modifier = Modifier.padding(horizontal = FilmaxMetrics.ScreenPadding).padding(bottom = 12.dp),
                )
            }
            Box(
                Modifier
                    .padding(horizontal = FilmaxMetrics.ScreenPadding, vertical = 6.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(FilmaxColors.CardBorder),
            )
            Facts(title = null, facts = detailsFacts(item, state.series), modifier = Modifier.padding(top = 10.dp))
        }
    }
}

/** «В папку»: список с отметками или редактор новой папки — по черновику в модели. */
@Composable
private fun FolderSheet(item: Item, isFav: Boolean, state: FolderPickerState, actions: DetailsActions) {
    val draft = state.draft
    val marked = state.memberOf.size + if (isFav) 1 else 0
    FilmaxSheet(
        onDismiss = { actions.onFolders(FolderPickerEvent.Close) },
        title = if (draft == null) "Добавить в папку" else "Новая папка",
        subtitle = if (draft == null) "«${item.title}» · отмечено $marked" else "«${item.title}» сразу попадёт в неё",
    ) {
        if (draft == null) {
            FolderList(isFav = isFav, state = state, actions = actions)
        } else {
            FolderEditor(draft = draft, creating = state.creating, error = state.error, onEvent = actions.onFolders)
        }
    }
}

@Composable
private fun FilmaxSheetScope.FolderList(isFav: Boolean, state: FolderPickerState, actions: DetailsActions) {
    val sheet = this
    Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = FolderListHeight)
                .verticalScroll(rememberScrollState()),
        ) {
            FilmaxCheckRow(
                title = "Буду смотреть",
                checked = isFav,
                onClick = { actions.onDetails(DetailsEvent.ToggleFav) },
                subtitle = titlesLabel(state.watchlistCount),
                leading = { FilmaxFolderIcon(hue = WATCHLIST_HUE, icon = Icons.Filled.Bookmark) },
            )
            if (state.loading) {
                Box(Modifier.fillMaxWidth().padding(vertical = 18.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = FilmaxTheme.palette.accent)
                }
            }
            state.folders.forEach { folder ->
                FilmaxCheckRow(
                    title = folder.title,
                    checked = folder.id in state.memberOf,
                    onClick = { actions.onFolders(FolderPickerEvent.Toggle(folder.id)) },
                    subtitle = titlesLabel(folder.count),
                    leading = { FilmaxFolderIcon(hue = folderHue(folder.tint)) },
                )
            }
            FilmaxNewFolderRow(onClick = { actions.onFolders(FolderPickerEvent.StartCreate) })
        }
        state.error?.let { SheetError(it, Modifier.padding(horizontal = FilmaxMetrics.ScreenPadding)) }
        FilmaxButton(
            text = "Готово",
            onClick = sheet::dismiss,
            icon = Icons.Filled.Check,
            modifier = Modifier.fillMaxWidth().padding(horizontal = FilmaxMetrics.ScreenPadding, vertical = 14.dp),
        )
    }
}

/** Новая папка: название, цвет и «Создать и добавить» — тайтл ляжет в неё сразу. */
@Composable
private fun FolderEditor(draft: FolderDraft, creating: Boolean, error: String?, onEvent: (FolderPickerEvent) -> Unit) {
    val actions = remember(onEvent) {
        FolderEditorActions(
            onTitleChange = { onEvent(FolderPickerEvent.Edit(FolderInput.Text(it))) },
            onTintChange = { onEvent(FolderPickerEvent.Edit(FolderInput.Tint(it))) },
            onSubmit = { onEvent(FolderPickerEvent.Submit) },
        )
    }
    FilmaxFolderEditor(
        title = draft.title,
        tint = draft.tint,
        submitLabel = "Создать и добавить",
        actions = actions,
        // Пока папка создаётся, повторное «Создать» завело бы вторую такую же.
        canSubmit = draft.canSubmit && !creating,
        error = error,
    )
}

/** Почему отметка или папка не сохранились — модель уже откатила их, шторка объясняет. */
@Composable
private fun SheetError(message: String, modifier: Modifier = Modifier) {
    Text(message, style = FilmaxType.meta, color = FilmaxColors.Error, modifier = modifier.padding(top = 8.dp))
}

private val FolderListHeight = 420.dp
