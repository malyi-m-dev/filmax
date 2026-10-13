package com.filmax.core.ui.components

/**
 * Текст, распознанный по ходу речи. [confirmed] распознаватель уже не переиграет — его рисуют
 * в полную силу; [hypothesis] — догадка о последних словах, она ещё поменяется, поэтому бледнее.
 */
data class VoiceTranscript(val confirmed: String = "", val hypothesis: String = "") {
    val text: String get() = confirmed + hypothesis
}

/**
 * Следующий снимок распознавания: прежний частичный результат [previous] и новый [current].
 *
 * Подтверждённым считается то, что пережило обновление, — общее начало обеих гипотез, обрезанное
 * до целого слова: распознаватель переигрывает хвост фразы, а не её начало, и полслова «погра»
 * уверенным ещё не стало. [unstable] — хвост, который распознаватель сам пометил неустойчивым
 * (сервис Google отдаёт его отдельным полем): когда он есть, он точнее любой эвристики.
 */
fun nextTranscript(previous: String, current: String, unstable: String? = null): VoiceTranscript {
    if (!unstable.isNullOrBlank()) return VoiceTranscript(current, joinedTail(current, unstable))
    val stable = stableLength(previous, current)
    return VoiceTranscript(current.take(stable), current.drop(stable))
}

/** Сколько символов [current] подтверждено: общее начало с [previous] до границы слова. */
private fun stableLength(previous: String, current: String): Int {
    val common = previous.commonPrefixWith(current, ignoreCase = true).length
    if (common == current.length) return common
    // Слово закончилось, если после общего начала в обеих гипотезах — пробел или конец.
    val previousEnded = common == previous.length || previous[common] == WORD_SEPARATOR
    val wordEnded = current[common] == WORD_SEPARATOR && previousEnded
    return if (wordEnded) common else current.lastIndexOf(WORD_SEPARATOR, startIndex = common - 1) + 1
}

/** Хвост через пробел, если подтверждённая часть не кончается им сама. */
private fun joinedTail(confirmed: String, tail: String): String {
    val needsSpace = confirmed.isNotEmpty() && !confirmed.endsWith(WORD_SEPARATOR) && !tail.startsWith(WORD_SEPARATOR)
    return if (needsSpace) "$WORD_SEPARATOR$tail" else tail
}

private const val WORD_SEPARATOR = ' '
