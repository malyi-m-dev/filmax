package com.filmax.core.tv.designsystem

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

private const val TRACK_ALPHA = 0.1f

/** Бегунок не короче этой доли дорожки: в длинной сетке он иначе превращается в точку. */
private const val MIN_THUMB_SHARE = 0.08f

/**
 * Индикатор прокрутки у правого края экрана: тонкая дорожка и бегунок акцентом. Размер задаёт
 * [modifier] (обычно 4×400dp). [thumbStart] и [thumbLength] — доли дорожки 0..1, [alpha] —
 * видимость. Все три читаются только при отрисовке: прокрутка не перекомпоновывает экран.
 */
@Composable
fun TvScrollBar(
    thumbStart: () -> Float,
    thumbLength: () -> Float,
    modifier: Modifier = Modifier,
    alpha: () -> Float = { 1f },
) {
    val accent = LocalFxColors.current.accent
    Spacer(
        modifier.drawBehind {
            val visibility = alpha().coerceIn(0f, 1f)
            if (visibility == 0f) return@drawBehind
            val radius = CornerRadius(size.width / 2)
            drawRoundRect(Color.White.copy(alpha = TRACK_ALPHA * visibility), cornerRadius = radius)
            val length = thumbLength().coerceIn(MIN_THUMB_SHARE, 1f) * size.height
            val top = thumbStart().coerceIn(0f, 1f) * (size.height - length)
            drawRoundRect(
                color = accent.copy(alpha = visibility),
                topLeft = Offset(0f, top),
                size = Size(size.width, length),
                cornerRadius = radius,
            )
        },
    )
}

/** Бегунок индикатора прокрутки: начало и длина в долях дорожки. */
data class ScrollThumb(val start: Float, val length: Float) {
    companion object {
        /** Всё помещается — бегунок во всю дорожку. */
        val Whole = ScrollThumb(start = 0f, length = 1f)
    }
}

/**
 * Бегунок ленивого списка из элементов разной высоты. [position] — номер первого видимого
 * элемента с дробной частью — сколько его уже уехало; длина — доля видимых элементов. Точной
 * высоты всего списка ленивый список не знает, а для подсказки «где я» хватает и такой оценки.
 */
fun scrollThumb(position: Float, visibleCount: Int, total: Int): ScrollThumb {
    if (total <= 0 || visibleCount >= total) return ScrollThumb.Whole
    val hidden = (total - visibleCount).toFloat()
    return ScrollThumb(start = (position / hidden).coerceIn(0f, 1f), length = visibleCount.toFloat() / total)
}
