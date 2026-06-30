package com.filmax.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/**
 * Бейдж рейтинга. [rating] — средняя внешняя оценка (IMDb + Кинопоиск) по шкале 0–10;
 * `null` означает отсутствие оценок и отображается как «N/A».
 */
@Composable
fun RatingPill(
    rating: Double?,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val color = when {
        rating == null -> Color(0xFFD5C2C8)
        rating >= 8.5 -> Color(0xFF6AC2B0)
        rating >= 7.5 -> Color(0xFFE8A43A)
        else -> Color(0xFFD5C2C8)
    }
    val padding = if (compact) 4.dp to 6.dp else 6.dp to 10.dp
    val iconSize = if (compact) 11.dp else 13.dp
    val fontSize = if (compact) 11.sp else 12.sp

    Row(
        modifier = modifier
            .background(Color(0x8C000000), RoundedCornerShape(percent = 50))
            .padding(vertical = padding.first, horizontal = padding.second),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(iconSize),
        )
        Spacer(Modifier.width(3.dp))
        Text(
            text = rating?.let { String.format(Locale.US, "%.1f", it) } ?: "N/A",
            color = color,
            fontSize = fontSize,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        )
    }
}
