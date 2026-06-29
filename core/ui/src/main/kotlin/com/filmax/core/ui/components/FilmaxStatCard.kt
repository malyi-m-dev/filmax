package com.filmax.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Выразительная stat-карточка Filmax — акцентный градиент + произвольная форма.
 *
 * Минимальное использование: `FilmaxStatCard("Рейтинг", "8.6", accent = …)`.
 */
@Composable
fun FilmaxStatCard(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
    sub: String? = null,
    shape: Shape = RoundedCornerShape(20.dp),
) {
    Column(
        modifier = modifier
            .clip(shape)
            .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.2f), accent.copy(alpha = 0.07f))))
            .border(1.dp, accent.copy(alpha = 0.27f), shape)
            .defaultMinSize(minHeight = 94.dp)
            .padding(horizontal = 22.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            label.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = accent,
            letterSpacing = 1.sp,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            value,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
        if (!sub.isNullOrEmpty()) {
            Spacer(Modifier.height(5.dp))
            Text(
                sub,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}
