package com.filmax.feature.search.tv

import androidx.compose.ui.graphics.Color
import com.filmax.core.ui.components.VoiceSearchError
import com.filmax.core.ui.components.voiceErrorText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Подписи ТВ-каталога, которым нужен экран: подсветка совпадения, скелетоны догрузки, ошибки голоса. */
class TvCatalogLabelsTest {

    @Test
    fun `title highlights the typed part in the accent color`() {
        val title = highlightedTitle("Хроники Пограничья", "хрони", Accent)

        assertEquals("Хроники Пограничья", title.text)
        val span = title.spanStyles.single()
        assertEquals(0 until 5, span.start until span.end)
        assertEquals(Accent, span.item.color)
    }

    @Test
    fun `title without a match is left as is`() {
        assertTrue(highlightedTitle("Хроники Пограничья", "нильсен", Accent).spanStyles.isEmpty())
    }

    @Test
    fun `load more skeletons fill the last row and add one more`() {
        assertEquals(CATALOG_COLUMNS, loadMoreSkeletons(40))
        assertEquals(3 + CATALOG_COLUMNS, loadMoreSkeletons(42))
    }

    @Test
    fun `voice errors are explained in words`() {
        assertTrue("интернет" in voiceErrorText(VoiceSearchError.NETWORK))
        assertTrue("микрофон" in voiceErrorText(VoiceSearchError.PERMISSION))
    }

    private companion object {
        val Accent = Color(0xFFBEB2FF)
    }
}
