package com.filmax.core.tv.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Формы TV. Только скруглённый прямоугольник — асимметричных и «печенек» в монохромном
 * редизайне нет. Круг задаётся точечно там, где он нужен (аватары).
 */
val FilmaxTvShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(16.dp),
)

/**
 * Метрики TV-раскладки. Проектируем в 960×540dp (1080p при density 2.0).
 *
 * До редизайна safe area не существовала как понятие: по экранам были захардкожены пять
 * разных горизонтальных отступов (48/56/72/80) и шесть вертикальных. Теперь — одна константа.
 */
object TvMetrics {

    /**
     * Горизонтальное поле. Google: 5% от 960dp = 48dp минимум, «чтобы контент был точно
     * в безопасности» — 58dp. Совпадает с полем 12-колоночной сетки (12×52 + 11×20 + 2×58 = 960).
     */
    val SafeHorizontal = 58.dp

    /** Вертикальное поле: 5% от 540dp ≈ 27dp, берём 28dp. */
    val SafeVertical = 28.dp

    /** Высота верхнего таб-бара. Контент под ним начинается с [ContentTop]. */
    val TopBarHeight = 64.dp

    /** Отступ контента от верха под таб-баром. */
    val ContentTop = 78.dp

    /** Шаг между карточками в ряду и между колонками сетки. */
    val CardGap = 18.dp

    /** Промежуток между рядами. */
    val RowGap = 24.dp

    /**
     * Запас вокруг сеток и рядов под увеличение карточки при фокусе. Без него граница
     * клипа срезает рамку фокуса у крайних карточек.
     */
    val FocusInset = 12.dp

    // ── Карточки: два типа на всё приложение ────────────────────────────────
    /** Постер 2:3 — ряды и сетки каталога. Один размер и в рядах, и в сетке. */
    val PosterWidth = 190.dp
    val PosterHeight = 285.dp

    /** Карточка 16:9 с прогрессом — «продолжить смотреть», история. */
    val ContinueWidth = 250.dp
    val ContinueHeight = 141.dp

    /** Эпизод 16:9 — список серий. */
    val EpisodeWidth = 236.dp
    val EpisodeHeight = 133.dp

    /** Высота hero на главной и в деталях. */
    val HeroHeight = 326.dp
    val DetailsHeroHeight = 346.dp

    // ── Формы: только скруглённый прямоугольник и круг ──────────────────────
    val PosterShape = RoundedCornerShape(8.dp)
    val CardShape = RoundedCornerShape(10.dp)
    val PanelShape = RoundedCornerShape(12.dp)
    val ButtonShape = RoundedCornerShape(9.dp)
    val ChipShape = RoundedCornerShape(20.dp)

    // ── Фокус ──────────────────────────────────────────────────────────────
    /** Масштаб сфокусированного элемента. Google допускает 1.025/1.05/1.1. */
    const val FocusScale = 1.08f

    /** Прозрачность несфокусированных карточек в ряду — монохромный аналог подсветки. */
    const val DimmedAlpha = 0.55f

    val FocusBorderWidth = 3.dp
    val FocusHaloWidth = 5.dp
}
