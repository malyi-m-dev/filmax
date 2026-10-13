package com.filmax.core.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

/** Разбиение речи на подтверждённое и гипотезу, громкость для эквалайзера. */
class VoiceTranscriptTest {

    @Test
    fun `first partial result is all hypothesis`() {
        assertEquals(VoiceTranscript("", "хрони"), nextTranscript("", "хрони"))
    }

    @Test
    fun `words that survived an update are confirmed, the word being spoken is not`() {
        val transcript = nextTranscript("хроники пог", "хроники погра")

        assertEquals(VoiceTranscript("хроники ", "погра"), transcript)
        assertEquals("хроники погра", transcript.text)
    }

    @Test
    fun `finished word is confirmed as soon as the next one starts`() {
        assertEquals(VoiceTranscript("хроники", " погра"), nextTranscript("хроники", "хроники погра"))
    }

    @Test
    fun `rewritten ending stays a hypothesis`() {
        assertEquals(VoiceTranscript("северный ", "ветер"), nextTranscript("северный вечер", "северный ветер"))
    }

    @Test
    fun `repeated result is fully confirmed`() {
        assertEquals(VoiceTranscript("дом на краю", ""), nextTranscript("дом на краю", "дом на краю"))
    }

    @Test
    fun `unstable tail from the recognizer wins over the guess`() {
        assertEquals(VoiceTranscript("хроники погра", " ничья"), nextTranscript("", "хроники погра", "ничья"))
        assertEquals(VoiceTranscript("хроники ", "погра"), nextTranscript("", "хроники ", "погра"))
    }

    @Test
    fun `loudness maps to a bar level and is clamped`() {
        assertEquals(0f, voiceLevel(QUIET_ROOM_DB))
        assertEquals(1f, voiceLevel(SHOUT_DB))
        assertEquals(HALF, voiceLevel(SPEECH_DB))
    }

    private companion object {
        const val QUIET_ROOM_DB = -5f
        const val SHOUT_DB = 14f
        const val SPEECH_DB = 4f
        const val HALF = 0.5f
    }
}
