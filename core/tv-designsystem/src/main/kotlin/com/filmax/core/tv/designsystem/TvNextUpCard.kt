// Карточка «Далее» поверх кадра в конце серии: что включится и через сколько.
package com.filmax.core.tv.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.filmax.core.ui.components.PosterImage

private val CardWidth = 230.dp
private val CardShape = RoundedCornerShape(16.dp)
private val ThumbWidth = 84.dp
private val ThumbHeight = 48.dp
private val ThumbShape = RoundedCornerShape(8.dp)
private val CountdownHeight = 3.dp
private const val TRACK_ALPHA = 0.2f

/** Подложка карточки — тёмное стекло: она лежит на кадре, и белый текст должен читаться на любом. */
private val CardFill = Color(0xB314121E)

/**
 * «Далее» в конце серии: кадр следующей серии, надзаголовок с отсчётом («Далее · 15 с»), название и
 * полоса отсчёта. [countdown] — доля прошедшего отсчёта 0..1, читается при отрисовке. Кадра нет
 * ([imageUrl] пустой) — карточка без него: заглушка на месте кадра выглядела бы сбоем.
 */
@Composable
fun TvNextUpCard(
    overline: String,
    title: String,
    imageUrl: String,
    countdown: () -> Float,
    modifier: Modifier = Modifier,
) {
    val colors = LocalFxColors.current
    Row(
        modifier
            .width(CardWidth)
            .fxGlass(CardShape, fill = CardFill)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (imageUrl.isNotBlank()) {
            PosterImage(
                url = imageUrl,
                contentDescription = null,
                modifier = Modifier.size(ThumbWidth, ThumbHeight),
                shape = ThumbShape,
                accentColor = TvSurfaceContainerHigh,
            )
        }
        Column(Modifier.weight(1f)) {
            Text(overline.uppercase(), style = FxType.overline, color = colors.accentText, maxLines = 1)
            Text(
                title,
                style = FxType.cardTitle,
                color = colors.text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 3.dp),
            )
            CountdownBar(countdown, Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
private fun CountdownBar(countdown: () -> Float, modifier: Modifier) {
    val accent = LocalFxColors.current.accent
    Box(
        modifier
            .fillMaxWidth()
            .height(CountdownHeight)
            .drawBehind {
                val radius = CornerRadius(size.height / 2)
                drawRoundRect(Color.White.copy(alpha = TRACK_ALPHA), cornerRadius = radius)
                drawRoundRect(
                    accent,
                    size = Size(size.width * countdown().coerceIn(0f, 1f), size.height),
                    cornerRadius = radius,
                )
            },
    )
}
