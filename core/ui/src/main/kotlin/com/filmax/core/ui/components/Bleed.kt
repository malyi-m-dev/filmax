package com.filmax.core.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp

/**
 * Элемент ленты шире её полей на [horizontal] с каждой стороны — шапка подборки во всю ширину
 * экрана внутри сетки с полями. Отрицательного padding Compose не принимает, а вынести шапку из
 * сетки значило бы прокручивать её отдельно.
 */
fun Modifier.bleed(horizontal: Dp): Modifier = layout { measurable, constraints ->
    val extra = horizontal.roundToPx() * 2
    val placeable = measurable.measure(
        constraints.copy(minWidth = constraints.minWidth + extra, maxWidth = constraints.maxWidth + extra),
    )
    layout(constraints.maxWidth, placeable.height) { placeable.place(-extra / 2, 0) }
}
