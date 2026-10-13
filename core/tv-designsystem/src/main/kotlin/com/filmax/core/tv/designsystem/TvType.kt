package com.filmax.core.tv.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Onest — заголовки, кнопки, чипы, названия карточек. Статические начертания 500–800, ужатые до
 * латиницы и кириллицы (лицензия OFL — LICENSE-Onest.txt модуля).
 */
val Onest = FontFamily(
    Font(R.font.onest_medium, FontWeight.Medium),
    Font(R.font.onest_semibold, FontWeight.SemiBold),
    Font(R.font.onest_bold, FontWeight.Bold),
    Font(R.font.onest_extrabold, FontWeight.ExtraBold),
)

/**
 * Шкала редизайна «Filmax TV»: Onest несёт характер, Roboto — текст и мету. Размеры 1:1 из
 * макета (960×540dp). Мельче 12sp ТВ не опускается — исключение только надзаголовок 11sp в
 * верхнем регистре с трекингом, он читается как метка, а не как текст.
 */
object FxType {
    val hero = TextStyle(
        fontFamily = Onest,
        fontWeight = FontWeight.Bold,
        fontSize = 46.sp,
        lineHeight = 47.sp,
        letterSpacing = (-0.02).em,
    )
    val screenTitle = TextStyle(
        fontFamily = Onest,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.02).em,
    )
    val rowTitle = TextStyle(fontFamily = Onest, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 22.sp)
    val cardTitle =
        TextStyle(fontFamily = Onest, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 17.sp)
    val button = TextStyle(fontFamily = Onest, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp)
    val chip = TextStyle(fontFamily = Onest, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 17.sp)

    /** Номер в топе на постере. */
    val rank = TextStyle(
        fontFamily = Onest,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 44.sp,
        lineHeight = 44.sp,
        shadow = Shadow(color = Color.Black.copy(alpha = 0.6f), offset = Offset(0f, 2f), blurRadius = 12f),
    )

    /** Название на карточке подборки. */
    val collectionTitle =
        TextStyle(fontFamily = Onest, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 20.sp)

    /** Часы в углу Главной. */
    val clock = TextStyle(fontFamily = Onest, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 18.sp)

    /** Пилюля рейтинга. */
    val pill = TextStyle(fontFamily = Onest, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 16.sp)

    /** Подпись поверх кадра 16:9 — карточка всего 130dp высотой, крупнее не помещается. */
    val titleOnImage =
        TextStyle(fontFamily = Onest, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp)
    val metaOnImage = TextStyle(fontSize = 12.sp, lineHeight = 16.sp)

    /** Roboto, только в верхнем регистре. */
    val overline =
        TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.16.em)
    val body = TextStyle(fontSize = 15.sp, lineHeight = 22.5.sp)
    val meta = TextStyle(fontSize = 13.sp, lineHeight = 17.sp)

    /** Мета-строка hero и деталей: жанры, год, длительность. */
    val metaRow = TextStyle(fontSize = 14.sp, lineHeight = 20.sp)
}

/**
 * Слоты Material-типографики ТВ на шкале [FxType]: экраны, ещё не переведённые на FxType
 * напрямую, получают новые шрифты и размеры через MaterialTheme.typography.
 */
val FilmaxTvTypography = Typography(
    displayLarge = FxType.hero,
    // Название тайтла в hero Главной.
    displayMedium = FxType.hero,
    // Название в деталях — на ступень меньше hero Главной.
    displaySmall = FxType.hero.copy(fontSize = 40.sp, lineHeight = 42.sp),
    // Заголовок раздела («Моё», «Каталог»).
    headlineMedium = FxType.screenTitle,
    // Название в плеере, заголовок ошибки.
    headlineSmall = TextStyle(fontFamily = Onest, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 28.sp),
    // Заголовок ряда.
    titleLarge = FxType.rowTitle,
    // Строка настройки, пункт меню.
    titleMedium = TextStyle(fontFamily = Onest, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    // Кнопка, название карточки «продолжить».
    titleSmall = FxType.button,
    // Описание, мета-строка.
    bodyLarge = FxType.body,
    bodyMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 18.sp),
    // Мета под названием, длительность серии.
    bodySmall = FxType.meta,
    // Чип.
    labelLarge = FxType.chip,
    // Надзаголовок секции — в верхнем регистре с трекингом.
    labelMedium = FxType.overline,
    labelSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp),
)
