package com.filmax.core.presentation.folders

import com.filmax.core.domain.user.model.FOLDER_TITLE_MAX_LENGTH
import com.filmax.core.presentation.input.KeyboardLayout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Набор названия папки с экранной клавиатуры: регистр, пробелы, предел длины, цвет. */
class FolderDraftTest {

    @Test
    fun `first letter is capital, the rest as typed`() {
        val draft = type(FolderDraft(), "дача")

        assertEquals("Дача", draft.title)
    }

    @Test
    fun `space never starts the title and never doubles`() {
        val draft = FolderDraft()
            .apply(FolderInput.Space)
            .let { type(it, "а") }
            .apply(FolderInput.Space)
            .apply(FolderInput.Space)

        assertEquals("А ", draft.title)
        assertEquals("А", draft.cleanTitle)
    }

    @Test
    fun `text from the phone keyboard is kept as typed, without leading spaces and within the limit`() {
        val draft = FolderDraft().apply(FolderInput.Text("  дача " + "я".repeat(FOLDER_TITLE_MAX_LENGTH)))

        assertEquals("дача " + "я".repeat(FOLDER_TITLE_MAX_LENGTH - "дача ".length), draft.title)
    }

    @Test
    fun `title stops growing at the limit`() {
        val draft = type(FolderDraft(), "я".repeat(FOLDER_TITLE_MAX_LENGTH + 5))

        assertEquals(FOLDER_TITLE_MAX_LENGTH, draft.title.length)
    }

    @Test
    fun `erase removes the last letter and blank title cannot be submitted`() {
        val draft = type(FolderDraft(), "аб").apply(FolderInput.Erase).apply(FolderInput.Erase)

        assertEquals("", draft.title)
        assertFalse(draft.canSubmit)
        assertTrue(type(draft, "в").canSubmit)
    }

    @Test
    fun `tint wraps around the palette and layout switches`() {
        val draft = FolderDraft()
            .apply(FolderInput.Tint(OUT_OF_PALETTE))
            .apply(FolderInput.Layout(KeyboardLayout.EN))

        assertEquals(OUT_OF_PALETTE % PALETTE_SIZE, draft.tint)
        assertEquals(KeyboardLayout.EN, draft.layout)
    }

    private fun type(draft: FolderDraft, text: String): FolderDraft =
        text.fold(draft) { current, letter -> current.apply(FolderInput.Key(letter.toString())) }

    private companion object {
        const val PALETTE_SIZE = 6
        const val OUT_OF_PALETTE = 7
    }
}
