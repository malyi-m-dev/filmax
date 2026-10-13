package com.filmax.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.ShapeFull

private val ChipHorizontalPadding = 16.dp

/**
 * Чип выбора: сезон, тип тайтла, сегмент раздела. Выбранный — белая заливка с тёмным текстом:
 * в монохроме заливка и есть единственный маркер выбора, галочек и рамок макет не знает.
 */
@Composable
fun FilmaxChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .height(FilmaxMetrics.ChipHeight)
            .clip(ShapeFull)
            .background(if (selected) colors.primary else colors.surfaceContainerHigh)
            // selectable, а не clickable: TalkBack объявляет, какой чип выбран.
            .selectable(selected = selected, onClick = onClick)
            .padding(horizontal = ChipHorizontalPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) colors.onPrimary else colors.onSurfaceVariant,
            maxLines = 1,
        )
    }
}
