// Крупная плитка значения: скорость воспроизведения «1.25×» в панели плеера.
package com.filmax.core.tv.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.Text

private val TileHeight = 84.dp
private val TileShape = RoundedCornerShape(16.dp)
private val TileFocus = TvFocusStyle(TileShape, glowRadius = 16.dp, focusedScale = TvMetrics.FocusScale)
private const val IDLE_FILL_ALPHA = 0.06f
private const val FOCUSED_FILL_ALPHA = 0.14f
private const val CAPTION_ALPHA = 0.7f

private val ValueStyle = TextStyle(
    fontFamily = Onest,
    fontWeight = FontWeight.Bold,
    fontSize = 24.sp,
    lineHeight = 28.sp,
    letterSpacing = (-0.01).em,
)
private val CaptionStyle = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp)

/**
 * Плитка значения высотой 84dp: крупное значение и подпись под ним ([caption] — «обычная»).
 * Выбранная — заливкой акцента целиком: из ряда одинаковых плиток выбор должен читаться издалека.
 * Ширину задаёт ряд (обычно поровну через weight).
 */
@Composable
fun TvValueTile(
    value: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    caption: String? = null,
) {
    val colors = LocalFxColors.current
    TvFocusSurface(
        onClick = onClick,
        modifier = modifier.height(TileHeight),
        style = TileFocus,
        colors = if (selected) {
            TvSurfaceColors(container = colors.accent, content = colors.onAccent)
        } else {
            TvSurfaceColors(
                container = Color.White.copy(alpha = IDLE_FILL_ALPHA),
                content = colors.text,
                focusedContainer = Color.White.copy(alpha = FOCUSED_FILL_ALPHA),
            )
        },
    ) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, style = ValueStyle, maxLines = 1)
            if (caption != null) {
                Text(
                    caption,
                    style = CaptionStyle,
                    color = LocalContentColor.current.copy(alpha = CAPTION_ALPHA),
                    maxLines = 1
                )
            }
        }
    }
}
