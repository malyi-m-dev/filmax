package com.filmax.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val StarSize = 40.dp

/**
 * Пять звёзд в ряд для оценки. [rating] — сколько залито (0 — ни одной); тап по звезде зовёт
 * [onRate] с её номером от 1. Залитые — акцентом темы, пустые — приглушённым контуром.
 */
@Composable
fun FilmaxRatingStars(rating: Int, onRate: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (star in 1..STARS) {
            val filled = star <= rating
            IconButton(onClick = { onRate(star) }) {
                Icon(
                    if (filled) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    contentDescription = "$star из $STARS",
                    tint = if (filled) colors.primary else colors.onSurfaceVariant,
                    modifier = Modifier.size(StarSize),
                )
            }
        }
    }
}

private const val STARS = 5
