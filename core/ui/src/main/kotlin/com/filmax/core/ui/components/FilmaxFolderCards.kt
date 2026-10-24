package com.filmax.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxColors
import com.filmax.core.designsystem.FilmaxTheme
import com.filmax.core.designsystem.FilmaxType
import com.filmax.core.designsystem.angledGradient
import com.filmax.core.designsystem.glow
import com.filmax.core.designsystem.oklch

/**
 * Оттенки папок по номеру цвета, который хранит домен (FolderAppearance.tint): фиолетовый, синий,
 * красный, зелёный, розовый, янтарный — тот же порядок, что у ТВ, поэтому папка одного цвета на
 * обоих экранах. Сами цвета считаются из оттенка формулами макета.
 */
val FolderHues = listOf(290f, 250f, 25f, 150f, 350f, 75f)

/** Оттенок папки; номер вне диапазона заворачивается по кругу, а не роняет экран. */
fun folderHue(tint: Int): Float = FolderHues[tint.mod(FolderHues.size)]

/** Кружок выбора цвета папки — самая светлая ступень оттенка, читается на тёмной шторке. */
fun folderSwatch(hue: Float): Color = oklch(lightness = 0.66f, chroma = 0.18f, hue = hue)

/** Плитка-иконка папки в списке «В папку»: полупрозрачная подложка и светлая иконка её оттенка. */
@Composable
fun FilmaxFolderIcon(hue: Float, modifier: Modifier = Modifier, icon: ImageVector = Icons.Filled.Folder) {
    Box(
        modifier
            .size(44.dp)
            .clip(FolderIconShape)
            .background(oklch(lightness = 0.6f, chroma = 0.18f, hue = hue, alpha = 0.35f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = oklch(lightness = 0.86f, chroma = 0.12f, hue = hue),
            modifier = Modifier.size(22.dp),
        )
    }
}

/**
 * Карточка папки в «Моё»: веер из трёх постеров на плашке её оттенка, название и счётчик. Пустая
 * папка — без веера: обещать содержимое, которого нет, незачем.
 */
// Компонент дизайн-системы: параметры — его публичный API (см. FilmaxButton).
@Suppress("LongParameterList")
@Composable
fun FilmaxFolderCard(
    name: String,
    count: String,
    hue: Float,
    posters: List<String>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .height(FolderCardHeight)
            .clip(FolderCardShape)
            .background(
                angledGradient(
                    angle = 160f,
                    0f to oklch(lightness = 0.5f, chroma = 0.17f, hue = hue, alpha = 0.55f),
                    1f to oklch(lightness = 0.3f, chroma = 0.1f, hue = hue, alpha = 0.35f),
                ),
            )
            .border(1.dp, oklch(lightness = 0.8f, chroma = 0.1f, hue = hue, alpha = 0.22f), FolderCardShape)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        FilmaxPosterFan(posters, Modifier.align(Alignment.TopCenter).padding(top = 20.dp))
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(14.dp),
        ) {
            Text(
                name,
                style = FilmaxType.itemTitle,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(count, style = FilmaxType.metaSmall, color = Color.White.copy(alpha = 0.72f))
        }
    }
}

/**
 * Веер постеров: первый — по центру поверх остальных, второй справа, третий слева, оба повёрнуты
 * на ±[angle]. [centerWidth] и [sideWidth] — ширина центрального и крайних (высота — 2:3): у папки
 * они равны, у подборки центр крупнее — он и есть обложка.
 */
// Компонент дизайн-системы: параметры — его публичный API (см. FilmaxButton).
@Suppress("LongParameterList")
@Composable
fun FilmaxPosterFan(
    posters: List<String>,
    modifier: Modifier = Modifier,
    centerWidth: Dp = FanPosterWidth,
    sideWidth: Dp = FanPosterWidth,
    angle: Float = FAN_ANGLE,
    step: Dp = FanStep,
) {
    val fan = posters.take(FAN_SIZE)
    // Пары «номер постера → место в веере» в порядке отрисовки: центр рисуется последним.
    val placements = when (fan.size) {
        0 -> emptyList()
        1 -> listOf(0 to 0)
        2 -> listOf(1 to 1, 0 to 0)
        else -> listOf(2 to -1, 1 to 1, 0 to 0)
    }
    Box(modifier, contentAlignment = Alignment.TopCenter) {
        placements.forEach { (index, slot) ->
            val width = if (slot == 0) centerWidth else sideWidth
            val shape = RoundedCornerShape(width * FAN_CORNER_SHARE)
            PosterImage(
                url = fan[index],
                contentDescription = null,
                shape = shape,
                modifier = Modifier
                    .padding(top = (centerWidth - width) / 2)
                    .offset(x = step * slot)
                    .rotate(angle * slot)
                    .shadow(10.dp, shape)
                    .size(width, width / POSTER_ASPECT),
            )
        }
    }
}

/**
 * «Создать папку» — первая карточка сетки папок: пунктирная рамка и круглая акцентная кнопка.
 * Пунктир отличает её от настоящих папок с первого взгляда.
 */
@Composable
fun FilmaxCreateFolderCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = FilmaxTheme.palette
    Column(
        modifier
            .height(FolderCardHeight)
            .clip(FolderCardShape)
            .dashedBorder(DashedColor, FolderCardRadius)
            .clickable(role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Box(
            Modifier
                .size(52.dp)
                .glow(palette.glow, CircleShape, blur = 24.dp, spread = 0.dp, offsetY = 6.dp)
                .clip(CircleShape)
                .background(palette.accent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = palette.onAccent, modifier = Modifier.size(26.dp))
        }
        Text("Создать папку", style = FilmaxType.chip, color = FilmaxColors.Text)
    }
}

/** Пунктирная рамка 2dp — у «Создать папку» и «Новая папка» в шторке. */
fun Modifier.dashedBorder(color: Color, radius: Dp): Modifier = drawBehind {
    val stroke = 2.dp.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(stroke / 2, stroke / 2),
        size = Size(size.width - stroke, size.height - stroke),
        cornerRadius = CornerRadius(radius.toPx()),
        style = Stroke(
            width = stroke,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(DASH * density, GAP * density))
        ),
    )
}

private const val FAN_SIZE = 3
private const val FAN_ANGLE = 10f
private const val DASH = 6f
private const val GAP = 5f
private val FanStep = 34.dp
private val FanPosterWidth = 70.dp
private const val FAN_CORNER_SHARE = 0.14f
private val FolderCardHeight = 208.dp
private val FolderCardRadius = 24.dp
private val FolderCardShape = RoundedCornerShape(FolderCardRadius)
private val FolderIconShape = RoundedCornerShape(14.dp)
private val DashedColor = Color.White.copy(alpha = 0.22f)

/** Кружок выбора цвета папки: выбранный обведён белым кольцом через зазор цвета фона. */
@Composable
fun FilmaxColorSwatch(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val background = FilmaxTheme.palette.background
    Box(
        modifier
            .size(SwatchSize)
            .clip(CircleShape)
            .then(if (selected) Modifier.border(2.dp, Color.White, CircleShape) else Modifier)
            .padding(if (selected) 2.dp else 0.dp)
            .then(if (selected) Modifier.border(3.dp, background, CircleShape) else Modifier)
            .clip(CircleShape)
            .background(color)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
    )
}

/** Строка «Новая папка» в шторке выбора: пунктирная плитка с «+» и подпись акцентом. */
@Composable
fun FilmaxNewFolderRow(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accent = FilmaxTheme.palette.accentText
    Row(
        modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(44.dp).dashedBorder(DashedColor, 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
        }
        Text("Новая папка", style = FilmaxType.itemTitle, color = accent)
    }
}

private val SwatchSize = 36.dp
