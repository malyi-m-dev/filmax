// Сообщение поверх кадра: ошибка воспроизведения, «нужна подписка».
package com.filmax.core.tv.designsystem

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text

private val CardShape = RoundedCornerShape(18.dp)
private val CardMaxWidth = 500.dp

/** Тёмное стекло: карточка лежит на кадре, и текст должен читаться на любом. */
private val CardFill = Color(0xD914121E)

/**
 * Карточка-сообщение поверх кадра: заголовок, пояснение и подсказка внизу ([hint] — «Назад — выйти
 * из плеера»). Без кнопок: она объясняет, что происходит, а выход остаётся на «Назад».
 */
@Composable
fun TvMessageCard(title: String, message: String, modifier: Modifier = Modifier, hint: String? = null) {
    val colors = LocalFxColors.current
    Column(
        modifier
            .widthIn(max = CardMaxWidth)
            .fxGlass(CardShape, fill = CardFill)
            .padding(horizontal = 28.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = FxType.rowTitle, color = colors.text, textAlign = TextAlign.Center)
        Text(
            message,
            style = FxType.meta,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        hint?.let {
            Text(it, style = FxType.metaOnImage, color = colors.textDim, modifier = Modifier.padding(top = 14.dp))
        }
    }
}
