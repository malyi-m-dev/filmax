// Раскладки экранной клавиатуры ТВ и правило набора с пульта. Чистые функции без UI: ряды рисует
// TvKeyboard из дизайн-системы, а выбранная раскладка и набранный текст живут в ScreenModel.
package com.filmax.core.presentation.input

/** Раскладка экранной клавиатуры: переключается кнопками под ней. */
enum class KeyboardLayout(val label: String) {
    RU("RU"),
    EN("EN"),
    DIGITS("123"),
}

/** Клавиша пробела: на клавиатуре — широкая кнопка, в тексте — обычный пробел. */
const val SPACE_KEY = " "

/**
 * Ряды клавиш по восемь — под сетку TvKeyboard. Последний ряд короче: рядом с ним встают
 * широкие «Пробел» и «Стереть».
 */
fun keyboardRows(layout: KeyboardLayout): List<List<String>> = when (layout) {
    KeyboardLayout.RU -> listOf("абвгдеёж", "зийклмно", "прстуфхц", "чшщъыьэю", "я")
    KeyboardLayout.EN -> listOf("abcdefgh", "ijklmnop", "qrstuvwx", "yz")
    KeyboardLayout.DIGITS -> listOf("12345678", "90")
}.map { row -> row.map(Char::toString) }

/**
 * Текст после нажатия [key]. Пробел не ставится в начало и не удваивается — с пульта лишний
 * пробел не виден. Первая буква — заглавная: названия папок и запросы начинаются с неё, а
 * переключателя регистра на ТВ-клавиатуре нет. Длиннее [maxLength] текст не растёт.
 */
fun typedText(text: String, key: String, maxLength: Int = Int.MAX_VALUE): String {
    if (key == SPACE_KEY && (text.isEmpty() || text.endsWith(SPACE_KEY))) return text
    val typed = if (text.isEmpty()) key.uppercase() else key
    return (text + typed).take(maxLength)
}

/** Текст после «Стереть»: минус последний символ. */
fun erasedText(text: String): String = text.dropLast(1)
