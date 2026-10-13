// Карточка папки-закладки в «Моё»: цветная плашка с иконкой и веером постеров, подпись под ней.
// И первая ячейка сетки — «Создать папку» пунктиром.
package com.filmax.core.tv.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.filmax.core.ui.components.PosterImage

/** Высота плашки папки: в сетке на три колонки при ширине 264dp это 16:9. */
private val FolderCardHeight = 148.dp
private val FolderRadius = 14.dp
private val FolderShape = RoundedCornerShape(FolderRadius)
private val FolderFocus = TvFocusStyle(FolderShape, glowRadius = 14.dp, focusedScale = TvMetrics.FocusScale)
private val IconTileSize = 30.dp
private val IconTileShape = RoundedCornerShape(9.dp)
private val PosterShape = RoundedCornerShape(7.dp)
private val PosterShadow = 8.dp
private val CaptionGap = 10.dp

/** Подложка иконки на цветной плашке — белый 18%, как стекло поверх цвета. */
private const val ICON_TILE_ALPHA = 0.18f
private const val EMPTY_TEXT_ALPHA = 0.7f

/** Постер веера: размер, отступ от правого и верхнего края плашки, наклон. */
private data class FanSlot(val width: Dp, val height: Dp, val end: Dp, val top: Dp, val degrees: Float)

/** Три постера веером, от заднего к переднему — передний рисуется последним, поверх соседей. */
private val FanSlots = listOf(
    FanSlot(66.dp, 99.dp, end = 118.dp, top = 34.dp, degrees = -9f),
    FanSlot(70.dp, 105.dp, end = 66.dp, top = 22.dp, degrees = -2f),
    FanSlot(70.dp, 105.dp, end = 14.dp, top = 30.dp, degrees = 7f),
)

/**
 * Карточка папки: плашка цвета [tint] (папке цвет выбирает пользователь), иконка папки и до трёх
 * постеров веером; пустая папка вместо веера говорит «Пусто». Под плашкой — название (в фокусе
 * бегущей строкой) и [caption] — число тайтлов и когда папку меняли. Ширину задаёт сетка; в
 * группе ([TvDimGroup]) карточка без фокуса приглушается.
 */
// Компонент дизайн-системы: параметры — его публичный API (содержимое, цвет, onClick, modifier),
// как у TvCollectionCard; объект-обёртка сломал бы Compose-конвенцию modifier прямым параметром.
@Suppress("LongParameterList")
@Composable
fun TvFolderCard(
    title: String,
    caption: String?,
    posters: List<String>,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Column(modifier.tvNeighborDim(focused)) {
        TvFocusSurface(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().height(FolderCardHeight),
            style = FolderFocus,
            colors = TvSurfaceColors(container = tint, content = Color.White),
            interactionSource = interaction,
        ) {
            FolderIcon(Modifier.align(Alignment.TopStart).padding(14.dp))
            if (posters.isEmpty()) {
                Text(
                    "Пусто",
                    style = FxType.metaOnImage,
                    color = Color.White.copy(alpha = EMPTY_TEXT_ALPHA),
                    modifier = Modifier.align(Alignment.Center),
                )
            } else {
                PosterFan(posters)
            }
        }
        TvCardTitle(title, focused, Modifier.padding(top = CaptionGap), style = FxType.titleOnImage)
        if (!caption.isNullOrBlank()) {
            Text(caption, style = FxType.metaOnImage, color = LocalFxColors.current.textSecondary, maxLines = 1)
        }
    }
}

@Composable
private fun FolderIcon(modifier: Modifier) {
    Box(
        modifier.size(IconTileSize).background(Color.White.copy(alpha = ICON_TILE_ALPHA), IconTileShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.Folder, contentDescription = null, modifier = Modifier.size(17.dp))
    }
}

/** Веер из последних слотов: один постер встаёт передним, два — передним и средним. */
@Composable
private fun BoxScope.PosterFan(posters: List<String>) {
    val shown = posters.take(FanSlots.size)
    val slots = FanSlots.takeLast(shown.size)
    // Первый тайтл папки — самый свежий, он должен лежать сверху, то есть рисоваться последним.
    shown.reversed().zip(slots).forEach { (url, slot) ->
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = -slot.end, y = slot.top)
                .size(slot.width, slot.height)
                .rotate(slot.degrees)
                .shadow(PosterShadow, PosterShape)
                .background(TvSurfaceContainerHigh, PosterShape),
        ) {
            PosterImage(
                url = url,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                shape = PosterShape,
            )
        }
    }
}

/** Пунктир «Создать папку»: белый 22%, штрих 6 через 5. */
private const val DASH_ALPHA = 0.22f
private val DashWidth = 2.dp
private val DashOn = 6.dp
private val DashOff = 5.dp
private val AddCircleSize = 44.dp
private const val ADD_CIRCLE_ALPHA = 0.08f

/**
 * Первая ячейка сетки папок: пунктирная плашка с «＋» — вход в создание папки. Того же размера,
 * что и [TvFolderCard], чтобы сетка не прыгала.
 */
@Composable
fun TvCreateFolderCard(onClick: () -> Unit, modifier: Modifier = Modifier, label: String = "Создать папку") {
    val colors = LocalFxColors.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    TvFocusSurface(
        onClick = onClick,
        modifier = modifier.tvNeighborDim(focused).fillMaxWidth().height(FolderCardHeight).dashedOutline(),
        style = FolderFocus,
        colors = TvSurfaceColors(container = Color.Transparent, content = colors.accentText),
        interactionSource = interaction,
    ) {
        Column(
            Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier.size(AddCircleSize).background(Color.White.copy(alpha = ADD_CIRCLE_ALPHA), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(24.dp))
            }
            Text(label, style = FxType.titleOnImage)
        }
    }
}

/** Пунктирная рамка по форме плашки. Рисуется под содержимым — рамка фокуса ложится поверх. */
private fun Modifier.dashedOutline(): Modifier = drawBehind {
    val stroke = DashWidth.toPx()
    val radius = CornerRadius(FolderRadius.toPx())
    drawRoundRect(
        color = Color.White.copy(alpha = DASH_ALPHA),
        topLeft = Offset(stroke / 2, stroke / 2),
        size = Size(size.width - stroke, size.height - stroke),
        cornerRadius = radius,
        style = Stroke(
            width = stroke,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(DashOn.toPx(), DashOff.toPx())),
        ),
    )
}
