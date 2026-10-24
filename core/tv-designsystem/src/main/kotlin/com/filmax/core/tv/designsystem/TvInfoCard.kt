// Справочная карточка без фокуса: подпись сверху, крупное значение и пояснение — «Пульт с телефона»
// в Профиле. В отличие от карточки подписки — нейтральное стекло без эффектов: её читают, а не любуются.
package com.filmax.core.tv.designsystem

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text

private val CardWidth = 290.dp
private val CardShape = RoundedCornerShape(20.dp)
private val ValueStyle = FxType.rowTitle.copy(fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 26.sp)
private val OverlineStyle = FxType.overline.copy(fontWeight = FontWeight.Bold)
private const val CAPTION_LINES = 2

/**
 * [overline] («ПУЛЬТ С ТЕЛЕФОНА»), крупное [value] («192.168.1.50») и [caption] — что с ним делать.
 * Значение — в одну строку, пояснение — не больше двух: карточку ставят под другими, и по высоте она
 * должна оставаться предсказуемой.
 */
@Composable
fun TvInfoCard(overline: String, value: String, caption: String?, modifier: Modifier = Modifier) {
    val colors = LocalFxColors.current
    Column(
        modifier
            .width(CardWidth)
            .fxGlass(CardShape)
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text(overline.uppercase(), style = OverlineStyle, color = colors.textSecondary)
        Text(
            value,
            style = ValueStyle,
            color = colors.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
        )
        if (caption != null) {
            Text(
                caption,
                style = FxType.meta,
                color = colors.textSecondary,
                maxLines = CAPTION_LINES,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}
