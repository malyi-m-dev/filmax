package com.filmax.data.tvlink.protocol

import com.filmax.core.domain.tvlink.PlaybackStatus
import com.filmax.core.domain.tvlink.RemoteCommand
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Строки протокола: что уходит в сеть и как разбирается сообщение другой версии приложения. */
class TvLinkCodecTest {

    @Test
    fun `every remote command survives the wire`() {
        val commands = listOf(
            RemoteCommand.Play(itemId = 2003, videoId = 7, season = 4, startSeconds = 1250),
            RemoteCommand.Pause,
            RemoteCommand.Resume,
            RemoteCommand.SeekBy(-10_000),
            RemoteCommand.SeekTo(600_000),
            RemoteCommand.NextEpisode,
            RemoteCommand.PreviousEpisode,
            RemoteCommand.Stop,
        )

        commands.forEach { command ->
            val line = TvLinkCodec.encode(command.toMessage())
            assertEquals(command, TvLinkCodec.decode(line)?.toCommand(), line)
        }
    }

    @Test
    fun `playback status survives the wire and closed player becomes idle`() {
        val status = PlaybackStatus(
            itemId = 2003,
            title = "Хроники Пограничья",
            posterUrl = "https://example.com/poster.jpg",
            season = 4,
            episode = 7,
            episodeTitle = "Перевал",
            playing = true,
            buffering = false,
            positionMs = 125_000,
            durationMs = 2_700_000,
            hasNext = true,
            hasPrevious = true,
        )

        val decoded = assertIs<TvLinkMessage.Status>(TvLinkCodec.decode(TvLinkCodec.encode(status.toMessage())))
        assertEquals(status, decoded.toDomain())
        assertEquals(TvLinkMessage.Idle, TvLinkCodec.decode(TvLinkCodec.encode(null.toMessage())))
    }

    @Test
    fun `message is one json line with a type`() {
        val line = TvLinkCodec.encode(TvLinkMessage.SeekBy(10_000))

        assertTrue('\n' !in line)
        assertTrue("\"type\":\"seek_by\"" in line, line)
    }

    @Test
    fun `newer phone fields are ignored, unknown commands are skipped`() {
        val play = TvLinkCodec.decode("""{"type":"play","itemId":1004,"subtitle":"ru"}""")

        assertEquals(RemoteCommand.Play(itemId = 1004, videoId = -1, season = -1, startSeconds = -1), play?.toCommand())
        assertNull(TvLinkCodec.decode("""{"type":"select_audio","label":"LostFilm"}"""))
        assertNull(TvLinkCodec.decode("not json"))
    }
}
