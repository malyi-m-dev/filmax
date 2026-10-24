package com.filmax.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Палитра Filmax — одна на телефон и ТВ: главное действие, текущий раздел, прогресс и свечение —
 * цветом [accent], рейтинги и аватар — [accent2]. Палитру выбирают в Профиле целиком: акценты и
 * фон подобраны друг к другу, текст и стекло общие для всех трёх. Значения — перевод из oklch
 * макетов «Filmax TV» и «Mobile redesign».
 */
@Immutable
data class FilmaxPalette(
    val accent: Color,
    val onAccent: Color,
    /** Второй акцент — рейтинги и аватар. */
    val accent2: Color,
    val onAccent2: Color,
    /** Акцент для текста на тёмном: надзаголовки, подсветка совпадений, выбранный вариант. */
    val accentText: Color,
    /** Свечение под главной кнопкой и выбранной вкладкой — акцент с прозрачностью. */
    val glow: Color,
    /** Цветная подсветка сверху экрана и под hero. */
    val wash: Color,
    val background: Color,
)

/** Три палитры приложения. По умолчанию — [Violet]. */
object FilmaxPalettes {

    /** «Фиолет». */
    val Violet = FilmaxPalette(
        accent = Color(0xFF7B5CF5),
        onAccent = Color.White,
        accent2 = Color(0xFFE14BA0),
        onAccent2 = Color.White,
        accentText = Color(0xFFBEB2FF),
        glow = Color(0xA67B5CF5),
        wash = Color(0x669B4FD8),
        background = Color(0xFF0F0C1A),
    )

    /** «Коралл». */
    val Coral = FilmaxPalette(
        accent = Color(0xFFF26B4C),
        onAccent = Color(0xFF1A0B06),
        accent2 = Color(0xFFF0B53C),
        onAccent2 = Color(0xFF1A1206),
        accentText = Color(0xFFFFB59E),
        glow = Color(0x99F26B4C),
        wash = Color(0x61E0603C),
        background = Color(0xFF170F0C),
    )

    /** «Мята». */
    val Mint = FilmaxPalette(
        accent = Color(0xFF3FDBB2),
        onAccent = Color(0xFF04140F),
        accent2 = Color(0xFF4D98EE),
        onAccent2 = Color.White,
        accentText = Color(0xFF93F0D2),
        glow = Color(0x8C3FDBB2),
        wash = Color(0x663D8FC7),
        background = Color(0xFF0A1218),
    )
}
