package com.filmax.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxColors
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.FilmaxType
import com.filmax.core.designsystem.ShapeThumb

/**
 * Строка истории: кадр с полосой прогресса, название, что смотрели («S4E7 «Мост»» / «Фильм») и
 * когда («21:10 · на 45%»). [onMore] — «⋮»: убрать тайтл из истории.
 */
// Компонент дизайн-системы: параметры — его публичный API (см. FilmaxButton).
@Suppress("LongParameterList")
@Composable
fun FilmaxHistoryRow(
    title: String,
    subtitle: String?,
    meta: String?,
    imageUrl: String,
    progress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onMore: (() -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = FilmaxMetrics.ScreenPadding, end = 8.dp, top = 6.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HistoryThumb(imageUrl = imageUrl, progress = progress)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                title,
                style = FilmaxType.itemTitle,
                color = FilmaxColors.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            subtitle?.let {
                Text(
                    it,
                    style = FilmaxType.meta,
                    color = FilmaxColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            meta?.let { Text(it, style = FilmaxType.metaSmall, color = FilmaxColors.TextDim, maxLines = 1) }
        }
        onMore?.let {
            FilmaxIconButton(
                Icons.Filled.MoreVert,
                contentDescription = "Ещё",
                onClick = it,
                style = FilmaxIconButtonStyle.Plain,
                tint = FilmaxColors.TextDim,
                size = FilmaxMetrics.IconButtonSmall,
            )
        }
    }
}

/** Кадр строки истории с полосой досмотренного по низу. */
@Composable
private fun HistoryThumb(imageUrl: String, progress: Float) {
    Box(
        Modifier
            .size(width = ThumbWidth, height = ThumbWidth / FRAME_ASPECT)
            .clip(ShapeThumb)
            .background(FilmaxColors.Card),
    ) {
        PosterImage(url = imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), shape = ShapeThumb)
        if (progress > 0f) {
            FilmaxProgressBar(
                progress = progress,
                thickness = FilmaxMetrics.ProgressBarThin,
                modifier = Modifier.align(Alignment.BottomStart),
            )
        }
    }
}

private val ThumbWidth = 120.dp
