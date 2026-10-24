// Элементы транспорта плеера: кнопка паузы, значки перемотки рядом с ней и плашки состояния
// («Субтитры», «Аудио · RU», «1080p») по краям ряда.
package com.filmax.core.tv.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.filmax.core.designsystem.Onest

private val PlayPauseSize = 56.dp
private val PlayPauseGlyph = 32.dp
private val GlyphCircleSize = 40.dp
private val GlyphSize = 24.dp
private val StatusShape = RoundedCornerShape(15.dp)
private val StatusIconSize = 16.dp
private const val GLYPH_FILL_ALPHA = 0.14f
private const val STATUS_FILL_ALPHA = 0.12f

private val StatusStyle = TextStyle(
    fontFamily = Onest,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    lineHeight = 16.sp
)

/**
 * Пауза / воспроизведение — круг 56dp акцентом, главный элемент транспорта. Фокус — общий
 * ([TvFocusSurface]): белая рамка, свечение, увеличение.
 */
@Composable
fun TvPlayPauseButton(playing: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalFxColors.current
    TvFocusSurface(
        onClick = onClick,
        modifier = modifier.size(PlayPauseSize),
        style = TvFocusStyle.Circle,
        colors = TvSurfaceColors(container = colors.accent, content = colors.onAccent),
    ) {
        Icon(
            if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = if (playing) "Пауза" else "Смотреть",
            modifier = Modifier.align(Alignment.Center).size(PlayPauseGlyph),
        )
    }
}

/**
 * Значок рядом с паузой (перемотка на 10 с): круг 40dp стеклом. Без фокуса намеренно — перемотку
 * ведут стрелки пульта, как велит гайдлайн Google для плеера, а не кнопки, до которых надо дойти.
 */
@Composable
fun TvTransportGlyph(icon: ImageVector, contentDescription: String?, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(GlyphCircleSize)
            .background(Color.White.copy(alpha = GLYPH_FILL_ALPHA), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = LocalFxColors.current.text,
            modifier = Modifier.size(GlyphSize)
        )
    }
}

/**
 * Плашка состояния без фокуса: что сейчас включено («Аудио · RU», «1080p»). Выбирают это в нижней
 * панели — плашки лишь подсказывают, что там есть.
 */
@Composable
fun TvStatusChip(label: String, icon: ImageVector, modifier: Modifier = Modifier) {
    val colors = LocalFxColors.current
    Row(
        modifier
            .background(Color.White.copy(alpha = STATUS_FILL_ALPHA), StatusShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, contentDescription = null, tint = colors.text, modifier = Modifier.size(StatusIconSize))
        Text(label, style = StatusStyle, color = colors.text, maxLines = 1)
    }
}
