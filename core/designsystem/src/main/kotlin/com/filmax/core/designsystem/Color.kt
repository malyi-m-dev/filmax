package com.filmax.core.designsystem

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

// ── Surfaces ──────────────────────────────────────────────────────────────────
val FilmaxSurface                  = Color(0xFF141012)
val FilmaxSurfaceDim               = Color(0xFF141012)
val FilmaxSurfaceBright            = Color(0xFF3C3539)
val FilmaxSurfaceContainerLowest   = Color(0xFF0E0B0D)
val FilmaxSurfaceContainerLow      = Color(0xFF1D1719)
val FilmaxSurfaceContainer         = Color(0xFF211B1D)
val FilmaxSurfaceContainerHigh     = Color(0xFF2B2527)
val FilmaxSurfaceContainerHighest  = Color(0xFF362F32)

// ── On-surfaces ───────────────────────────────────────────────────────────────
val FilmaxOnSurface                = Color(0xFFEFDFE3)
val FilmaxOnSurfaceVariant         = Color(0xFFD5C2C8)
val FilmaxOutline                  = Color(0xFF9E8B91)
val FilmaxOutlineVariant           = Color(0xFF514347)
val FilmaxInverseSurface           = Color(0xFFEFDFE3)
val FilmaxInverseOnSurface         = Color(0xFF362F32)

// ── Default accent: Deep Rose #B4305A ─────────────────────────────────────────
val FilmaxPrimary                  = Color(0xFFFFB1C8)
val FilmaxOnPrimary                = Color(0xFF5E1133)
val FilmaxPrimaryContainer         = Color(0xFFB4305A)
val FilmaxOnPrimaryContainer       = Color(0xFFFFD9E2)
val FilmaxInversePrimary           = Color(0xFF8C1547)

// ── Secondary ─────────────────────────────────────────────────────────────────
val FilmaxSecondary                = Color(0xFFE4BDC8)
val FilmaxOnSecondary              = Color(0xFF432933)
val FilmaxSecondaryContainer       = Color(0xFF5C3F49)
val FilmaxOnSecondaryContainer     = Color(0xFFFFD9E2)

// ── Tertiary (warm amber) ─────────────────────────────────────────────────────
val FilmaxTertiary                 = Color(0xFFF4B792)
val FilmaxOnTertiary               = Color(0xFF4F2500)
val FilmaxTertiaryContainer        = Color(0xFF713A10)
val FilmaxOnTertiaryContainer      = Color(0xFFFFDCC4)

// ── Error ─────────────────────────────────────────────────────────────────────
val FilmaxError                    = Color(0xFFFFB4AB)
val FilmaxOnError                  = Color(0xFF690005)
val FilmaxErrorContainer           = Color(0xFF93000A)
val FilmaxOnErrorContainer         = Color(0xFFFFDAD6)

val FilmaxDarkColorScheme = darkColorScheme(
    primary                = FilmaxPrimary,
    onPrimary              = FilmaxOnPrimary,
    primaryContainer       = FilmaxPrimaryContainer,
    onPrimaryContainer     = FilmaxOnPrimaryContainer,
    inversePrimary         = FilmaxInversePrimary,
    secondary              = FilmaxSecondary,
    onSecondary            = FilmaxOnSecondary,
    secondaryContainer     = FilmaxSecondaryContainer,
    onSecondaryContainer   = FilmaxOnSecondaryContainer,
    tertiary               = FilmaxTertiary,
    onTertiary             = FilmaxOnTertiary,
    tertiaryContainer      = FilmaxTertiaryContainer,
    onTertiaryContainer    = FilmaxOnTertiaryContainer,
    error                  = FilmaxError,
    onError                = FilmaxOnError,
    errorContainer         = FilmaxErrorContainer,
    onErrorContainer       = FilmaxOnErrorContainer,
    surface                = FilmaxSurface,
    onSurface              = FilmaxOnSurface,
    surfaceVariant         = FilmaxSurfaceContainerHigh,
    onSurfaceVariant       = FilmaxOnSurfaceVariant,
    surfaceTint            = FilmaxPrimary,
    inverseSurface         = FilmaxInverseSurface,
    inverseOnSurface       = FilmaxInverseOnSurface,
    outline                = FilmaxOutline,
    outlineVariant         = FilmaxOutlineVariant,
    background             = FilmaxSurface,
    onBackground           = FilmaxOnSurface,
    surfaceBright          = FilmaxSurfaceBright,
    surfaceDim             = FilmaxSurfaceDim,
    surfaceContainerLowest = FilmaxSurfaceContainerLowest,
    surfaceContainerLow    = FilmaxSurfaceContainerLow,
    surfaceContainer       = FilmaxSurfaceContainer,
    surfaceContainerHigh   = FilmaxSurfaceContainerHigh,
    surfaceContainerHighest = FilmaxSurfaceContainerHighest,
    scrim                  = Color(0xFF000000),
)

// ── Accent presets (accent switcher in TweakPanel) ────────────────────────────
data class AccentPreset(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
)

val AccentPresets = mapOf(
    Color(0xFFB4305A) to AccentPreset(Color(0xFFFFB1C8), Color(0xFF5E1133), Color(0xFFB4305A), Color(0xFFFFD9E2)),
    Color(0xFF6750A4) to AccentPreset(Color(0xFFD0BCFF), Color(0xFF381E72), Color(0xFF6750A4), Color(0xFFEADDFF)),
    Color(0xFFE46962) to AccentPreset(Color(0xFFFFB4AB), Color(0xFF690005), Color(0xFFE46962), Color(0xFFFFDAD6)),
    Color(0xFFFFB86B) to AccentPreset(Color(0xFFF4B792), Color(0xFF4F2500), Color(0xFFB15C0B), Color(0xFFFFDCC4)),
    Color(0xFF1E88E5) to AccentPreset(Color(0xFFA5CBFF), Color(0xFF00325E), Color(0xFF1E88E5), Color(0xFFD6E3FF)),
    Color(0xFF2E7D52) to AccentPreset(Color(0xFF8FD6B5), Color(0xFF003824), Color(0xFF2E7D52), Color(0xFFADF2CD)),
)
