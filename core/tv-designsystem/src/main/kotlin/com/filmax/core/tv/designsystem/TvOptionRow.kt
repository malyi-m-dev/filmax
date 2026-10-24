// Строка варианта в панели выбора: дорожка, субтитры, качество. Выбранный — акцентом и галочкой.
package com.filmax.core.tv.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.filmax.core.designsystem.Onest

private val RowHeight = 56.dp
private val MarkSize = 24.dp
private val EmptyMarkSize = 20.dp
private val BadgeShape = RoundedCornerShape(5.dp)
private const val IDLE_FILL_ALPHA = 0.06f
private const val FOCUSED_FILL_ALPHA = 0.14f
private const val EMPTY_MARK_ALPHA = 0.25f
private const val BADGE_BORDER_ALPHA = 0.3f
private const val BADGE_TEXT_ALPHA = 0.8f

private val TitleStyle = TextStyle(
    fontFamily = Onest,
    fontWeight = FontWeight.SemiBold,
    fontSize = 14.sp,
    lineHeight = 18.sp
)
private val BadgeStyle = TextStyle(
    fontFamily = Onest,
    fontWeight = FontWeight.Bold,
    fontSize = 10.sp,
    lineHeight = 12.sp,
    letterSpacing = 0.04.em,
)

/**
 * Вариант выбора в панели плеера: название, пояснение под ним ([detail]), значок формата справа
 * ([badge] — «5.1», «SDH») и отметка. Выбранный — название акцентом и галочка в акцентном круге,
 * остальные — пустой круг: выбор и фокус читаются раздельно. Фокус — как у широких строк (1.02).
 */
// Компонент дизайн-системы: параметры — его публичный API (содержимое, выбор, действие, хвост опций).
@Suppress("LongParameterList")
@Composable
fun TvOptionRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    detail: String? = null,
    badge: String? = null,
) {
    val colors = LocalFxColors.current
    TvFocusSurface(
        onClick = onClick,
        modifier = modifier.height(RowHeight),
        style = TvFocusStyle.Row,
        colors = TvSurfaceColors(
            container = Color.White.copy(alpha = IDLE_FILL_ALPHA),
            content = colors.text,
            focusedContainer = Color.White.copy(alpha = FOCUSED_FILL_ALPHA),
        ),
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = TitleStyle,
                    color = if (selected) colors.accentText else colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!detail.isNullOrBlank()) {
                    Text(
                        detail,
                        style = FxType.metaOnImage,
                        color = colors.textDim,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            badge?.let { FormatBadge(it) }
            SelectionMark(selected)
        }
    }
}

/** Значок формата: «5.1», «HDR» — контуром, чтобы не спорить с отметкой выбора. */
@Composable
private fun FormatBadge(text: String) {
    Text(
        text,
        style = BadgeStyle,
        color = LocalFxColors.current.text.copy(alpha = BADGE_TEXT_ALPHA),
        modifier = Modifier
            .border(1.dp, Color.White.copy(alpha = BADGE_BORDER_ALPHA), BadgeShape)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
private fun SelectionMark(selected: Boolean) {
    val colors = LocalFxColors.current
    if (selected) {
        Box(Modifier.size(MarkSize).background(colors.accent, CircleShape), contentAlignment = Alignment.Center) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = "Выбрано",
                tint = colors.onAccent,
                modifier = Modifier.size(16.dp)
            )
        }
    } else {
        Box(
            Modifier
                .padding(2.dp)
                .size(EmptyMarkSize)
                .border(2.dp, Color.White.copy(alpha = EMPTY_MARK_ALPHA), CircleShape),
        )
    }
}
