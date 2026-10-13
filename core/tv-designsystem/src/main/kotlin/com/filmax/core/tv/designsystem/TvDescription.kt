package com.filmax.core.tv.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text

/** Поле между текстом и рамкой фокуса — снаружи его вычитают, чтобы текст стоял в линию с соседями. */
val TvDescriptionInset = 10.dp

private const val COLLAPSED_LINES = 3

/** С какой доли высоты свёрнутый текст начинает затухать. */
private const val FADE_FROM = 0.55f

/**
 * Описание тайтла: три строки с затуханием и «Читать полностью ›» — [onOpen] открывает полный
 * текст в боковой панели ([TvSheetHost]). Фокусируется, только если текст правда не влез: иначе
 * это лишняя остановка пульта по дороге к кнопкам. Фокус — рамка и свечение без увеличения.
 */
@Composable
fun TvDescription(text: String, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    var overflows by remember(text) { mutableStateOf(false) }
    val body: @Composable () -> Unit = {
        Column(Modifier.padding(TvDescriptionInset), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text,
                style = FxType.body,
                color = LocalFxColors.current.textSecondary,
                maxLines = COLLAPSED_LINES,
                overflow = TextOverflow.Clip,
                onTextLayout = { layout -> overflows = layout.hasVisualOverflow },
                modifier = if (overflows) Modifier.fadeBottom() else Modifier,
            )
            if (overflows) ReadMore()
        }
    }
    if (overflows) {
        TvFocusSurface(onClick = onOpen, modifier = modifier, style = TvFocusStyle.Text) { body() }
    } else {
        Column(modifier) { body() }
    }
}

@Composable
private fun ReadMore() {
    val color = LocalFxColors.current.accentText
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("Читать полностью", style = FxType.chip, color = color)
        Icon(
            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** Затухание нижних строк: обрезанный текст уходит в фон, а не обрывается на полуслове. */
private fun Modifier.fadeBottom(): Modifier =
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            drawRect(
                brush = Brush.verticalGradient(FADE_FROM to Color.Black, 1f to Color.Transparent),
                blendMode = BlendMode.DstIn,
            )
        }
