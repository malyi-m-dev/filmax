package com.filmax.feature.search.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Раскладки экранной клавиатуры, правила набора с пульта и подсветка совпадения в названии. */
class SearchKeyboardTest {

    @Test
    fun `every layout fits the eight-column grid and leaves room for space and delete`() {
        KeyboardLayout.entries.forEach { layout ->
            val rows = keyboardRows(layout)
            assertTrue(rows.all { it.size <= KEYBOARD_COLUMNS }, "$layout: ряд шире восьми клавиш")
            assertTrue(rows.last().size <= KEYBOARD_COLUMNS - 2, "$layout: «Пробелу» и «Стереть» нет места")
        }
    }

    @Test
    fun `russian layout has the whole alphabet with yo in order`() {
        val letters = keyboardRows(KeyboardLayout.RU).flatten().joinToString("")

        assertEquals("абвгдеёжзийклмнопрстуфхцчшщъыьэюя", letters)
    }

    @Test
    fun `english and digit layouts are complete`() {
        assertEquals(('a'..'z').joinToString(""), keyboardRows(KeyboardLayout.EN).flatten().joinToString(""))
        assertEquals("1234567890", keyboardRows(KeyboardLayout.DIGITS).flatten().joinToString(""))
    }

    @Test
    fun `layouts are labelled in switcher order`() {
        assertEquals(listOf("RU", "EN", "123"), KeyboardLayoutLabels.map { (_, label) -> label })
        assertEquals(KeyboardLayout.entries, KeyboardLayoutLabels.map { (layout, _) -> layout })
    }

    @Test
    fun `typing appends letters`() {
        assertEquals("хр", typedQuery("х", "р"))
    }

    @Test
    fun `space is not typed at the start or twice in a row`() {
        assertEquals("", typedQuery("", SPACE_KEY))
        assertEquals("дом ", typedQuery("дом", SPACE_KEY))
        assertEquals("дом ", typedQuery("дом ", SPACE_KEY))
    }

    @Test
    fun `match is found ignoring case and outer spaces`() {
        assertEquals(0 until 5, matchRange("Хроники Пограничья", " хрони "))
        assertEquals(8 until 12, matchRange("Хроники Пограничья", "ПОГР"))
    }

    @Test
    fun `match treats yo as e both ways`() {
        assertEquals(0 until 5, matchRange("Шёпот в соснах", "шепот"))
        assertEquals(0 until 5, matchRange("Шепот в соснах", "шёпот"))
    }

    @Test
    fun `no match for a blank query or a title found by something else`() {
        assertNull(matchRange("Хроники Пограничья", "  "))
        assertNull(matchRange("Хроники Пограничья", "Нильсен"))
    }

    private companion object {
        const val KEYBOARD_COLUMNS = 8
    }
}
