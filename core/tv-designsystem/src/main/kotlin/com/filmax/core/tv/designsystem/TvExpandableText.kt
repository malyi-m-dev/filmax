@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.filmax.core.tv.designsystem

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface

/** Сколько строк видно до разворота: абзац, но не простыня между кнопками и сериями. */
private const val DEFAULT_COLLAPSED_LINES = 4

/** Поле между текстом и рамкой фокуса — текст не должен упираться в обводку. */
val TvExpandableTextInset = 12.dp

/**
 * Длинный текст под пульт: свёрнут до [collapsedMaxLines] строк, «ОК» разворачивает и сворачивает.
 *
 * Фокусируется, только если текст правда не влез: иначе это лишняя остановка пульта на пути от
 * кнопок к сериям. Фокус — та же белая рамка с ореолом, что у карточек, но без увеличения:
 * абзац, раздувшийся на 8%, читается как сбой вёрстки, а не как фокус.
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
        Column(Modifier.animateContentSize().padding(TvExpandableTextInset)) {
            Text(
                text,
                style = MaterialTheme.typography.bodyLarge,
                color = TvOnSurfaceVariant,
                maxLines = if (expanded) Int.MAX_VALUE else collapsedMaxLines,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { layout -> if (!expanded) overflows = layout.hasVisualOverflow },
            )
            if (toggleable) {
                Text(
                    if (expanded) COLLAPSE_LABEL else EXPAND_LABEL,
                    style = MaterialTheme.typography.labelLarge,
                    color = TvOnSurface,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
    if (toggleable) {
        ExpandableSurface(onClick = { expanded = !expanded }, modifier = modifier, content = body)
    } else {
        Box(modifier) { body() }
    }
}

/** Фокусируемая подложка текста: рамка и лёгкая заливка при фокусе, без масштаба. */
@Composable
private fun ExpandableSurface(onClick: () -> Unit, modifier: Modifier, content: @Composable () -> Unit) {
    // Как в TvFocusCard: фокус читается только в draw-лямбде — его смена перерисовывает рамку,
    // не перекомпоновывая текст.
    val focused = remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(TvMetrics.CardShape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = TvSurfaceContainer,
            pressedContainerColor = TvSurfaceContainer,
            contentColor = TvOnSurface,
            focusedContentColor = TvOnSurface,
        ),
        modifier = modifier.onFocusChanged { focused.value = it.isFocused },
    ) {
        Box(
            Modifier.drawWithContent {
                drawContent()
                if (focused.value) drawFocusRing(TvMetrics.CardShape)
            },
        ) {
            content()
        }
    }
}

private const val EXPAND_LABEL = "Подробнее"
private const val COLLAPSE_LABEL = "Свернуть"
