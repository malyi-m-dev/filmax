// Из чего складывается сетка раздела «Моё»: карточки с прогрессом, плитки папок, постеры
// внутри папки, пустые состояния сегментов и индикатор загрузки. Какой сегмент что
// показывает — в LibraryScreen.

package com.filmax.feature.library.mobile

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.ShapeButton
import com.filmax.core.designsystem.ShapeCard
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.user.model.BookmarkFolder
import com.filmax.core.domain.watching.model.WatchHistory
import com.filmax.core.ui.components.FilmaxEmptyState
import com.filmax.core.ui.components.FilmaxPosterCard
import com.filmax.core.ui.components.FilmaxProgressCard
import com.filmax.core.ui.components.continueMeta
import com.filmax.core.ui.components.posterMeta
import com.filmax.core.ui.components.ratingLabel
import com.filmax.feature.library.common.LibraryScreenModel

@Composable
internal fun ProgressCard(entry: WatchHistory, onOpenItem: (Int) -> Unit) {
    FilmaxProgressCard(
        title = entry.title,
        meta = continueMeta(entry.progress),
        // Карточка 16:9 — берём кадр серии, а не вертикальный постер: тот обрезался бы по центру.
        posterUrl = entry.wideOrPoster,
        progress = entry.progress?.fraction ?: 0f,
        // В карточку тайтла, а не сразу в плеер: там «Продолжить · SxEy», серии и описание.
        onClick = { onOpenItem(entry.itemId) },
        width = FilmaxMetrics.MineCardWidth,
        height = FilmaxMetrics.MineCardHeight,
    )
}

/**
 * Плитка папки-закладки. Тап открывает содержимое (грузит [LibraryScreenModel]), долгое нажатие —
 * запрос на удаление: отдельной кнопки на плитке нет, чтобы не зашумлять сетку.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun FolderTile(folder: BookmarkFolder, onClick: () -> Unit, onLongClick: () -> Unit) {
    val count = folder.count
    val word = when {
        count % 100 in 11..14 -> "тайтлов"
        count % 10 == 1 -> "тайтл"
        count % 10 in 2..4 -> "тайтла"
        else -> "тайтлов"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(FolderTileHeight)
            .clip(ShapeCard)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.Bottom,
    ) {
        Text(
            folder.title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            "$count $word",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

/** Плитка «＋ Новая папка» — первая ячейка сетки папок и вход в диалог создания. */
@Composable
internal fun NewFolderTile(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(FolderTileHeight)
            .clip(ShapeCard)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Filled.Add,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Новая папка",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * Карточка тайтла внутри папки. Поверх постера — маленький крестик «убрать»: у [FilmaxPosterCard]
 * нет долгого нажатия, а явная иконка заметнее жеста. Крестик слева, чтобы не спорить с пилюлей
 * рейтинга справа.
 */
@Composable
internal fun FolderItemCard(item: Item, onOpen: () -> Unit, onRemove: () -> Unit) {
    Box {
        FilmaxPosterCard(
            title = item.title,
            posterUrl = item.posters.medium.ifBlank { item.posters.small },
            onClick = onOpen,
            width = FilmaxMetrics.GridPosterWidth,
            height = FilmaxMetrics.GridPosterHeight,
            rating = ratingLabel(item.rating.external),
            meta = posterMeta(type = null, year = item.year),
        )
        RemoveBadge(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(5.dp),
        )
    }
}

/** Круглый крестик поверх постера: убирает тайтл из папки (по подтверждению). */
@Composable
private fun RemoveBadge(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.Close,
            contentDescription = "Убрать из папки",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(14.dp),
        )
    }
}

/**
 * Содержимое пустого сегмента. «Открыть каталог» — там, где каталог и есть ответ; [onAction] с
 * [actionLabel] — главное действие сегмента (для «Закладок» это «Новая папка»).
 */
internal data class MineEmptySpec(
    val icon: ImageVector,
    val title: String,
    val hint: String,
    val onOpenCatalog: (() -> Unit)? = null,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
)

/** Пустое состояние занимает всю ширину сетки, а не одну колонку. */
internal fun LazyGridScope.emptyItem(spec: MineEmptySpec) {
    item(key = "empty", span = { GridItemSpan(maxLineSpan) }) { MineEmpty(spec) }
}

@Composable
private fun MineEmpty(spec: MineEmptySpec) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FilmaxEmptyState(icon = spec.icon, title = spec.title, subtitle = spec.hint)
        val onAction = spec.onAction
        val actionLabel = spec.actionLabel
        if (onAction != null && actionLabel != null) {
            Spacer(Modifier.height(18.dp))
            PrimaryActionButton(label = actionLabel, onClick = onAction)
        }
        val onOpenCatalog = spec.onOpenCatalog
        if (onOpenCatalog != null) {
            Spacer(Modifier.height(18.dp))
            OpenCatalogButton(onClick = onOpenCatalog)
        }
    }
}

/** Главная кнопка сегмента: единственная белая заливка на пустом экране (создать папку). */
@Composable
private fun PrimaryActionButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(FilmaxMetrics.PrimaryButtonHeight)
            .clip(ShapeButton)
            .background(MaterialTheme.colorScheme.primary)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

/** Вторичная кнопка: в монохроме белая заливка зарезервирована за главным действием экрана. */
@Composable
private fun OpenCatalogButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(FilmaxMetrics.SecondaryButtonHeight)
            .clip(ShapeButton)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "Открыть каталог",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
internal fun LoadingBox(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

private val FolderTileHeight = 84.dp
