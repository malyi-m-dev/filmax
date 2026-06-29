package com.filmax.core.tv.designsystem

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ── TV surfaces ───────────────────────────────────────────────────────────────
// Чуть темнее мобильных — рассчитаны на просмотр с дивана (10-foot), значения из
// макета «Filmax TV — Все экраны».
val TvSurface = Color(0xFF0A0809)
val TvSurfaceContainer = Color(0xFF1A1518)
val TvSurfaceContainerHigh = Color(0xFF221D20)
val TvSurfaceContainerHighest = Color(0xFF2E2729)
val TvOnSurfaceVariant = Color(0xFFC5B3B9)
val TvOutlineVariant = Color(0xFF3A2F33)

// ── Focus highlight ─────────────────────────────────────────────────────────
// Фирменная TV-аффорданс: жёлтая обводка вокруг сфокусированного элемента (D-pad).
val TvFocus = Color(0xFFFFD466)

/** Цвет focus-обводки текущей темы. Доступен через [androidx.compose.runtime.CompositionLocal]. */
val LocalTvFocusColor = staticCompositionLocalOf { TvFocus }
