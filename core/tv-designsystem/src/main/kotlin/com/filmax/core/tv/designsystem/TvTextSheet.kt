package com.filmax.core.tv.designsystem

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import kotlinx.coroutines.launch

/** Шаг прокрутки текста стрелкой. */
private val ScrollStep = 80.dp

private val SheetTitle = FxType.screenTitle.copy(fontSize = 26.sp, lineHeight = 30.sp)
private val SheetBody = TextStyle(fontSize = 16.sp, lineHeight = 25.6.sp)

/**
 * Полный текст в боковой панели ([TvSheetHost]): надзаголовок, название, мета и абзацы.
 * ↑↓ прокручивают текст по 80dp, «←» закрывает панель — как «Назад».
 */
@Composable
fun BoxScope.TvTextSheet(overline: String, title: String, meta: String?, text: String, onClose: () -> Unit) {
    val colors = LocalFxColors.current
    val scroll = rememberScrollState()
    Column(
        Modifier
            .fillMaxSize()
            .padding(start = 34.dp, end = 40.dp, top = 30.dp, bottom = 70.dp)
            .sheetKeys(scroll, onClose)
            .focusable(),
    ) {
        SheetHeading(overline, title, meta)
        Text(
            text,
            style = SheetBody,
            color = colors.text.copy(alpha = BODY_ALPHA),
            modifier = Modifier
                .padding(top = 20.dp)
                .sheetEnter(3)
                .fadeWhileMoreBelow(scroll)
                .verticalScroll(scroll),
        )
    }
    ScrollIndicator(
        scroll,
        Modifier.align(Alignment.TopEnd).padding(end = 16.dp, top = 30.dp, bottom = 70.dp).sheetEnter(4),
    )
    TvKeyHints(
        hints = listOf(
            TvKeyHint(listOf(Icons.Rounded.KeyboardArrowUp, Icons.Rounded.KeyboardArrowDown), "прокрутка"),
            TvKeyHint(listOf(Icons.AutoMirrored.Rounded.ArrowBack), "закрыть"),
        ),
        modifier = Modifier.align(Alignment.BottomStart).padding(start = 34.dp, bottom = 22.dp),
    )
}

private const val BODY_ALPHA = 0.86f

/** ↑↓ — прокрутка текста шагом 80dp, «←» — закрыть панель. */
@Composable
private fun Modifier.sheetKeys(scroll: ScrollState, onClose: () -> Unit): Modifier {
    val scope = rememberCoroutineScope()
    val step = with(LocalDensity.current) { ScrollStep.toPx() }
    return onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        when (event.key) {
            Key.DirectionUp -> {
                scope.launch { scroll.animateScrollBy(-step, FxMotion.scroll()) }
                true
            }
            Key.DirectionDown -> {
                scope.launch { scroll.animateScrollBy(step, FxMotion.scroll()) }
                true
            }
            Key.DirectionLeft -> {
                onClose()
                true
            }
            else -> false
        }
    }
}

/** Надзаголовок, название и мета — появляются по очереди вслед за панелью. */
@Composable
private fun SheetHeading(overline: String, title: String, meta: String?) {
    val colors = LocalFxColors.current
    Text(overline.uppercase(), style = FxType.overline, color = colors.accentText, modifier = Modifier.sheetEnter(0))
    Text(title, style = SheetTitle, color = colors.text, modifier = Modifier.padding(top = 8.dp).sheetEnter(1))
    if (meta != null) {
        Text(
            meta,
            style = FxType.meta,
            color = colors.textSecondary,
            modifier = Modifier.padding(top = 6.dp).sheetEnter(2)
        )
    }
}

/** Содержимое панели появляется, когда сама панель почти выехала, — не раньше неё. */
private const val SHEET_ENTER_DELAY_MS = 120

private fun Modifier.sheetEnter(index: Int): Modifier = fxEnter(index, startDelayMillis = SHEET_ENTER_DELAY_MS)

/** Текст гаснет с 80% высоты окна — так видно, что ниже есть продолжение. */
private const val FADE_FROM = 0.8f

/**
 * Затухание низа текста, пока ниже есть что читать; докрутили до конца — последние строки
 * видны целиком. Маска в координатах окна прокрутки, а не текста: стоит на месте, текст под ней
 * едет. Сила затухания читается при отрисовке — прокрутка не перекомпоновывает панель.
 */
@Composable
private fun Modifier.fadeWhileMoreBelow(scroll: ScrollState): Modifier {
    val strength by animateFloatAsState(if (scroll.canScrollForward) 1f else 0f, FxMotion.fade(), label = "textFade")
    return graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val edge = Color.Black.copy(alpha = 1f - strength)
            drawRect(Brush.verticalGradient(FADE_FROM to Color.Black, 1f to edge), blendMode = BlendMode.DstIn)
        }
}

/** Полоса прокрутки — только если тексту есть куда прокручиваться. */
@Composable
private fun ScrollIndicator(scroll: ScrollState, modifier: Modifier) {
    if (scroll.maxValue == 0) return
    val colors = LocalFxColors.current
    Box(modifier.width(4.dp).fillMaxHeight().background(Color.White.copy(alpha = TRACK_ALPHA), IndicatorShape)) {
        val viewport = scroll.viewportSize.toFloat()
        val total = viewport + scroll.maxValue
        val thumb = (viewport / total).coerceIn(MIN_THUMB, 1f)
        val offset = scroll.value / total
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(thumb)
                .graphicsOffset(offset)
                .background(colors.accent, IndicatorShape),
        )
    }
}

private val IndicatorShape = RoundedCornerShape(2.dp)
private const val TRACK_ALPHA = 0.1f
private const val MIN_THUMB = 0.15f

/** Сдвиг бегунка долей высоты трека — без перемера лейаута на каждом шаге прокрутки. */
private fun Modifier.graphicsOffset(fraction: Float): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) {
            placeable.place(0, (constraints.maxHeight * fraction).toInt())
        }
    }

/**
 * Подсказка клавиш внизу панели: значки клавиш и что они делают. [keyText] — клавиша с надписью
 * вместо значка («OK»): у центральной кнопки пульта значка нет.
 */
data class TvKeyHint(val keys: List<ImageVector>, val label: String, val keyText: String? = null)

/**
 * Размер клавиши-подсказки: обычная 22dp; [Small] — 18dp под транспортом плеера, где подсказки —
 * второй план и не должны спорить с кнопками.
 */
enum class TvKeyCapSize(val size: Dp, val glyph: Dp) {
    Regular(22.dp, 16.dp),
    Small(18.dp, 12.dp),
}

/** Ряд подсказок клавиш: «↑↓ прокрутка  ← закрыть». */
@Composable
fun TvKeyHints(hints: List<TvKeyHint>, modifier: Modifier = Modifier, keySize: TvKeyCapSize = TvKeyCapSize.Regular) {
    val colors = LocalFxColors.current
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
        hints.forEach { hint ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                hint.keys.forEach { icon -> TvKeyCap(icon, keySize) }
                hint.keyText?.let { text -> TextKeyCap(text, keySize) }
                Text(hint.label, style = FxType.metaOnImage, color = colors.textSecondary)
            }
        }
    }
}

private val KeyCapShape = RoundedCornerShape(6.dp)
private const val KEYCAP_FILL_ALPHA = 0.12f
private const val KEYCAP_BORDER_ALPHA = 0.22f
private val KeyTextPadding = 6.dp
private val KeyTextStyle =
    TextStyle(fontFamily = Onest, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 0.04.em)

/** Клавиша-значок подсказки. internal — подсказку «назад» с гаснущей подписью собирает [TvBackHint]. */
@Composable
internal fun TvKeyCap(icon: ImageVector, keySize: TvKeyCapSize = TvKeyCapSize.Regular) {
    Box(Modifier.size(keySize.size).keyCapSurface(), contentAlignment = Alignment.Center) {
        Icon(
            icon,
            contentDescription = null,
            tint = LocalFxColors.current.text,
            modifier = Modifier.size(keySize.glyph)
        )
    }
}

/** Клавиша с надписью («OK»): той же высоты, ширина — по надписи. */
@Composable
private fun TextKeyCap(text: String, keySize: TvKeyCapSize) {
    Box(
        Modifier.height(keySize.size).keyCapSurface().padding(horizontal = KeyTextPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = KeyTextStyle, color = LocalFxColors.current.text)
    }
}

private fun Modifier.keyCapSurface(): Modifier = this
    .background(Color.White.copy(alpha = KEYCAP_FILL_ALPHA), KeyCapShape)
    .border(1.dp, Color.White.copy(alpha = KEYCAP_BORDER_ALPHA), KeyCapShape)
