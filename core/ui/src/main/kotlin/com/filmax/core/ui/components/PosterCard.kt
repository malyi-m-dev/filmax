package com.filmax.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filmax.core.domain.catalog.model.Item

@Composable
fun PosterCard(
    item: Item,
    isFav: Boolean,
    onClick: () -> Unit,
    onFavClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.width(140.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(24.dp))
                .clickable(onClick = onClick),
        ) {
            PosterImage(
                url = item.posters.medium,
                contentDescription = item.title,
                modifier = Modifier.matchParentSize(),
                accentColor = Color(0xFFB4305A),
            )
            // Rating pill
            Box(Modifier.align(Alignment.TopStart).padding(8.dp)) {
                RatingPill(rating = item.rating.filmax.toFloat() / 10f, compact = true)
            }
            // Fav button
            FavButton(
                isFav = isFav,
                onClick = onFavClick,
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text     = item.title,
            style    = MaterialTheme.typography.bodyMedium,
            color    = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text  = "${item.year} · ${item.duration.averageMinutes ?: "??"} мин",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun FavButton(
    isFav: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (isFav) 1.25f else 1f,
        animationSpec = spring(stiffness = 500f, dampingRatio = 0.5f),
        label = "favScale",
    )
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Color(0x80000000))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isFav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = if (isFav) "Убрать из избранного" else "Добавить в избранное",
            tint = if (isFav) Color(0xFFFFB1C8) else Color.White,
            modifier = Modifier.size(16.dp).scale(scale),
        )
    }
}
