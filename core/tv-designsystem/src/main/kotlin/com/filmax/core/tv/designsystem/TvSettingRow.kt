// Строки настроек Профиля: со значением и «›» (выбор в панели) и с переключателем (вкл/выкл).
// Фокус — рамка, свечение и увеличение 1.02 (широкая строка), подложка светлеет, подпись
// становится плотнее, значение окрашивается акцентом.
package com.filmax.core.tv.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import kotlin.math.roundToInt

private val RowHeight = 52.dp
private val RowPadding = 18.dp
private val ChevronSize = 22.dp

/** Строка — стекло 6%, в фокусе — 14%: фокус держат рамка и подъём фона, а не увеличение. */
private const val ROW_ALPHA = 0.06f
private const val ROW_FOCUSED_ALPHA = 0.14f

/** Шеврон тише значения: в фокусе — 60%, без фокуса — 40%. */
private const val CHEVRON_ALPHA = 0.4f
private const val CHEVRON_FOCUSED_ALPHA = 0.6f

/**
 * Строка настройки. [value] — текущее значение справа, со стрелкой «›»: «ОК» открывает выбор;
 * null — строка-действие без значения («Проверить обновления»). [destructive] красит подпись
 * приглушённым красным — «Выйти из аккаунта».
 */
@Composable
fun TvSettingRow(
    label: String,
    value: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    destructive: Boolean = false,
) {
    val colors = LocalFxColors.current
    SettingSurface(onClick, modifier, content = if (destructive) colors.error else colors.text) { focused ->
        SettingLabel(label, focused)
        if (value != null) SettingValue(value, focused)
    }
}

/**
 * Строка с переключателем: «ОК» включает и выключает [checked]. Бегунок едет за 160 мс (токен
 * фокуса), дорожка включённого — акцентом.
 */
@Composable
fun TvSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    SettingSurface(
        onClick = { onCheckedChange(!checked) },
        modifier = modifier.semantics {
            role = Role.Switch
            stateDescription = if (checked) "Вкл" else "Выкл"
        },
        content = LocalFxColors.current.text,
    ) { focused ->
        SettingLabel(label, focused)
        TvSwitch(checked)
    }
}

/** Подложка и фокус строки; [row] получает фокус строки — от него зависит вес подписи и значения. */
@Composable
private fun SettingSurface(
    onClick: () -> Unit,
    modifier: Modifier,
    content: Color,
    row: @Composable RowScope.(focused: Boolean) -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    TvFocusSurface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(RowHeight),
        style = TvFocusStyle.Row,
        colors = TvSurfaceColors(
            container = Color.White.copy(alpha = ROW_ALPHA),
            content = content,
            focusedContainer = Color.White.copy(alpha = ROW_FOCUSED_ALPHA),
        ),
        interactionSource = interaction,
    ) {
        Row(
            Modifier.align(Alignment.CenterStart).fillMaxWidth().padding(horizontal = RowPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            row(focused)
        }
    }
}

/** Подпись строки: без фокуса — 500, в фокусе — 600, как в макете. */
@Composable
private fun RowScope.SettingLabel(label: String, focused: Boolean) {
    val weight = if (focused) FontWeight.SemiBold else FontWeight.Medium
    Text(label, style = FxType.button.copy(fontWeight = weight), maxLines = 1)
    Spacer(Modifier.weight(1f))
}

@Composable
private fun SettingValue(value: String, focused: Boolean) {
    val colors = LocalFxColors.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            value,
            style = FxType.metaRow.copy(fontWeight = if (focused) FontWeight.Medium else FontWeight.Normal),
            color = if (focused) colors.accentText else colors.textSecondary,
            maxLines = 1,
        )
        Icon(
            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.text.copy(alpha = if (focused) CHEVRON_FOCUSED_ALPHA else CHEVRON_ALPHA),
            modifier = Modifier.size(ChevronSize),
        )
    }
}

private val TrackWidth = 40.dp
private val TrackHeight = 22.dp
private val ThumbSize = 16.dp
private val ThumbInset = 3.dp

/** Дорожка выключенного — белый 18%: видна на стекле строки, но не спорит с включённым. */
private const val TRACK_OFF_ALPHA = 0.18f

/**
 * Переключатель: дорожка 40×22 и белый бегунок 16dp. Положение бегунка и цвет дорожки
 * анимируются и читаются в layout- и draw-фазах — переключение не перекомпонует строку.
 */
@Composable
private fun TvSwitch(checked: Boolean) {
    val colors = LocalFxColors.current
    val position by animateFloatAsState(if (checked) 1f else 0f, FxMotion.focus(), label = "switchThumb")
    val track by animateColorAsState(
        if (checked) colors.accent else Color.White.copy(alpha = TRACK_OFF_ALPHA),
        FxMotion.focus(),
        label = "switchTrack",
    )
    Box(
        Modifier
            .size(TrackWidth, TrackHeight)
            .drawBehind { drawRoundRect(track, cornerRadius = CornerRadius(size.height / 2)) },
    ) {
        Box(
            Modifier
                .offset {
                    val travel = (TrackWidth - ThumbSize - ThumbInset * 2).toPx()
                    IntOffset((ThumbInset.toPx() + travel * position).roundToInt(), ThumbInset.roundToPx())
                }
                .size(ThumbSize)
                .drawBehind { drawCircle(Color.White) },
        )
    }
}
