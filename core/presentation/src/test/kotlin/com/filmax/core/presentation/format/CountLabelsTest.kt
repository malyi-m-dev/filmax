package com.filmax.core.presentation.format

import kotlin.test.Test
import kotlin.test.assertEquals

class CountLabelsTest {

    @Test
    fun `films are declined by the last two digits`() {
        assertEquals("1 фильм", filmsLabel(1))
        assertEquals("3 фильма", filmsLabel(3))
        assertEquals("5 фильмов", filmsLabel(5))
        assertEquals("11 фильмов", filmsLabel(11))
        assertEquals("21 фильм", filmsLabel(21))
        assertEquals("24 фильма", filmsLabel(24))
        assertEquals("112 фильмов", filmsLabel(112))
    }

    @Test
    fun `titles are declined the same way`() {
        assertEquals("1 тайтл", titlesLabel(1))
        assertEquals("4 тайтла", titlesLabel(4))
        assertEquals("12 тайтлов", titlesLabel(12))
    }
}
