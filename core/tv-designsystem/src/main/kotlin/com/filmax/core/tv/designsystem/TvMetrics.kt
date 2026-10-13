package com.filmax.core.tv.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Формы TV. Только скруглённый прямоугольник и круг (аватары, иконки-кнопки).
 */
val FilmaxTvShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(22.dp),
)

/**
 * Метрики TV-раскладки. Проектируем в 960×540dp (1080p при density 2.0). Значения — из
 * редизайна «Filmax TV» (docs/tv-redesign.md).
 */
object TvMetrics {

    /**
     * Горизонтальное поле экранов без бокового меню — детали, плеер, подборка. Google: 5% от
     * 960dp = 48dp минимум, «чтобы контент был точно в безопасности» — 58dp.
     */
    val SafeHorizontal = 58.dp

    /** Поле слева у разделов с меню: свёрнутая колонка меню занимает 14..62dp, контент — с 80dp. */
    val SectionStart = 80.dp

    /** Поле справа у разделов. */
    val SectionEnd = 40.dp

    /** Вертикальное поле: 5% от 540dp ≈ 27dp, берём 28dp. */
    val SafeVertical = 28.dp

    /** Отступ контента разделов от верха — над ним только безопасное поле. */
    val ContentTop = 32.dp

    /** Шаг между карточками в ряду. */
    val CardGap = 18.dp

    /** Шаг между колонками и строками сетки каталога. */
    val GridGap = 20.dp

    /** Промежуток между рядами. */
    val RowGap = 36.dp

    /**
     * Запас вокруг сеток и рядов под увеличение карточки и свечение фокуса. Без него граница
     * клипа срезает рамку и свечение у крайних карточек.
     */
    val FocusInset = 16.dp

    // ── Карточки: два типа на всё приложение ────────────────────────────────
    /** Постер 2:3 в рядах. */
    val PosterWidth = 132.dp
    val PosterHeight = 198.dp

    /** Постер 2:3 в сетке каталога — крупнее, сетка просматривается целиком. */
    val GridPosterWidth = 152.dp
    val GridPosterHeight = 228.dp

    /** Карточка 16:9 с прогрессом — «продолжить смотреть», история. */
    val ContinueWidth = 230.dp
    val ContinueHeight = 130.dp

    /** Широкая карточка 16:9 — новинки. */
    val WideCardWidth = 300.dp
    val WideCardHeight = 169.dp

    /** Карточка подборки: цветная плашка с веером постеров. */
    val CollectionCardWidth = 260.dp
    val CollectionCardHeight = 140.dp

    /** Эпизод 16:9 — список серий. */
    val EpisodeWidth = 236.dp
    val EpisodeHeight = 133.dp

    /** Высота hero на главной и в деталях. */
    val HeroHeight = 384.dp
    val DetailsHeroHeight = 346.dp

    // ── Формы: только скруглённый прямоугольник и круг ──────────────────────
    val PosterShape = RoundedCornerShape(12.dp)
    val CardShape = RoundedCornerShape(12.dp)

    /** Строки и поля: строка настройки, строка поиска. */
    val PanelShape = RoundedCornerShape(14.dp)

    /** Панели и диалоги поверх экрана. */
    val SheetShape = RoundedCornerShape(22.dp)
    val ButtonShape = RoundedCornerShape(12.dp)
    val ChipShape = RoundedCornerShape(17.dp)

    // ── Фокус ──────────────────────────────────────────────────────────────
    /** Масштаб сфокусированной карточки или кнопки. */
    const val FocusScale = 1.06f

    /**
     * Масштаб широкого элемента — строки поиска, строки настроек, панели во всю ширину. 6% от
     * полутора тысяч пикселей — это ~45px с каждой стороны, строка наехала бы на соседей. Здесь
     * фокус держат рамка и свечение, а увеличение лишь намекает на него.
     */
    const val WideFocusScale = 1.02f

    /** Прозрачность соседей карточки в фокусе в том же ряду. */
    const val DimmedAlpha = 0.6f

    val FocusBorderWidth = 3.dp
}
