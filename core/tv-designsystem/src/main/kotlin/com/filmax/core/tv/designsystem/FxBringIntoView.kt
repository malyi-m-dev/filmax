// Показ элемента вместе с тем, что над ним: первая строка сетки возвращает шапку экрана.
@file:OptIn(ExperimentalFoundationApi::class)

package com.filmax.core.tv.designsystem

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewResponder
import androidx.compose.foundation.relocation.bringIntoViewResponder
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize

/**
 * Прокрутка к элементу показывает его целиком и ещё [above] пикселей над ним.
 *
 * Зачем: сетка с шапкой (поле поиска, чипы) прокручивается с опорной точкой
 * ([PivotBringIntoViewSpec]) — строка в фокусе встаёт на неё, шапка уезжает. Но первая строка
 * стоит прямо под шапкой: «вверх» со второй строки поставил бы её на опорную точку под обрезанной
 * шапкой. Помеченная этим модификатором, первая строка просит показать себя вместе с шапкой — блок
 * выше двух третей экрана, и сетка возвращается к самому началу.
 *
 * [above] читается в момент прокрутки — передайте лямбду над состоянием с измеренной шапкой.
 */
fun Modifier.bringIntoViewWithAbove(above: () -> Float): Modifier = composed {
    val responder = remember(above) { AboveResponder(above) }
    onSizeChanged { responder.size = it }.bringIntoViewResponder(responder)
}

private class AboveResponder(private val above: () -> Float) : BringIntoViewResponder {
    var size: IntSize = IntSize.Zero

    override fun calculateRectForParent(localRect: Rect): Rect =
        Rect(0f, -above(), size.width.toFloat(), size.height.toFloat())

    // Внутри элемента прокручивать нечего — он сам целиком в кадре.
    override suspend fun bringChildIntoView(localRect: () -> Rect?) = Unit
}
