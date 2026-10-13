package com.filmax.core.tv.designsystem

import androidx.compose.ui.graphics.Color

// ── Цвета экранов, ещё не переведённых на FxColors ───────────────────────────────
// Редизайн «Filmax TV» сменил монохром на палитру (FxColors.kt). Экраны переезжают на
// LocalFxColors волнами (docs/tv-redesign.md), а пока читают эти токены — они уже в цветах
// «Фиолета». Поверхности здесь непрозрачные: стекло FxColors.surface, наложенное на фон
// Violet.background, — старые экраны кладут поверхности друг на друга, и полупрозрачные
// складывались бы. Когда последний экран переедет на LocalFxColors, файл уходит.

/** Подложка экрана — фон палитры. */
val TvSurface = Violet.background

/** Карточки, панели, поля ввода: стекло 7% поверх фона. */
val TvSurfaceContainer = Color(0xFF201D2A)

/** Чипы, вторичные кнопки, группы настроек: стекло 12% поверх фона. */
val TvSurfaceContainerHigh = Color(0xFF2C2935)

/** Приподнятые элементы, разделители, аватар-плейсхолдер: стекло 18% поверх фона. */
val TvSurfaceContainerHighest = Color(0xFF3A3843)

/** Основной текст. */
val TvOnSurface = Violet.text

/** Вторичный текст: мета, подписи, плейсхолдеры. */
val TvOnSurfaceVariant = Violet.textSecondary

/** Третичный текст — надзаголовки секций, служебные подписи. */
val TvOnSurfaceDim = Violet.textDim

val TvOutlineVariant = Violet.outline

/** Акцент палитры: главное действие, текущий раздел, выбранный чип, полоса прогресса. */
val TvAccent = Violet.accent

/** Контент на акцентной заливке. */
val TvOnAccent = Violet.onAccent

// ── Ошибки ───────────────────────────────────────────────────────────────────
val TvError = Violet.error
val TvErrorContainer = Color(0xFF3A1A22)

// ── Фокус ────────────────────────────────────────────────────────────────────
/** Рамка фокуса — белая на любой палитре: её узнают раньше, чем цвет свечения. */
val TvFocus = Color.White

/** Свечение вокруг рамки фокуса — цвет палитры. */
val TvFocusHalo = Violet.glow
