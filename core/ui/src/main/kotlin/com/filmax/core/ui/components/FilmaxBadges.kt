package com.filmax.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxColors
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.FilmaxTheme
import com.filmax.core.designsystem.FilmaxType
import com.filmax.core.designsystem.ShapeBadge
import com.filmax.core.designsystem.ShapePill
import com.filmax.core.designsystem.glow

/** Пилюля рейтинга — вторым акцентом палитры: на постере и в мете оценка видна первой. */
@Composable
fun FilmaxRatingPill(rating: String, modifier: Modifier = Modifier) {
    val palette = FilmaxTheme.palette
    Box(
        modifier
            .clip(ShapePill)
            .background(palette.accent2)
            .padding(horizontal = 7.dp, vertical = 2.dp),
    ) {
        Text(rating, style = FilmaxType.pill, color = palette.onAccent2, maxLines = 1)
    }
}

/** Бейдж «5.1», «HDR» рядом с вариантом дорожки — та же заливка, что у рейтинга, но мельче. */
@Composable
fun FilmaxTagBadge(text: String, modifier: Modifier = Modifier) {
    val palette = FilmaxTheme.palette
    Box(
        modifier
            .clip(TagShape)
            .background(palette.accent2)
            .padding(horizontal = 6.dp, vertical = 1.dp),
    ) {
        Text(text, style = FilmaxType.pill, color = palette.onAccent2, maxLines = 1)
    }
}

/** Контурный бейдж мета-строки: возраст «12+», «4K». */
@Composable
fun FilmaxBadge(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .border(1.dp, BadgeBorder, ShapeBadge)
            .padding(horizontal = 6.dp, vertical = 1.dp),
    ) {
        Text(text, style = FilmaxType.tag, color = FilmaxColors.TextSecondary, maxLines = 1)
    }
}

/**
 * Надзаголовок «ВЫБОР РЕДАКЦИИ», «ФИЛЬМ · ДРАМА»: точка-акцент со свечением и подпись в верхнем
 * регистре. [color] — цвет текста: на Главной и в деталях это акцент для текста, в подборке — её оттенок.
 */
@Composable
fun FilmaxOverline(text: String, modifier: Modifier = Modifier, color: Color = FilmaxTheme.palette.accentText) {
    val palette = FilmaxTheme.palette
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(6.dp)
                .glow(palette.accent, CircleShape, blur = 10.dp, spread = 0.dp, offsetY = 0.dp)
                .clip(CircleShape)
                .background(palette.accent),
        )
        Text(text.uppercase(), style = FilmaxType.overline, color = color, maxLines = 1)
    }
}

/**
 * Заголовок ряда: «Продолжить просмотр 4 →». [count] — серым рядом с названием; [onMore] — стрелка
 * справа и тап по всей строке открывают ряд целиком. Без [onMore] стрелки нет: обещать «ещё» нечем.
 */
@Composable
fun FilmaxSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    count: Int? = null,
    onMore: (() -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onMore != null) Modifier.clickable(role = Role.Button, onClick = onMore) else Modifier)
            .padding(
                start = FilmaxMetrics.ScreenPadding,
                end = FilmaxMetrics.ScreenPadding,
                top = FilmaxMetrics.SectionTop,
                bottom = FilmaxMetrics.SectionBottom,
            ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            title,
            style = FilmaxType.sectionTitle,
            color = FilmaxColors.Text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false).semantics { heading() },
        )
        count?.let {
            Text(
                it.toString(),
                style = FilmaxType.meta,
                color = FilmaxColors.TextDim,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
        if (onMore != null) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Все",
                    tint = FilmaxColors.TextSecondary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

private val TagShape = RoundedCornerShape(6.dp)
private val BadgeBorder = Color.White.copy(alpha = 0.4f)
