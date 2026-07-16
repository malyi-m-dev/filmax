package com.filmax.core.designsystem

import androidx.compose.ui.unit.dp

/**
 * Метрики мобильной раскладки. Значения — из макета «Filmax Mobile» (кадр 360dp).
 */
object FilmaxMetrics {

    /** Боковые поля экрана. */
    val ScreenPadding = 20.dp

    /** Поля на экране деталей — чуть шире: там длинный текст. */
    val DetailsPadding = 22.dp

    /** Шаг между карточками в ряду. */
    val CardGap = 12.dp

    /** Промежуток между рядами. */
    val RowGap = 20.dp

    // ── Карточки: два типа на всё приложение ────────────────────────────────
    /** Постер 2:3 в горизонтальном ряду («Новинки недели»). */
    val PosterWidth = 120.dp
    val PosterHeight = 180.dp

    /** Постер 2:3 в сетке каталога — три колонки на 360dp. */
    val GridPosterWidth = 98.dp
    val GridPosterHeight = 147.dp

    /** Постер 2:3 в ряду «Похожее». */
    val SimilarPosterWidth = 112.dp
    val SimilarPosterHeight = 168.dp

    /** Карточка 16:9 с прогрессом — «Продолжить» на главной. */
    val ContinueWidth = 204.dp
    val ContinueHeight = 115.dp

    /** Карточка 16:9 в сетке «Моё» — две колонки. */
    val MineCardWidth = 150.dp
    val MineCardHeight = 85.dp

    /** Превью серии в списке эпизодов. */
    val EpisodeThumbWidth = 120.dp
    val EpisodeThumbHeight = 68.dp

    // ── Hero ───────────────────────────────────────────────────────────────
    val HomeHeroHeight = 412.dp
    val DetailsHeroHeight = 340.dp
    val SeriesHeroHeight = 300.dp

    /**
     * Насколько заголовок деталей заезжает на hero. Отрицательный отступ — не трюк вёрстки:
     * текст должен лежать на затемнённом низе кадра, а не под ним.
     */
    val DetailsTitleOverlap = 52.dp
    val SeriesTitleOverlap = 46.dp

    // ── Элементы ───────────────────────────────────────────────────────────
    /** Высота главной кнопки. Тап-цель ≥48dp. */
    val PrimaryButtonHeight = 50.dp
    val SecondaryButtonHeight = 46.dp
    val SearchFieldHeight = 50.dp
    val SettingsRowHeight = 56.dp
    val ChipHeight = 36.dp
    val GenreChipHeight = 33.dp

    /** Круглая кнопка «назад» поверх hero. */
    val BackButtonSize = 38.dp

    /** Полоса прогресса на карточке. */
    val ProgressBarHeight = 3.dp
}
