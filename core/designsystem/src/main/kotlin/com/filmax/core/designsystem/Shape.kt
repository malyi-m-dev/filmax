package com.filmax.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Standard M3 shape scale + Filmax expressive shapes
val FilmaxShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

// ── Expressive asymmetric shapes ─────────────────────────────────────────────
// Matches CSS: border-radius: 28px 48px 28px 48px (TL TR BR BL)
val ShapeAsymA = RoundedCornerShape(
    topStart = 28.dp,
    topEnd = 48.dp,
    bottomEnd = 28.dp,
    bottomStart = 48.dp,
)

// Matches CSS: border-radius: 48px 28px 48px 28px
val ShapeAsymB = RoundedCornerShape(
    topStart = 48.dp,
    topEnd = 28.dp,
    bottomEnd = 48.dp,
    bottomStart = 28.dp,
)

// Approximates CSS: border-radius: 40% 60% 55% 45% / 50% 45% 55% 50%
val ShapeCookie = RoundedCornerShape(
    topStartPercent = 40,
    topEndPercent = 60,
    bottomEndPercent = 55,
    bottomStartPercent = 45,
)

val ShapeLg = RoundedCornerShape(28.dp)
val ShapeMd = RoundedCornerShape(20.dp)
val ShapeFull = RoundedCornerShape(percent = 50)
