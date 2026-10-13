package com.filmax.core.presentation.input

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Раскладки редактора папки: ряды влезают в одиннадцать колонок и не теряют ни одной буквы. */
class ScreenKeyboardTest {

    @Test
    fun `every row fits the eleven column panel keyboard`() {
        KeyboardLayout.entries.forEach { layout ->
            assertTrue(keyboardRows(layout).all { row -> row.size <= PANEL_COLUMNS }, "$layout шире панели")
        }
    }

    @Test
    fun `russian layout starts with jcuken and keeps the whole alphabet`() {
        val rows = keyboardRows(KeyboardLayout.RU)

        assertEquals("йцукенгшщзх", rows.first().joinToString(""))
        assertEquals(RUSSIAN_ALPHABET.toSet(), rows.flatten().joinToString("").toSet())
    }

    @Test
    fun `english and digits keep every key`() {
        assertEquals(('a'..'z').toSet(), keyboardRows(KeyboardLayout.EN).flatten().joinToString("").toSet())
        assertEquals("1234567890", keyboardRows(KeyboardLayout.DIGITS).flatten().joinToString(""))
    }

    private companion object {
        const val PANEL_COLUMNS = 11
        const val RUSSIAN_ALPHABET = "абвгдеёжзийклмнопрстуфхцчшщъыьэюя"
    }
}
