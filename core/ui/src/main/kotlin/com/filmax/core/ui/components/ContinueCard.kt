package com.filmax.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filmax.core.domain.catalog.model.Item
import kotlin.math.roundToInt

@Composable
fun ContinueCard(
    item: Item,
    progress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val totalMin = item.duration.averageMinutes ?: 120.0
    val remainMin = ((1f - progress) * totalMin).roundToInt()

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick),
    ) {
        ContinueCardThumbnail(item)
        ContinueCardInfo(title = item.title, remainMin = remainMin, progress = progress)
    }
}

/** Обложка карточки с затемняющим градиентом и кнопкой воспроизведения по центру. */
@Composable
private fun ContinueCardThumbnail(item: Item) {
    // Thumbnail with play button overlay
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp),
    ) {
        PosterImage(
            url = item.posters.wide ?: item.posters.medium,
            contentDescription = item.title,
            modifier = Modifier.matchParentSize(),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            accentColor = Color(0xFFB4305A),
        )
        // Bottom gradient
        Box(
            Modifier.matchParentSize().background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Color(0xFF141012).copy(alpha = 0.9f)),
                    startY = 60f,
                )
            )
        )
        // Play button
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.95f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = "Играть",
                tint = Color.Black,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

/** Подпись карточки: заголовок, остаток времени и полоса прогресса. */
@Composable
private fun ContinueCardInfo(title: String, remainMin: Int, progress: Float) {
    // Info
    Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Осталось $remainMin мин",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        // Progress bar
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(4.dp)
                    .background(Color(0xFFB4305A))
            )
        }
    }
}
