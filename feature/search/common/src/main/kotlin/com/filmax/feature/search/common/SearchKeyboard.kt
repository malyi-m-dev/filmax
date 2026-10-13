// Экранная клавиатура поиска на ТВ и правила набора: раскладки, ввод с пульта, подсветка
// совпадения в названии. Чистые функции без UI — их рисует ТВ-экран и проверяют тесты.

package com.filmax.feature.search.common

/** Клавиша пробела: на клавиатуре — отдельная широкая кнопка, в запросе — обычный пробел. */
const val SPACE_KEY = " "

/** Подписи раскладок в переключателе под клавиатурой. */
val KeyboardLayoutLabels = listOf(
    KeyboardLayout.RU to "RU",
    KeyboardLayout.EN to "EN",
    KeyboardLayout.DIGITS to "123",
)

/**
 * Ряды клавиш раскладки по восемь — сетка помещается в левую колонку экрана поиска. Последний
 * ряд короче: рядом с ним встают широкие «Пробел» и «Стереть».
 */
fun keyboardRows(layout: KeyboardLayout): List<List<String>> = when (layout) {
    KeyboardLayout.RU -> listOf("абвгдеёж", "зийклмно", "прстуфхц", "чшщъыьэю", "я")
    KeyboardLayout.EN -> listOf("abcdefgh", "ijklmnop", "qrstuvwx", "yz")
    KeyboardLayout.DIGITS -> listOf("12345678", "90")
}.map { row -> row.map(Char::toString) }

/**
 * Запрос после нажатия [key]. Пробел не ставится в начало и не удваивается: с пульта лишний
 * пробел не виден, а запрос с ним — уже другой запрос.
 */
fun typedQuery(query: String, key: String): String {
    if (key == SPACE_KEY && (query.isEmpty() || query.endsWith(SPACE_KEY))) return query
    return query + key
}

/**
 * Где в [title] совпадение с [query] — его подсвечивают в результатах. Без учёта регистра,
 * крайних пробелов и разницы «е/ё»: поиск kino.pub их не различает, и «шепот» находит «Шёпот» —
 * подсветка должна найти то же. null — совпадения в названии нет (тайтл нашёлся по актёру или
 * оригинальному названию).
 */
fun matchRange(title: String, query: String): IntRange? {
    val needle = query.trim().withoutYo()
    if (needle.isEmpty()) return null
    // Замена «ё» на «е» не меняет длину строки — индексы совпадения годятся для исходного названия.
    val start = title.withoutYo().indexOf(needle, ignoreCase = true)
    return if (start < 0) null else start until start + needle.length
}

private fun String.withoutYo(): String = replace('ё', 'е').replace('Ё', 'Е')
