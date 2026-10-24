package com.filmax.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxColors
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.FilmaxTheme
import com.filmax.core.designsystem.glassFill
import com.filmax.core.designsystem.glow

/** Недоступная кнопка остаётся на месте: ряд пульта не прыгает, когда серии кончились. */
private const val DISABLED_ALPHA = 0.38f

/**
 * Круглая кнопка пульта и плеера: перемотка, серии, воспроизведение. [emphasized] — главная
 * (пауза/воспроизведение): крупнее и залита акцентом. [onClick] = null — действие сейчас недоступно.
 */
@Composable
fun FilmaxRoundButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
) {
    val palette = FilmaxTheme.palette
    val surface = if (emphasized) {
        Modifier.glow(palette.glow, CircleShape).clip(CircleShape).background(palette.accent)
    } else {
        Modifier.glassFill(CircleShape)
    }
    Box(
        modifier = modifier
            .size(if (emphasized) FilmaxMetrics.RoundButtonLargeSize else FilmaxMetrics.RoundButtonSize)
            .alpha(if (onClick == null) DISABLED_ALPHA else 1f)
            .then(surface)
            .clickable(enabled = onClick != null, role = Role.Button) { onClick?.invoke() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = if (emphasized) palette.onAccent else FilmaxColors.Text,
            modifier = Modifier.size(if (emphasized) 36.dp else 26.dp),
        )
    }
}
