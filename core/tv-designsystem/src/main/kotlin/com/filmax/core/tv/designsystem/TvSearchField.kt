// Строка поиска для пульта: поле-кнопка, которое открывает экранную клавиатуру, и кнопка голоса.
package com.filmax.core.tv.designsystem

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.filmax.core.designsystem.Onest

/** Высота строки поиска — одна на поле и на кнопку голоса рядом. */
val TvSearchBarHeight = 48.dp

private val FieldPadding = 18.dp
private val FieldIconSize = 20.dp
private val MicGlyphSize = 22.dp
private val CaretWidth = 2.dp
private val CaretHeight = 22.dp
private const val CARET_BLINK_MS = 530

/** Подложка поля — чуть плотнее стекла панелей, чтобы строка читалась как поле ввода. */
private const val FIELD_ALPHA = 0.08f
private const val ACTIVE_FIELD_ALPHA = 0.1f

private val PlaceholderStyle = TextStyle(fontSize = 15.sp, lineHeight = 20.sp)
private val QueryStyle =
    TextStyle(fontFamily = Onest, fontWeight = FontWeight.Medium, fontSize = 17.sp, lineHeight = 22.sp)

private val MicGlowRadius = 14.dp
private val MicFocus =
    TvFocusStyle(TvMetrics.PanelShape, glowRadius = MicGlowRadius, focusedScale = TvMetrics.FocusScale)

/** Обводка поля под диктовку — нейтральная: акцент в этот момент у микрофона. */
private const val DICTATION_BORDER_ALPHA = 0.12f

/**
 * Режим поля. [Button] — поиск закрыт, поле — кнопка, открывающая его. [Typing] — текст набирают
 * экранной клавиатурой: обводка акцентом и мигающий курсор показывают, куда идёт набор.
 * [Dictation] — текст диктуют голосом: нейтральная обводка без курсора, акцент — у микрофона.
 */
enum class TvSearchFieldMode { Button, Typing, Dictation }

/**
 * Поле поиска высотой 48dp: лупа, запрос или [placeholder].
 *
 * В режиме [TvSearchFieldMode.Button] поле — кнопка: фокус как у широких строк, «ОК» ([onClick])
 * открывает поиск. В остальных режимах поле само фокус не берёт: ввод идёт клавишами или голосом.
 */
@Composable
fun TvSearchField(
    query: String,
    placeholder: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    mode: TvSearchFieldMode = TvSearchFieldMode.Button,
) {
    if (mode != TvSearchFieldMode.Button) {
        ActiveField(query, typing = mode == TvSearchFieldMode.Typing, modifier = modifier)
        return
    }
    val colors = LocalFxColors.current
    TvFocusSurface(
        onClick = onClick,
        modifier = modifier.height(TvSearchBarHeight),
        style = TvFocusStyle.Row,
        colors = TvSurfaceColors(container = Color.White.copy(alpha = FIELD_ALPHA), content = colors.text),
    ) {
        FieldContent(Modifier.border(1.dp, colors.outline, TvMetrics.PanelShape)) {
            Icon(
                Icons.Rounded.Search,
                contentDescription = null,
                tint = colors.textDim,
                modifier = Modifier.size(FieldIconSize)
            )
            if (query.isEmpty()) {
                Text(placeholder, style = PlaceholderStyle, color = colors.textDim, maxLines = 1)
            } else {
                Text(query, style = QueryStyle, color = colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Поле в режиме ввода: не фокусируется; набор [typing] — обводка акцентом и курсор, диктовка — без них. */
@Composable
private fun ActiveField(query: String, typing: Boolean, modifier: Modifier) {
    val colors = LocalFxColors.current
    val border = if (typing) colors.accent else Color.White.copy(alpha = DICTATION_BORDER_ALPHA)
    Box(
        modifier
            .height(TvSearchBarHeight)
            .background(Color.White.copy(alpha = ACTIVE_FIELD_ALPHA), TvMetrics.PanelShape),
    ) {
        FieldContent(Modifier.border(1.dp, border, TvMetrics.PanelShape)) {
            Icon(
                Icons.Rounded.Search,
                contentDescription = null,
                tint = colors.accentText,
                modifier = Modifier.size(FieldIconSize)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(query, style = QueryStyle, color = colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (typing) TvCaret(CaretWidth, CaretHeight)
            }
        }
    }
}

@Composable
private fun FieldContent(modifier: Modifier, content: @Composable () -> Unit) {
    Row(
        modifier.fillMaxSize().padding(horizontal = FieldPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) { content() }
}

/**
 * Кнопка голосового поиска 48dp рядом с полем. [listening] — идёт слушание: кнопка залита
 * акцентом и выглядит как в фокусе (белая рамка, увеличение, пульс свечения), хотя фокус заперт в
 * оверлее, — видно, откуда пришёл голос.
 */
@Composable
fun TvMicButton(onClick: () -> Unit, modifier: Modifier = Modifier, listening: Boolean = false) {
    val colors = LocalFxColors.current
    TvFocusSurface(
        onClick = onClick,
        modifier = modifier.then(if (listening) Modifier.listeningLook() else Modifier).size(TvSearchBarHeight),
        style = MicFocus,
        colors = if (listening) {
            TvSurfaceColors(container = colors.accent, content = colors.onAccent)
        } else {
            TvSurfaceColors(container = Color.White.copy(alpha = FIELD_ALPHA), content = colors.text)
        },
    ) {
        Icon(
            Icons.Rounded.Mic,
            contentDescription = "Голосовой поиск",
            modifier = Modifier.align(Alignment.Center).size(MicGlyphSize),
        )
    }
}

/**
 * Слушающая кнопка выглядит как в фокусе ([TvFocusSurface]): рамка, увеличение и пульс свечения.
 * Пульс — тот же, что у фокуса: в облегчённом режиме и при сниженной анимации он горит ровно.
 */
private fun Modifier.listeningLook(): Modifier = this
    .graphicsLayer {
        scaleX = TvMetrics.FocusScale
        scaleY = TvMetrics.FocusScale
    }
    .fxGlowPulse(active = true, cornerRadius = MicGlowRadius)
    .tvFocusOutline(focused = true, shape = TvMetrics.PanelShape)

/**
 * Мигающий курсор набора — после запроса в поле и после распознанной речи. При сниженной
 * анимации горит ровно: мигание — та же анимация, только бесконечная.
 */
@Composable
internal fun TvCaret(width: Dp, height: Dp, modifier: Modifier = Modifier) {
    val reduced = LocalReducedMotion.current
    val transition = rememberInfiniteTransition(label = "caret")
    val blink by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(CARET_BLINK_MS), RepeatMode.Reverse),
        label = "caretBlink",
    )
    Box(
        modifier
            .padding(start = 2.dp)
            .size(width, height)
            .graphicsLayer { alpha = if (reduced) 1f else blink }
            .background(LocalFxColors.current.accent),
    )
}
