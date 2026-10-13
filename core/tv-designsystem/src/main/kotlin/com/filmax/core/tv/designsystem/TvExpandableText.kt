package com.filmax.core.tv.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.saveable.rememberSaveable
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

/** Сколько строк видно до разворота: абзац, но не простыня между кнопками и сериями. */
private const val DEFAULT_COLLAPSED_LINES = 3

/** Поле между текстом и рамкой фокуса — текст не должен упираться в обводку. */
val TvExpandableTextInset = 12.dp

/** С какой доли высоты свёрнутый текст начинает затухать. */
private const val FADE_FROM = 0.55f

/**
 * Длинный текст под пульт: свёрнут до [collapsedMaxLines] строк с затуханием снизу, «ОК»
 * разворачивает и сворачивает.
 *
 * Фокусируется, только если текст правда не влез: иначе это лишняя остановка пульта на пути от
 * кнопок к сериям. Фокус — рамка и свечение без увеличения ([TvFocusStyle.Text]).
 *
 * Внутри — поле [TvExpandableTextInset] под рамку. Снаружи его вычитают из отступа, чтобы сам
 * текст стоял на одной линии с соседними секциями, а рамка выходила за неё.
 */
@Composable
fun TvExpandableText(
    text: String,
    modifier: Modifier = Modifier,
    collapsedMaxLines: Int = DEFAULT_COLLAPSED_LINES,
) {
    var expanded by rememberSaveable(text) { mutableStateOf(false) }
    // Меряется только в свёрнутом виде: развёрнутый текст влезает всегда.
    var overflows by remember(text) { mutableStateOf(false) }
    val toggleable = expanded || overflows

    val body: @Composable () -> Unit = {
        Column(Modifier.padding(TvExpandableTextInset)) {
            Text(
                text,
                style = FxType.body,
                color = LocalFxColors.current.textSecondary,
                maxLines = if (expanded) Int.MAX_VALUE else collapsedMaxLines,
                overflow = TextOverflow.Clip,
                onTextLayout = { layout -> if (!expanded) overflows = layout.hasVisualOverflow },
                modifier = if (overflows && !expanded) Modifier.fadeBottom() else Modifier,
            )
            if (toggleable) ToggleLabel(expanded)
        }
    }
    if (toggleable) {
        TvFocusSurface(onClick = { expanded = !expanded }, modifier = modifier, style = TvFocusStyle.Text) { body() }
    } else {
        Box(modifier) { body() }
    }
}

@Composable
private fun ToggleLabel(expanded: Boolean) {
    Row(
        Modifier.padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            if (expanded) COLLAPSE_LABEL else EXPAND_LABEL,
            style = FxType.chip,
            color = LocalFxColors.current.text,
        )
        if (!expanded) {
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }
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

private const val EXPAND_LABEL = "Читать полностью"
private const val COLLAPSE_LABEL = "Свернуть"
