package com.filmax.core.ui.components

private const val HUNDRED = 100
private const val TEN = 10
private val TEENS = 11..14
private val FEW = 2..4

/**
 * «1 тайтл», «3 тайтла», «12 тайтлов» — счётчик папки. «Тайтл», а не «фильм»: в папке лежат и
 * фильмы, и сериалы. Одна функция на «Моё», экран папки и панель «В папку».
 */
fun titlesLabel(count: Int): String {
    val lastTwo = count % HUNDRED
    val last = count % TEN
    val word = when {
        lastTwo in TEENS -> "тайтлов"
        last == 1 -> "тайтл"
        last in FEW -> "тайтла"
        else -> "тайтлов"
    }
    return "$count $word"
}
