package com.filmax.core.designsystem

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

// ── Монохромная палитра ───────────────────────────────────────────────────────
// Нейтральная ахроматичная лестница (R = G = B). Единственный цвет на экране — постер:
// интерфейс не соревнуется с контентом за внимание. Значения общие с TV-темой, кроме
// поверхностей: на телефоне смотрят с 30 см, и вторичный текст не нужно осветлять.

/** Подложка экрана. Не чистый чёрный: #000 проваливается на OLED и «звенит» на границах. */
val FilmaxSurface = Color(0xFF0A0A0A)
val FilmaxSurfaceDim = Color(0xFF0A0A0A)
val FilmaxSurfaceBright = Color(0xFF2E2E2E)
val FilmaxSurfaceContainerLowest = Color(0xFF050505)
val FilmaxSurfaceContainerLow = Color(0xFF111111)

/** Карточки, строки настроек, поле поиска. */
val FilmaxSurfaceContainer = Color(0xFF141414)

/** Чипы, вторичные кнопки, pill активной вкладки. */
val FilmaxSurfaceContainerHigh = Color(0xFF1F1F1F)

/** Аватар-плейсхолдер, разделители. */
val FilmaxSurfaceContainerHighest = Color(0xFF2E2E2E)

// ── Текст ─────────────────────────────────────────────────────────────────────
/** Основной текст. Не #FFFFFF: чистый белый на тёмном «вибрирует» и мылится. */
val FilmaxOnSurface = Color(0xFFE8E8E8)

/** Вторичный текст: мета, подписи, плейсхолдеры. */
val FilmaxOnSurfaceVariant = Color(0xFFA0A0A0)

/** Третичный: надзаголовки секций, служебные подписи. */
val FilmaxOnSurfaceDim = Color(0xFF8A8A8A)

val FilmaxOutline = Color(0xFF5A5A5A)
val FilmaxOutlineVariant = Color(0xFF1F1F1F)
val FilmaxInverseSurface = Color(0xFFE8E8E8)
val FilmaxInverseOnSurface = Color(0xFF141414)

// ── Акцент ────────────────────────────────────────────────────────────────────
/**
 * Акцент — чистый белый. Главное действие («Смотреть»), активная вкладка, прогресс.
 * Другого акцента в приложении нет: шесть цветных пресетов убраны вместе с редизайном.
 */
val FilmaxAccent = Color(0xFFFFFFFF)

/** Контент на акцентной заливке — почти-чёрный. */
val FilmaxOnAccent = Color(0xFF0A0A0A)

// ── Ошибки ────────────────────────────────────────────────────────────────────
// Единственное исключение из монохрома: ошибки и деструктив («Выйти»). Цвет при этом
// никогда не единственный носитель смысла — рядом всегда текст.
val FilmaxError = Color(0xFFE0736B)
val FilmaxOnError = Color(0xFF3A1512)
val FilmaxErrorContainer = Color(0xFF3A1512)
val FilmaxOnErrorContainer = Color(0xFFFFDAD6)

val FilmaxDarkColorScheme = darkColorScheme(
    primary = FilmaxAccent,
    onPrimary = FilmaxOnAccent,
    // primaryContainer — вторичная поверхность, а не цветная плашка: заливкой в монохроме
    // отмечается только выбор (чип) и главное действие (белая кнопка).
    primaryContainer = FilmaxSurfaceContainerHigh,
    onPrimaryContainer = FilmaxOnSurface,
    inversePrimary = FilmaxSurfaceContainerHighest,
    secondary = FilmaxOnSurfaceVariant,
    onSecondary = FilmaxOnAccent,
    secondaryContainer = FilmaxSurfaceContainerHigh,
    onSecondaryContainer = FilmaxOnSurface,
    tertiary = FilmaxOnSurfaceVariant,
    onTertiary = FilmaxOnAccent,
    tertiaryContainer = FilmaxSurfaceContainerHigh,
    onTertiaryContainer = FilmaxOnSurface,
    error = FilmaxError,
    onError = FilmaxOnError,
    errorContainer = FilmaxErrorContainer,
    onErrorContainer = FilmaxOnErrorContainer,
    surface = FilmaxSurface,
    onSurface = FilmaxOnSurface,
    surfaceVariant = FilmaxSurfaceContainerHigh,
    onSurfaceVariant = FilmaxOnSurfaceVariant,
    surfaceTint = FilmaxAccent,
    inverseSurface = FilmaxInverseSurface,
    inverseOnSurface = FilmaxInverseOnSurface,
    outline = FilmaxOutline,
    outlineVariant = FilmaxOutlineVariant,
    background = FilmaxSurface,
    onBackground = FilmaxOnSurface,
    surfaceBright = FilmaxSurfaceBright,
    surfaceDim = FilmaxSurfaceDim,
    surfaceContainerLowest = FilmaxSurfaceContainerLowest,
    surfaceContainerLow = FilmaxSurfaceContainerLow,
    surfaceContainer = FilmaxSurfaceContainer,
    surfaceContainerHigh = FilmaxSurfaceContainerHigh,
    surfaceContainerHighest = FilmaxSurfaceContainerHighest,
    scrim = Color(0xFF000000),
)
