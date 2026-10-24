package com.filmax.core.designsystem

import androidx.compose.ui.unit.dp

/**
 * Метрики мобильной раскладки. Значения — из макета «Mobile redesign» (кадр 412dp); сетки
 * считают ширину колонки от экрана, поэтому на узком телефоне карточки ужимаются, а не режутся.
 */
object FilmaxMetrics {

    /** Боковые поля экрана. */
    val ScreenPadding = 20.dp

    /** Поля сетки и плавающих панелей — у краёв стекла воздуха меньше, чем у текста. */
    val GridPadding = 16.dp

    /** Шаг между карточками в ряду. */
    val CardGap = 12.dp

    /** Воздух над заголовком ряда и под ним. */
    val SectionTop = 28.dp
    val SectionBottom = 12.dp

    // ── Карточки ────────────────────────────────────────────────────────────
    /** Постер 2:3 в горизонтальном ряду. */
    val PosterWidth = 128.dp
    val PosterHeight = 192.dp

    /** Кадр 16:9 с прогрессом — «Продолжить просмотр». */
    val ContinueWidth = 232.dp
    val ContinueHeight = 131.dp

    /** Карточка «Топ-10»: цифра позади постера. */
    val TopTenWidth = 184.dp
    val TopTenHeight = 204.dp

    /** Карточка подборки в ряду. */
    val CollectionWidth = 280.dp
    val CollectionHeight = 160.dp

    /** Кадр фильма в ряду «Кадры». */
    val StillWidth = 220.dp
    val StillHeight = 124.dp

    /** Колонки сеток: постеры по три, кадры и папки по две. */
    const val POSTER_COLUMNS = 3
    const val WIDE_COLUMNS = 2

    // ── Hero ───────────────────────────────────────────────────────────────
    val HomeHeroHeight = 560.dp
    val DetailsBackdropHeight = 520.dp

    /** Где начинается текст деталей — поверх затемнённого низа кадра. */
    val MovieContentTop = 300.dp
    val SeriesContentTop = 270.dp

    // ── Элементы ───────────────────────────────────────────────────────────
    /** Главная кнопка. Тап-цель ≥48dp. */
    val PrimaryButtonHeight = 52.dp

    /** Кнопка второго ряда: «Смотреть подряд», «Поделиться». */
    val CompactButtonHeight = 44.dp

    /** Круглые стеклянные кнопки: в шапке, поверх кадра, рядом с главной. */
    val IconButtonSmall = 40.dp
    val IconButtonSize = 44.dp
    val IconButtonLarge = 52.dp

    val ChipHeight = 36.dp
    val SegmentHeight = 36.dp
    val SearchFieldHeight = 56.dp
    val SettingsRowHeight = 56.dp

    /** Плавающая навигация: высота, отступ от низа и место, которое ей оставляет лента. */
    val NavBarHeight = 64.dp
    val NavBarBottom = 24.dp
    val NavBarClearance = 112.dp

    /** Плавающая панель «Смотреть» в деталях. */
    val ActionPanelHeight = 68.dp

    /** Высота строки шапки раздела («Каталог», «Моё»). */
    val HeaderRowHeight = 60.dp

    /** Круглые кнопки пульта: перемотка и серии — обычные, воспроизведение — крупная. */
    val RoundButtonSize = 56.dp
    val RoundButtonLargeSize = 76.dp

    /** Строка списка в две строки текста (телевизор: имя и адрес; папка: имя и счётчик). */
    val ListRowHeight = 64.dp

    /** Полоса прогресса на карточке. */
    val ProgressBarHeight = 4.dp
    val ProgressBarThin = 3.dp
}
