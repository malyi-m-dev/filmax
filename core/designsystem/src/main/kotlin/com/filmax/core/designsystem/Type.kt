package com.filmax.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Onest — заголовки, кнопки, чипы, названия карточек на телефоне и ТВ. Статические начертания
 * 500–800, ужатые до латиницы и кириллицы (лицензия OFL — LICENSE-Onest.txt модуля).
 */
val Onest = FontFamily(
    Font(R.font.onest_medium, FontWeight.Medium),
    Font(R.font.onest_semibold, FontWeight.SemiBold),
    Font(R.font.onest_bold, FontWeight.Bold),
    Font(R.font.onest_extrabold, FontWeight.ExtraBold),
)

/**
 * Шкала мобильного редизайна: Onest несёт характер — заголовки, кнопки, чипы, названия; Roboto —
 * текст и мету. Размеры 1:1 из макета «Mobile redesign» (кадр 412dp). Ролей больше, чем слотов
 * Material-типографики, поэтому экраны берут стиль отсюда по смыслу, а [FilmaxTypography] лишь
 * раздаёт эту же шкалу стандартным компонентам.
 */
object FilmaxType {
    /** Название тайтла в hero Главной. */
    val hero = onest(FontWeight.Bold, size = 38, line = 39, tracking = -0.025f)

    /** Название на экране деталей. */
    val title = onest(FontWeight.Bold, size = 34, line = 35, tracking = -0.02f)

    /** Название открытой папки, распознанная фраза голосового поиска. */
    val headline = onest(FontWeight.Bold, size = 32, line = 36, tracking = -0.02f)

    /** Заголовок раздела: «Каталог», «Моё». */
    val screenTitle = onest(FontWeight.Bold, size = 30, line = 34, tracking = -0.02f)

    /** Название подборки в её шапке. */
    val collectionTitle = onest(FontWeight.Bold, size = 28, line = 31, tracking = -0.015f)

    /** Имя в Профиле. */
    val profileName = onest(FontWeight.Bold, size = 24, line = 28)

    /** Заголовок шторки, логотип в шапке Главной. */
    val sheetTitle = onest(FontWeight.Bold, size = 22, line = 26)
    val logo = onest(FontWeight.ExtraBold, size = 22, line = 26, tracking = -0.03f)

    /** Логотип на экране входа — там он один на экране. */
    val logoLarge = onest(FontWeight.ExtraBold, size = 30, line = 34, tracking = -0.03f)

    /** Название на карточке подборки. */
    val cardHeading = onest(FontWeight.Bold, size = 18, line = 22)

    /** Заголовок ряда: «Продолжить просмотр», «Похожие». */
    val sectionTitle = onest(FontWeight.SemiBold, size = 19, line = 24, tracking = -0.01f)

    /** Название в плеере, заголовок дня в истории. */
    val playerTitle = onest(FontWeight.SemiBold, size = 17, line = 22)

    /** Главная кнопка, название в результатах поиска. */
    val button = onest(FontWeight.SemiBold, size = 16, line = 20)

    /** Название серии, папки, строки истории. */
    val itemTitle = onest(FontWeight.SemiBold, size = 15, line = 20)

    /** Чип, вкладка, название карточки «Продолжить», ссылка «Читать полностью». */
    val chip = onest(FontWeight.SemiBold, size = 14, line = 18)

    /** Название под постером, мелкий чип. */
    val cardTitle = onest(FontWeight.SemiBold, size = 13, line = 17)

    /** Имя актёра под фото. */
    val caption = onest(FontWeight.SemiBold, size = 12, line = 15)

    /** Пилюля рейтинга, бейдж «5.1». */
    val pill = onest(FontWeight.Bold, size = 12, line = 14)

    /** Цифра «Топ-10» — контурная, позади постера. */
    val rank = onest(FontWeight.ExtraBold, size = 168, line = 168, tracking = -0.04f)

    /** Значение скорости в панели плеера. */
    val bigValue = onest(FontWeight.Bold, size = 50, line = 54, tracking = -0.02f)

    // ── Roboto ──────────────────────────────────────────────────────────────
    /** Текст в поле поиска. */
    val field = roboto(FontWeight.Normal, size = 17, line = 22)

    /** Пункт подсказки поиска. */
    val bodyLarge = roboto(FontWeight.Normal, size = 16, line = 22)

    /** Описание, абзацы шторки. */
    val body = roboto(FontWeight.Normal, size = 15, line = 22.5f)

    /** Подпись строки настроек и варианта дорожки. */
    val label = roboto(FontWeight.Medium, size = 15, line = 20)

    /** Почта в Профиле, значение настройки, факт «О фильме». */
    val bodySmall = roboto(FontWeight.Normal, size = 14, line = 20)

    /** Мета под названием: «Сериал · 2019–2024», счётчик ряда. */
    val meta = roboto(FontWeight.Normal, size = 13, line = 18)

    /** Мелкая мета под карточкой, длительность серии. */
    val metaSmall = roboto(FontWeight.Normal, size = 12, line = 16)

    /** Подпись плитки действия и вкладки панели плеера, бейдж «12+». */
    val tag = roboto(FontWeight.Medium, size = 12, line = 16)

    /** Подпись над полем ввода: «Название», «Цвет». */
    val fieldLabel = roboto(FontWeight.SemiBold, size = 12, line = 16)

    /** Надзаголовок — только в верхнем регистре с трекингом. */
    val overline = roboto(FontWeight.SemiBold, size = 12, line = 16, tracking = 0.14f)

    /** Субтитр поверх видео. */
    val subtitle = roboto(FontWeight.Medium, size = 20, line = 27)
}

private fun onest(weight: FontWeight, size: Int, line: Int, tracking: Float = 0f) = TextStyle(
    fontFamily = Onest,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.em,
)

private fun roboto(weight: FontWeight, size: Int, line: Number, tracking: Float = 0f) = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.toFloat().sp,
    letterSpacing = tracking.em,
)

/**
 * Слоты Material-типографики на шкале [FilmaxType]: стандартные компоненты (поле ввода, шторка,
 * диалог) и `Text` без стиля получают те же шрифты, что и экраны.
 */
val FilmaxTypography = Typography(
    displayLarge = FilmaxType.hero,
    displayMedium = FilmaxType.title,
    displaySmall = FilmaxType.screenTitle,
    headlineLarge = FilmaxType.headline,
    headlineMedium = FilmaxType.collectionTitle,
    headlineSmall = FilmaxType.profileName,
    titleLarge = FilmaxType.sheetTitle,
    titleMedium = FilmaxType.button,
    titleSmall = FilmaxType.itemTitle,
    bodyLarge = FilmaxType.body,
    bodyMedium = FilmaxType.bodySmall,
    bodySmall = FilmaxType.meta,
    labelLarge = FilmaxType.chip,
    labelMedium = FilmaxType.tag,
    labelSmall = FilmaxType.metaSmall,
)
