// Шапка и шторки открытой папки на телефоне: плитка оттенка папки с названием и «Смотреть подряд»,
// «Назад» и «Удалить» поверх ленты, переименование и подтверждение удаления.
package com.filmax.feature.library.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxColors
import com.filmax.core.designsystem.FilmaxType
import com.filmax.core.designsystem.glow
import com.filmax.core.designsystem.oklch
import com.filmax.core.presentation.folders.FolderInput
import com.filmax.core.ui.components.FilmaxButton
import com.filmax.core.ui.components.FilmaxButtonSize
import com.filmax.core.ui.components.FilmaxConfirmSheet
import com.filmax.core.ui.components.FilmaxFolderEditor
import com.filmax.core.ui.components.FilmaxIconButton
import com.filmax.core.ui.components.FilmaxSheet
import com.filmax.core.ui.components.FolderEditorActions
import com.filmax.feature.library.common.FolderEvent
import com.filmax.feature.library.common.FolderState

/** Шапка папки: плитка её цвета со свечением, название, счётчик и действия. */
@Composable
internal fun FolderHeader(state: FolderState, hue: Float, onPlay: (() -> Unit)?, onRename: () -> Unit) {
    val tile = oklch(lightness = 0.62f, chroma = 0.2f, hue = hue)
    Column(Modifier.fillMaxWidth().statusBarsPadding().padding(top = FolderTop, start = 4.dp, bottom = 8.dp)) {
        Box(
            Modifier
                .size(64.dp)
                .glow(tile.copy(alpha = TILE_GLOW_ALPHA), TileShape, blur = 30.dp, spread = 0.dp, offsetY = 8.dp)
                .clip(TileShape)
                .background(tile),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Folder, contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp))
        }
        Text(
            state.folder?.title.orEmpty(),
            style = FilmaxType.headline,
            color = FilmaxColors.Text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 16.dp),
        )
        state.countLabel?.let {
            Text(
                it,
                style = FilmaxType.bodySmall,
                color = FilmaxColors.TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Row(
            Modifier.padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            onPlay?.let {
                FilmaxButton("Смотреть", onClick = it, icon = Icons.Filled.PlayArrow, size = FilmaxButtonSize.Compact)
            }
            FilmaxIconButton(Icons.Filled.Edit, contentDescription = "Переименовать", onClick = onRename)
        }
    }
}

@Composable
internal fun FolderTopBar(onBack: () -> Unit, onDelete: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        FilmaxIconButton(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", onClick = onBack)
        FilmaxIconButton(Icons.Filled.Delete, contentDescription = "Удалить папку", onClick = onDelete)
    }
}

@Composable
internal fun FolderSheets(state: FolderState, onEvent: (FolderEvent) -> Unit) {
    state.draft?.let { draft ->
        val actions = remember(onEvent) {
            FolderEditorActions(
                onTitleChange = { onEvent(FolderEvent.EditFolder(FolderInput.Text(it))) },
                onTintChange = { onEvent(FolderEvent.EditFolder(FolderInput.Tint(it))) },
                onSubmit = { onEvent(FolderEvent.SubmitRename) },
            )
        }
        FilmaxSheet(onDismiss = {
            onEvent(FolderEvent.CancelRename)
        }, title = "Переименовать", subtitle = "Название и цвет — на этом устройстве") {
            FilmaxFolderEditor(
                title = draft.title,
                tint = draft.tint,
                submitLabel = "Сохранить",
                actions = actions,
                submitIcon = Icons.Filled.Edit,
                canSubmit = draft.canSubmit,
            )
        }
    }
    if (state.confirmDelete) {
        FilmaxConfirmSheet(
            title = "Удалить папку?",
            message = "Папка «${state.folder?.title.orEmpty()}» удалится с аккаунта kino.pub. " +
                "Тайтлы в ней останутся в каталоге.",
            confirmLabel = "Удалить",
            onConfirm = { onEvent(FolderEvent.ConfirmDelete) },
            onDismiss = { onEvent(FolderEvent.DismissDelete) },
        )
    }
}

private val FolderTop = 56.dp
private val TileShape = RoundedCornerShape(22.dp)
private const val TILE_GLOW_ALPHA = 0.6f
