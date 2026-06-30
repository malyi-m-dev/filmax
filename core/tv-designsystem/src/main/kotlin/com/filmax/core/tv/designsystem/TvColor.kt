package com.filmax.core.tv.designsystem

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ── TV-палитра ────────────────────────────────────────────────────────────────
// Совпадает с мобильной дизайн-системой (бренд rose `#B4305A`), поверхности чуть
// темнее под просмотр с дивана (10-foot). Значения — из макета «Filmax TV».
val TvPrimary = Color(0xFFFFB1C8)
val TvOnPrimary = Color(0xFF5E1133)
val TvPrimaryContainer = Color(0xFFB4305A)
val TvOnPrimaryContainer = Color(0xFFFFD9E2)

val TvSurface = Color(0xFF0A0809)
val TvSurfaceContainer = Color(0xFF1A1518)
val TvSurfaceContainerHigh = Color(0xFF221D20)
val TvSurfaceContainerHighest = Color(0xFF2E2729)

val TvOnSurface = Color(0xFFEFDFE3)
val TvOnSurfaceVariant = Color(0xFFC5B3B9)
val TvOutline = Color(0xFF9E8B91)
val TvOutlineVariant = Color(0xFF3A2F33)

val TvError = Color(0xFFFFB4AB)
val TvErrorContainer = Color(0xFF93000A)

// ── Focus highlight ─────────────────────────────────────────────────────────
// Фирменная TV-аффорданс: жёлтая обводка вокруг сфокусированного элемента (D-pad).
val TvFocus = Color(0xFFFFD466)

/** Цвет focus-обводки текущей темы. */
val LocalTvFocusColor = staticCompositionLocalOf { TvFocus }
