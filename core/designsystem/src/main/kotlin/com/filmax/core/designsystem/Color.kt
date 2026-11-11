package com.filmax.core.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver

/**
 * Цвета, общие для всех палитр. Поверхности — полупрозрачный белый поверх того, что под ними:
 * стеклянные панели лежат на кадре и фоне палитры, а не перекрывают их глухой плашкой.
 */
object FilmaxColors {
    /** Основной текст: тёплый белый, не #FFF — чистый белый «звенит» на тёмном фоне. */
    val Text = Color(0xFFF4F2FA)
    val TextSecondary = Text.copy(alpha = 0.72f)
    val TextDim = Text.copy(alpha = 0.5f)

    /** Ошибки и деструктив («Выйти») — тёплый красный, общий для всех палитр. */
    val Error = Color(0xFFF07A6A)

    /** Мелкое стекло: круглые кнопки и чипы поверх кадра и ленты. */
    val Glass = Color.White.copy(alpha = 0.13f)
    val GlassBorder = Color.White.copy(alpha = 0.14f)

    /** Обводка стеклянных панелей: шапки, навигации, шторки. */
    val PanelBorder = Color.White.copy(alpha = 0.12f)

    /** Карточка группы настроек и плитка-иконка строки. */
    val Card = Color.White.copy(alpha = 0.06f)
    val CardBorder = Color.White.copy(alpha = 0.08f)

    /** Поле ввода, контейнер сегментов, подложка выбранной строки. */
    val Field = Color.White.copy(alpha = 0.08f)

    /** Разделитель строк внутри группы. */
    val Divider = Color.White.copy(alpha = 0.07f)

    /** Незаполненная часть полосы прогресса. */
    val Track = Color.White.copy(alpha = 0.22f)

    /** Затемнение под шторкой — с фиолетовым подтоном фона, а не чистый чёрный. */
    val Scrim = Color(0x7306040E)

    /** Глухое затемнение под раскрытой боковой навигацией ТВ — тот же подтон. */
    val ScrimDeep = Color(0xFF05040A)

    /** Затемнение оверлея голосового ввода поверх размытого экрана (макет: rgba(6,5,12,.55)). */
    val ScrimVoice = Color(0x8C06050C)

    /** Тёмное стекло панели плеера: подписи вариантов читаются на любом кадре. */
    val PanelDark = Color(0xD112101C)

    /** Тёмная иконка на светлой заливке — ползунок громкости плеера. */
    val OnLight = Color(0xFF14111F)

    /** Нижний тон заглушки постера: нейтральный, как и вся палитра. */
    val PosterPlaceholder = Color(0xFF0F0F0F)

    /** Бэкдроп деталей: акцент заглушки и два тона затемнения (сверху и при сворачивании героя). */
    val BackdropAccent = Color(0xFFB4305A)
    val BackdropScrimTop = Color(0xFF141012)
    val BackdropScrimDark = Color(0xFF0A0809)
}

/**
 * Material-схема на палитре: стандартные компоненты (шторка, поле, диалог, снекбар) берут
 * акцент и фон выбранной палитры. Контейнеры — тот же полупрозрачный белый, но уже смешанный с
 * фоном: Material ждёт непрозрачные цвета, а шторка под стеклом должна оставаться читаемой.
 */
fun filmaxColorScheme(palette: FilmaxPalette): ColorScheme {
    val background = palette.background
    fun white(alpha: Float) = Color.White.copy(alpha = alpha).compositeOver(background)
    return darkColorScheme(
        primary = palette.accent,
        onPrimary = palette.onAccent,
        primaryContainer = palette.accent.copy(alpha = CONTAINER_ALPHA).compositeOver(background),
        onPrimaryContainer = palette.accentText,
        inversePrimary = palette.accentText,
        secondary = palette.accent2,
        onSecondary = palette.onAccent2,
        secondaryContainer = white(HIGH_ALPHA),
        onSecondaryContainer = FilmaxColors.Text,
        tertiary = palette.accentText,
        onTertiary = background,
        error = FilmaxColors.Error,
        onError = background,
        errorContainer = FilmaxColors.Error.copy(alpha = CONTAINER_ALPHA).compositeOver(background),
        onErrorContainer = FilmaxColors.Error,
        background = background,
        onBackground = FilmaxColors.Text,
        surface = background,
        onSurface = FilmaxColors.Text,
        surfaceVariant = white(HIGH_ALPHA),
        onSurfaceVariant = FilmaxColors.TextSecondary,
        surfaceTint = Color.Transparent,
        inverseSurface = FilmaxColors.Text,
        inverseOnSurface = background,
        outline = white(OUTLINE_ALPHA),
        outlineVariant = white(LOW_ALPHA),
        scrim = FilmaxColors.Scrim,
        surfaceBright = white(HIGHEST_ALPHA),
        surfaceDim = background,
        surfaceContainerLowest = background,
        surfaceContainerLow = white(LOWEST_ALPHA),
        surfaceContainer = white(LOW_ALPHA),
        surfaceContainerHigh = white(HIGH_ALPHA),
        surfaceContainerHighest = white(HIGHEST_ALPHA),
    )
}

private const val LOWEST_ALPHA = 0.04f
private const val LOW_ALPHA = 0.07f
private const val HIGH_ALPHA = 0.11f
private const val HIGHEST_ALPHA = 0.16f
private const val OUTLINE_ALPHA = 0.3f
private const val CONTAINER_ALPHA = 0.24f
