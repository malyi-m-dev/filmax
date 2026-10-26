package com.filmax.core.ui.components

import kotlin.test.Test
import kotlin.test.assertTrue

/** Ошибки распознавания объясняются словами, а не кодами. */
class VoiceErrorTextTest {

    @Test
    fun `voice errors are explained in words`() {
        assertTrue("интернет" in voiceErrorText(VoiceSearchError.NETWORK))
        assertTrue("микрофон" in voiceErrorText(VoiceSearchError.PERMISSION))
    }
}
