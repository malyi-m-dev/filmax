package com.filmax.core.domain.catalog

import com.filmax.core.domain.catalog.model.audioLanguageName
import com.filmax.core.domain.catalog.model.subtitleLanguageName
import kotlin.test.Test
import kotlin.test.assertEquals

/** Коды языков kino.pub бывают трёх- и двухбуквенными и в любом регистре — название одно. */
class TrackLanguagesTest {

    @Test
    fun `both code forms give the same name regardless of case`() {
        assertEquals("Русский", audioLanguageName("rus"))
        assertEquals("Русский", audioLanguageName("RU"))
        assertEquals("English", subtitleLanguageName("eng"))
        assertEquals("Українська", subtitleLanguageName("uk"))
    }

    @Test
    fun `missing code falls back to a neutral label`() {
        assertEquals("Оригинал", audioLanguageName(null))
        assertEquals("Оригинал", audioLanguageName(""))
        assertEquals("Субтитры", subtitleLanguageName(null))
    }

    @Test
    fun `unknown code is shown as is`() {
        assertEquals("fra", audioLanguageName("fra"))
    }
}
