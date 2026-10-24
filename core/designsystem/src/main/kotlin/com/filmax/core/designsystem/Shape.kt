package com.filmax.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Формы. Во всём приложении их две: скруглённый прямоугольник и круг (аватары, круглые кнопки).
 * Радиусы — из макета «Mobile redesign»: чем крупнее элемент, тем мягче угол.
 */
val FilmaxShapes = Shapes(
    extraSmall = RoundedCornerShape(7.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** Постер 2:3 на карточках. */
val ShapePoster = RoundedCornerShape(14.dp)

/** Кадр 16:9: «Продолжить», кадры фильма. */
val ShapeCard = RoundedCornerShape(16.dp)

/** Превью серии, миниатюра в истории. */
val ShapeThumb = RoundedCornerShape(12.dp)

/** Плитка действия деталей, строка серии. */
val ShapeTile = RoundedCornerShape(20.dp)

/** Карточка подборки и группы настроек, плитка раздела. */
val ShapePanel = RoundedCornerShape(22.dp)

/** Верх шторки и боковая панель плеера. */
val ShapeSheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
val ShapeSidePanel = RoundedCornerShape(28.dp)

/** Пилюля рейтинга. */
val ShapePill = RoundedCornerShape(7.dp)

/** Бейдж «12+», «4K». */
val ShapeBadge = RoundedCornerShape(5.dp)

/** Кнопки, чипы, поля, навигация — полная пилюля. */
val ShapeFull = RoundedCornerShape(percent = 50)
