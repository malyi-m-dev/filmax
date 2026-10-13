package com.filmax.core.tv.designsystem

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text

/**
 * Поля строки: столько же, на сколько строку сдвигают влево ([TvTextRowInset]), — тогда текст
 * стоит ровно по линии заголовка над списком, а подложка фокуса выступает за неё.
 */
val TvTextRowInset = 12.dp

private val RowPadding = PaddingValues(horizontal = TvTextRowInset, vertical = 8.dp)

/** Плотная строка — шаг 24dp: недавние запросы под клавиатурой, где места по высоте мало. */
private val DenseRowPadding = PaddingValues(horizontal = TvTextRowInset, vertical = 3.dp)
private val RowTextStyle = TextStyle(fontSize = 14.sp, lineHeight = 18.sp)
private val CheckSize = 18.dp
private const val IDLE_TEXT_ALPHA = 0.75f

/**
 * Строка-текст во всю ширину списка: недавний запрос, вариант сортировки. Без фокуса — просто
 * текст, в фокусе — стеклянная подложка с рамкой. [selected] — выбранный вариант: акцентом и с
 * галочкой справа. [dense] — плотный шаг строк (недавние запросы поиска).
 */
@Composable
fun TvTextRow(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    dense: Boolean = false,
) {
    val colors = LocalFxColors.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val idleText = if (selected) colors.accentText else colors.text.copy(alpha = IDLE_TEXT_ALPHA)
    TvFocusSurface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        style = TvFocusStyle.Row,
        colors = TvSurfaceColors(
            container = Color.Transparent,
            content = idleText,
            focusedContainer = colors.surfaceHigh,
            focusedContent = colors.text,
        ),
        interactionSource = interaction,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(if (dense) DenseRowPadding else RowPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text,
                style = RowTextStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (selected) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = if (focused) colors.text else colors.accentText,
                    modifier = Modifier.size(CheckSize),
                )
            }
        }
    }
}
