// Оценка звёздами с пульта: окно отзыва о приложении.
package com.filmax.core.tv.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon

private val StarButtonSize = 56.dp
private val StarGlyph = 32.dp
private val StarGap = 12.dp

/**
 * Пять звёзд в ряд. [rating] — сколько залито (0 — ни одной); «ОК» на звезде зовёт [onRate] с её
 * номером от 1. Звёзды — круглые кнопки с общим фокусом интерфейса; залитые — цветом акцента.
 * [firstStar] — фокус на первую звезду, когда окно открылось. [exitDown] — куда «вниз»: ближайшим
 * по геометрии бывает не то, что под звёздами (в окне отзыва — раскладка клавиатуры справа).
 */
@Composable
fun TvRatingStars(
    rating: Int,
    onRate: (Int) -> Unit,
    modifier: Modifier = Modifier,
    firstStar: FocusRequester? = null,
    exitDown: FocusRequester? = null,
) {
    val colors = LocalFxColors.current
    Row(modifier.tvFocusGroup(), horizontalArrangement = Arrangement.spacedBy(StarGap)) {
        for (star in 1..STARS) {
            val filled = star <= rating
            TvFocusSurface(
                onClick = { onRate(star) },
                modifier = Modifier
                    .size(StarButtonSize)
                    .then(if (star == 1 && firstStar != null) Modifier.focusRequester(firstStar) else Modifier)
                    .then(if (exitDown != null) Modifier.focusProperties { down = exitDown } else Modifier),
                style = TvFocusStyle.Circle,
                colors = TvSurfaceColors(
                    container = colors.surfaceHigh,
                    content = if (filled) colors.accent else colors.textDim,
                ),
            ) {
                Icon(
                    if (filled) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    contentDescription = "$star из $STARS",
                    modifier = Modifier.align(Alignment.Center).size(StarGlyph),
                )
            }
        }
    }
}

private const val STARS = 5
