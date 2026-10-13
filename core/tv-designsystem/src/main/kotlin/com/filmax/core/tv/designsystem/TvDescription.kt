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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.tv.material3.Icon
import androidx.tv.material3.Text

/** Поле между текстом и рамкой фокуса. */
private val DescriptionInset = 10.dp

private const val COLLAPSED_LINES = 3

/** С какой доли высоты свёрнутый текст начинает затухать. */
private const val FADE_FROM = 0.55f

/**
 * Описание тайтла: три строки с затуханием и «Читать полностью ›» — [onOpen] открывает полный
 * текст в боковой панели ([TvSheetHost]). Фокусируется, только если текст правда не влез: иначе
 * это лишняя остановка пульта по дороге к кнопкам. Фокус — рамка и свечение без увеличения.
 *
 * Поле под рамку фокуса выступает за границы описания: в колонке оно занимает ровно место
 * текста — стоит в линию с названием и не раздвигает соседей. [style] — кегль текста: у фильма
 * описание крупнее, у сериала колонка уже.
 */
@Composable
fun TvDescription(text: String, onOpen: () -> Unit, modifier: Modifier = Modifier, style: TextStyle = FxType.body) {
    var overflows by remember(text) { mutableStateOf(false) }
    val body: @Composable () -> Unit = {
        Column(Modifier.padding(DescriptionInset), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text,
                style = style,
                color = LocalFxColors.current.textSecondary,
                maxLines = COLLAPSED_LINES,
                overflow = TextOverflow.Clip,
                onTextLayout = { layout -> overflows = layout.hasVisualOverflow },
                modifier = if (overflows) Modifier.fadeBottom() else Modifier,
            )
            if (overflows) ReadMore()
        }
    }
    val outset = modifier.outset(DescriptionInset)
    if (overflows) {
        TvFocusSurface(onClick = onOpen, modifier = outset, style = TvFocusStyle.Text) { body() }
    } else {
        Column(outset) { body() }
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

/**
 * Содержимое шире и выше раскладки на [inset] с каждой стороны: родитель видит размер без поля,
 * а поле с рамкой фокуса рисуется поверх соседних отступов.
 */
private fun Modifier.outset(inset: Dp): Modifier = layout { measurable, constraints ->
    val insetPx = inset.roundToPx()
    val placeable = measurable.measure(constraints.offset(horizontal = insetPx * 2, vertical = insetPx * 2))
    val width = (placeable.width - insetPx * 2).coerceAtLeast(0)
    val height = (placeable.height - insetPx * 2).coerceAtLeast(0)
    layout(width, height) { placeable.place(-insetPx, -insetPx) }
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
