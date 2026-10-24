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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxColors
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.FilmaxTheme
import com.filmax.core.designsystem.glassFill
import com.filmax.core.designsystem.glow

/**
 * Вид круглой кнопки: стекло поверх кадра и ленты, акцент — микрофон, или без подложки — внутри
 * поля и панели, которые сами уже стеклянные.
 */
enum class FilmaxIconButtonStyle { Glass, Accent, Plain }

/**
 * Круглая кнопка-иконка: «назад», «на ТВ», «поделиться» поверх кадра, действия в шапке раздела.
 * Размер — из метрик ([FilmaxMetrics.IconButtonSize] по умолчанию), иконка — чуть больше половины.
 * [onClick] = null — действие сейчас недоступно: кнопка остаётся на месте, ряд не прыгает.
 */
// Компонент дизайн-системы: параметры — его публичный API (см. FilmaxButton).
@Suppress("LongParameterList")
@Composable
fun FilmaxIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    size: Dp = FilmaxMetrics.IconButtonSize,
    style: FilmaxIconButtonStyle = FilmaxIconButtonStyle.Glass,
    tint: Color = if (style == FilmaxIconButtonStyle.Accent) FilmaxTheme.palette.onAccent else FilmaxColors.Text,
) {
    val palette = FilmaxTheme.palette
    val surface = when (style) {
        FilmaxIconButtonStyle.Glass -> Modifier.glassFill(CircleShape)
        FilmaxIconButtonStyle.Accent ->
            Modifier
                .glow(palette.glow, CircleShape, blur = GlowBlur, spread = 0.dp, offsetY = 6.dp)
                .clip(CircleShape)
                .background(palette.accent)
        FilmaxIconButtonStyle.Plain -> Modifier.clip(CircleShape)
    }
    Box(
        modifier = modifier
            .size(size)
            .alpha(if (onClick == null) DISABLED_ALPHA else 1f)
            .then(surface)
            .clickable(enabled = onClick != null, role = Role.Button) { onClick?.invoke() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSizeFor(size)),
        )
    }
}

/** Иконка в круге: 40 → 20, 44 → 22, 52 → 24, 60 → 30 — как в макете. */
private fun iconSizeFor(size: Dp): Dp = when {
    size >= LargeButton -> size / 2
    size >= FilmaxMetrics.IconButtonLarge -> 24.dp
    size >= FilmaxMetrics.IconButtonSize -> 22.dp
    else -> 20.dp
}

private val LargeButton = 56.dp
private val GlowBlur = 24.dp
private const val DISABLED_ALPHA = 0.38f
