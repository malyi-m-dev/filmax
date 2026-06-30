package com.filmax.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.filmax.core.domain.catalog.model.Item

/**
 * Палитра и фабрики backdrop-градиентов деталей. Раньше эти цвета и градиенты были
 * захардкожены и продублированы в mobile- и tv-экранах деталей; собраны здесь, чтобы
 * существовал единственный источник правды по цветам затемнения бэкдропа.
 *
 * Цвета затемнения зависят от темы (уходят в [Color] поверхности), поэтому фабрики
 * принимают `surface` параметром — он читается на стороне вызова из `MaterialTheme`.
 */
object BackdropGradients {

    /** Акцент постера-заглушки бэкдропа (тот же, что у обоих экранов деталей). */
    val Accent: Color = Color(0xFFB4305A)

    /** Верхний тёмный оттенок вертикального градиента бэкдропа. */
    private val ScrimTop: Color = Color(0xFF141012)

    /** Цвет затемнения-скрима, нарастающего при сворачивании героя (mobile). */
    private val ScrimDark: Color = Color(0xFF0A0809)

    /**
     * Mobile: вертикальный градиент — лёгкое затемнение сверху и плавный уход в [surface] снизу.
     * Прозрачный в середине, чтобы под ним был виден сам постер.
     */
    fun mobileVertical(surface: Color): Brush = Brush.verticalGradient(
        0f to ScrimTop.copy(alpha = 0.3f),
        0.3f to Color.Transparent,
        0.7f to Color.Transparent,
        1f to surface,
    )

    /** TV: горизонтальный градиент — затемнение в [surface] слева, прозрачность справа. */
    fun tvHorizontal(surface: Color): Brush = Brush.horizontalGradient(
        0f to surface,
        0.5f to surface.copy(alpha = 0.6f),
        0.85f to Color.Transparent,
    )

    /** TV: вертикальный градиент — нижний уход в [surface]. */
    fun tvVerticalBottom(surface: Color): Brush = Brush.verticalGradient(
        0.45f to Color.Transparent,
        1f to surface,
    )

    /**
     * Mobile: цвет затемнения-скрима, нарастающего по прогрессу сворачивания [progress] (0..1).
     * Накладывается отдельным слоем поверх [HeroBackdrop] на стороне mobile-экрана.
     */
    fun collapseScrim(progress: Float): Color = ScrimDark.copy(alpha = progress * 0.55f)
}

/**
 * Общий бэкдроп героя деталей: постер на всю область + слои градиентов [scrims] поверх него.
 *
 * Намеренно «тонкий» и платформо-нейтральный — содержит ТОЛЬКО реально дублировавшуюся часть
 * (постер + базовые градиенты затемнения). Платформенные надстройки остаются на местах вызова:
 * mobile поверх кладёт collapse-скрим и info-оверлей и оборачивает в parallax-`graphicsLayer`,
 * tv — свой layout-контент. Слои рисуются в порядке: постер, затем [scrims] в порядке списка.
 *
 * URL постера передаётся явно ([posterUrl]), т.к. экраны выбирают разный кадр: mobile — `big`,
 * tv — широкий `wide` с откатом на `big`.
 */
@Composable
fun HeroBackdrop(
    item: Item,
    scrims: List<Brush>,
    modifier: Modifier = Modifier,
    posterUrl: String = item.posters.big,
    accentColor: Color = BackdropGradients.Accent,
) {
    Box(modifier) {
        PosterImage(
            url = posterUrl,
            contentDescription = item.title,
            modifier = Modifier.matchParentSize(),
            shape = RoundedCornerShape(0.dp),
            accentColor = accentColor,
        )
        scrims.forEach { brush ->
            Box(Modifier.matchParentSize().background(brush))
        }
    }
}
