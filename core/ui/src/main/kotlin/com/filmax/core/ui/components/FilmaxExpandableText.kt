package com.filmax.core.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Сколько строк описания видно до разворота: абзац, но не простыня над кнопками и сериями. */
private const val DEFAULT_COLLAPSED_LINES = 4

/**
 * Длинный текст, свёрнутый до [collapsedMaxLines] строк, с переключателем «Подробнее» / «Свернуть».
 *
 * Переключатель появляется, только если текст правда не влез: короткому описанию он ни к чему.
 * Разворачивает тап по всему блоку, а не только по подписи — в мелкую ссылку пальцем не попасть.
 * Ripple у блока нет: подсвеченный абзац текста выглядит как ошибка, а ответ на тап и так виден.
 */
@Composable
fun FilmaxExpandableText(
    text: String,
    modifier: Modifier = Modifier,
    collapsedMaxLines: Int = DEFAULT_COLLAPSED_LINES,
) {
    var expanded by rememberSaveable(text) { mutableStateOf(false) }
    // Меряется только в свёрнутом виде: развёрнутый текст влезает всегда.
    var overflows by remember(text) { mutableStateOf(false) }
    val toggleable = expanded || overflows

    Column(
        modifier
            .animateContentSize()
            .clickable(
                enabled = toggleable,
                onClickLabel = if (expanded) COLLAPSE_LABEL else EXPAND_LABEL,
                interactionSource = null,
                indication = null,
            ) { expanded = !expanded },
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (expanded) Int.MAX_VALUE else collapsedMaxLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { layout -> if (!expanded) overflows = layout.hasVisualOverflow },
        )
        if (toggleable) {
            Text(
                if (expanded) COLLAPSE_LABEL else EXPAND_LABEL,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

private const val EXPAND_LABEL = "Подробнее"
private const val COLLAPSE_LABEL = "Свернуть"
