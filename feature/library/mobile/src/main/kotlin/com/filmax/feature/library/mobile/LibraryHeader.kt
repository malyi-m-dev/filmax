// Шапка раздела «Моё»: заголовок, чипы сегментов и строка открытой папки.
// Сетка и состояние экрана — в LibraryScreen.

package com.filmax.feature.library.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.ShapeFull
import com.filmax.core.domain.user.model.BookmarkFolder

/** Заголовок и сегменты. Не скроллятся вместе с сеткой: сегмент всегда должен быть виден. */
@Composable
internal fun MineHeader(segment: MineSegment, onSegment: (MineSegment) -> Unit) {
    Column {
        Text(
            "Моё",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(horizontal = FilmaxMetrics.ScreenPadding)
                .padding(top = 6.dp),
        )
        LazyRow(
            modifier = Modifier.padding(top = 16.dp, bottom = 14.dp),
            contentPadding = PaddingValues(horizontal = FilmaxMetrics.ScreenPadding),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(MineSegment.entries, key = { it.name }) { entry ->
                MineChip(
                    label = entry.label,
                    selected = entry == segment,
                    onClick = { onSegment(entry) },
                )
            }
        }
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
    }
}

/** Чип-сегмент: выбранный — единственная белая заливка на экране, остальные без обводки и фона. */
@Composable
private fun MineChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(FilmaxMetrics.ChipHeight)
            .clip(ShapeFull)
            .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

/** Строка открытой папки: где мы и чем отсюда выйти — жестом «назад» или тапом по этой строке. */
@Composable
internal fun OpenFolderBar(folder: BookmarkFolder, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onBack)
            .padding(horizontal = FilmaxMetrics.ScreenPadding, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "К списку папок",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Text(
            folder.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
