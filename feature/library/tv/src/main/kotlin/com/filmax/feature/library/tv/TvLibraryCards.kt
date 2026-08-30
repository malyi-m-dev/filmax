package com.filmax.feature.library.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filmax.core.domain.user.model.BookmarkFolder
import com.filmax.core.domain.watching.model.WatchHistory
import com.filmax.core.tv.designsystem.TvAccent
import com.filmax.core.tv.designsystem.TvButton
import com.filmax.core.tv.designsystem.TvFocusCard
import com.filmax.core.tv.designsystem.TvMetrics
import com.filmax.core.tv.designsystem.TvOnSurface
import com.filmax.core.tv.designsystem.TvOnSurfaceVariant
import com.filmax.core.tv.designsystem.TvProgressCard
import com.filmax.core.tv.designsystem.TvScreenFocus
import com.filmax.core.tv.designsystem.TvSurface
import com.filmax.core.tv.designsystem.TvSurfaceContainer
import com.filmax.core.tv.designsystem.TvSurfaceContainerHigh
import com.filmax.core.tv.designsystem.TvSurfaceContainerHighest
import com.filmax.core.ui.components.PosterImage
import com.filmax.core.ui.components.continueMeta
// Карточки и плитки раздела «Моё»: то, из чего сетка складывается. Раскладка сегментов
// и состояние живут в TvLibraryScreen, диалоги закладок — в TvBookmarkDialogs.

@Composable
internal fun ProgressCard(
    entry: WatchHistory,
    returnKey: String,
    focus: TvScreenFocus,
    onOpenItem: (Int) -> Unit,
) {
    TvProgressCard(
        title = entry.title,
        meta = continueMeta(entry.progress),
        // Карточка 16:9 — берём кадр, а не вертикальный постер: тот обрезался бы по центру.
        posterUrl = entry.wideOrPoster,
        progress = entry.progress?.fraction ?: 0f,
        // Карточка ведёт в карточку тайтла, а не сразу в плеер: оттуда «Продолжить · SxEy»
        // играет ту же серию, но остаётся выбор эпизода, сезонов и описание.
        onClick = { onOpenItem(entry.itemId) },
        modifier = focus.item(returnKey),
        posterContent = { url, posterModifier ->
            TvPoster(url, entry.title, posterModifier, TvMetrics.CardShape)
        },
    )
}

/** Плитка папки-закладки. Фокусируется и открывается — содержимое грузит [LibraryScreenModel]. */
@Composable
internal fun FolderTile(folder: BookmarkFolder, onClick: () -> Unit) {
    val count = folder.count
    val word = when {
        count % 100 in 11..14 -> "тайтлов"
        count % 10 == 1 -> "тайтл"
        count % 10 in 2..4 -> "тайтла"
        else -> "тайтлов"
    }
    TvFocusCard(
        onClick = onClick,
        shape = TvMetrics.PanelShape,
        modifier = Modifier.height(FolderTileHeight),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(TvMetrics.PanelShape)
                .background(TvSurfaceContainer)
                .padding(16.dp),
            verticalArrangement = Arrangement.Bottom,
        ) {
            Text(
                folder.title,
                style = MaterialTheme.typography.titleMedium,
                color = TvOnSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "$count $word",
                style = MaterialTheme.typography.bodySmall,
                color = TvOnSurfaceVariant,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}

/** Плитка «＋ Новая папка» — первая ячейка сетки папок, вход в диалог создания. */
@Composable
internal fun NewFolderTile(onClick: () -> Unit) {
    TvFocusCard(
        onClick = onClick,
        shape = TvMetrics.PanelShape,
        modifier = Modifier.height(FolderTileHeight),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(TvMetrics.PanelShape)
                .background(TvSurfaceContainerHigh)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = null,
                tint = TvOnSurface,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text("Новая папка", style = MaterialTheme.typography.titleMedium, color = TvOnSurface)
        }
    }
}

/** Пустое состояние закладок: подсказка и фокусируемая кнопка создания папки. */
@Composable
internal fun BookmarksEmpty(onNewFolder: () -> Unit) {
    val createFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { createFocus.requestFocus() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Folder,
            contentDescription = null,
            tint = TvSurfaceContainerHighest,
            modifier = Modifier.size(36.dp),
        )
        Text("Папок нет", style = MaterialTheme.typography.titleLarge, color = TvOnSurface)
        Text(
            "Создайте папку и собирайте в неё тайтлы",
            style = MaterialTheme.typography.bodyLarge,
            color = TvOnSurfaceVariant,
        )
        TvButton(
            text = "Новая папка",
            onClick = onNewFolder,
            leadingIcon = Icons.Filled.Add,
            focusRequester = createFocus,
        )
    }
}

/** Постер тайтла в папке. В режиме удаления поверх — крестик: маркер, что клик уберёт тайтл. */
@Composable
internal fun FolderPoster(url: String, title: String, modifier: Modifier, removeMode: Boolean) {
    Box(modifier) {
        TvPoster(url, title, Modifier.fillMaxSize(), TvMetrics.PosterShape)
        if (removeMode) {
            RemoveBadgeTv(Modifier.align(Alignment.TopStart).padding(6.dp))
        }
    }
}

/** Круглый крестик поверх постера — маркер режима удаления (слева, чтобы не спорить с рейтингом). */
@Composable
private fun RemoveBadgeTv(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(TvSurface.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.Close,
            contentDescription = null,
            tint = TvOnSurface,
            modifier = Modifier.size(16.dp),
        )
    }
}

/** Постер для слота карточек дизайн-системы: монохромный плейсхолдер вместо розового по умолчанию. */
@Composable
internal fun TvPoster(url: String, title: String, modifier: Modifier, shape: Shape) {
    PosterImage(
        url = url,
        contentDescription = title,
        modifier = modifier,
        shape = shape,
        accentColor = TvSurfaceContainerHighest,
    )
}

@Composable
internal fun LoadingBox(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = TvAccent)
    }
}
