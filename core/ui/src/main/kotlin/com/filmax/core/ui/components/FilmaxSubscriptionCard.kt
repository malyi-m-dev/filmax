package com.filmax.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxColors
import com.filmax.core.designsystem.FilmaxTheme
import com.filmax.core.designsystem.FilmaxType
import com.filmax.core.designsystem.angledGradient

/**
 * Карточка подписки в Профиле: градиент двух акцентов палитры, «Filmax Премиум» и срок. Без
 * активной подписки — то же место, но стекло вместо градиента: аккаунт без подписки не праздник.
 */
@Composable
fun FilmaxSubscriptionCard(title: String, subtitle: String?, active: Boolean, modifier: Modifier = Modifier) {
    val palette = FilmaxTheme.palette
    val fill = if (active) {
        Modifier.background(
            angledGradient(
                angle = 135f,
                0f to palette.accent.copy(alpha = ACCENT_ALPHA),
                1f to palette.accent2.copy(alpha = ACCENT2_ALPHA),
            ),
        )
    } else {
        Modifier.background(FilmaxColors.Card)
    }
    Column(
        modifier
            .fillMaxWidth()
            .clip(CardShape)
            .then(fill)
            .border(1.dp, CardBorder, CardShape)
            .padding(18.dp),
    ) {
        Text("ПОДПИСКА", style = FilmaxType.overline, color = Color.White.copy(alpha = 0.8f))
        Text(title, style = FilmaxType.sheetTitle, color = Color.White, modifier = Modifier.padding(top = 6.dp))
        subtitle?.let {
            Text(
                it,
                style = FilmaxType.meta,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

private val CardShape = RoundedCornerShape(26.dp)
private val CardBorder = Color.White.copy(alpha = 0.16f)
private const val ACCENT_ALPHA = 0.45f
private const val ACCENT2_ALPHA = 0.22f
