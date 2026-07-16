package com.filmax.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Формы. Во всём приложении их две: скруглённый прямоугольник и круг (аватары).
 *
 * Асимметричные «экспрессивные» формы и «печенька» убраны: на одном экране их сходилось по
 * три-четыре штуки, и это читалось как шум, а не как система. Радиусы — из макета.
 */
val FilmaxShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(20.dp),
)

/** Постер 2:3 и кадр 16:9 на карточках. */
val ShapePoster = RoundedCornerShape(8.dp)

/** Карточка «продолжить», превью эпизода. */
val ShapeCard = RoundedCornerShape(11.dp)

/** Кнопки, поле поиска, строки настроек. */
val ShapeButton = RoundedCornerShape(13.dp)

/** Чипы и pill нижней навигации. */
val ShapeFull = RoundedCornerShape(percent = 50)
