package com.filmax.core.tv.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** База градиента-скрима под названием постера (как c2 в дизайне). */
private val PosterScrim = Color(0xFF141012)

/**
 * Название фильма, «запечённое» в нижнюю часть постера (как в дизайне Filmax TV): тёмный
 * градиент-скрим + крупный uppercase-заголовок и строка `год · жанр`. Кладётся внутрь [Box]
 * поверх постера, чтобы зритель ориентировался не только по обложке.
 *
 * [subtitle] опционален (нет года/жанра — показываем только название).
 */
@Composable
fun BoxScope.TvPosterTitle(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .align(Alignment.BottomStart)
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.55f to PosterScrim.copy(alpha = 0.55f),
                    1f to PosterScrim.copy(alpha = 0.94f),
                )
            )
            .padding(start = 12.dp, end = 12.dp, top = 40.dp, bottom = 12.dp),
    ) {
        Text(
            text = title.uppercase(),
            color = Color.White,
            fontSize = 14.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.2).sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** Собирает строку `год · жанр`, пропуская пустые части. */
fun posterSubtitle(year: Int, genre: String?): String? {
    val parts = buildList {
        if (year > 0) add(year.toString())
        if (!genre.isNullOrBlank()) add(genre)
    }
    return parts.joinToString(" · ").ifBlank { null }
}
