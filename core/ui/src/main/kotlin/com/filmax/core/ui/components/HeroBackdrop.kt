package com.filmax.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

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
 *
 * Палитра и фабрики градиентов затемнения вынесены в [BackdropGradients].
 */
@Composable
fun HeroBackdrop(
    posterUrl: String,
    contentDescription: String?,
    scrims: List<Brush>,
    modifier: Modifier = Modifier,
    accentColor: Color = BackdropGradients.Accent,
) {
    Box(modifier) {
        PosterImage(
            url = posterUrl,
            contentDescription = contentDescription,
            modifier = Modifier.matchParentSize(),
            shape = RoundedCornerShape(0.dp),
            accentColor = accentColor,
        )
        scrims.forEach { brush ->
            Box(Modifier.matchParentSize().background(brush))
        }
    }
}
