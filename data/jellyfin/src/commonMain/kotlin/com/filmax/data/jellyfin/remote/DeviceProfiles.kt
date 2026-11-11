package com.filmax.data.jellyfin.remote

import com.filmax.data.jellyfin.remote.dto.DeviceProfileDto
import com.filmax.data.jellyfin.remote.dto.DirectPlayProfileDto
import com.filmax.data.jellyfin.remote.dto.SubtitleProfileDto
import com.filmax.data.jellyfin.remote.dto.TranscodingProfileDto

/**
 * Что умеет плеер (Media3 ExoPlayer) — по этому профилю сервер решает, отдать файл как есть
 * или перекодировать. Ошибаться в сторону «умеет» опасно по-разному: неподдержанный видеокодек
 * плеер заметит ошибкой и уйдёт на запасной поток, а неподдержанный аудиокодек он молча
 * выключит — фильм пойдёт без звука. Поэтому аудио перечислено осторожно: AC-3 и E-AC-3
 * декодируют почти все ТВ-приставки и почти никакие телефоны, и телефон получает их
 * перекодированными в AAC (видео при этом копируется — серверу это почти бесплатно).
 */
internal object DeviceProfiles {

    fun of(tv: Boolean): DeviceProfileDto {
        val audio = if (tv) BASE_AUDIO + DOLBY_AUDIO else BASE_AUDIO
        return DeviceProfileDto(
            name = "Filmax",
            maxStreamingBitrate = MAX_BITRATE,
            maxStaticBitrate = MAX_BITRATE,
            directPlayProfiles = listOf(
                DirectPlayProfileDto(container = CONTAINERS, videoCodec = VIDEO, audioCodec = audio.joinToString(",")),
            ),
            transcodingProfiles = listOf(
                TranscodingProfileDto(
                    container = "ts",
                    videoCodec = "h264",
                    audioCodec = (if (tv) listOf("aac", "ac3", "eac3") else listOf("aac")).joinToString(","),
                    protocol = "hls",
                    maxAudioChannels = if (tv) SURROUND else STEREO,
                ),
            ),
            // Текстовые субтитры сервер отдаёт отдельным VTT: их понимает плеер и при прямом
            // файле, и при перекодировании, где встроенные дорожки теряются.
            subtitleProfiles = listOf(SubtitleProfileDto(format = "vtt", method = "External")),
        )
    }

    /** Потолок «как есть» — выше любого домашнего файла: в своей сети ограничивать незачем. */
    const val MAX_BITRATE = 200_000_000L

    private const val CONTAINERS = "mp4,m4v,mkv,webm,mov,ts,mpegts"
    private const val VIDEO = "h264,hevc,vp8,vp9,av1,mpeg4"
    private val BASE_AUDIO = listOf("aac", "mp3", "opus", "flac", "vorbis", "alac")
    private val DOLBY_AUDIO = listOf("ac3", "eac3")
    private const val SURROUND = "6"
    private const val STEREO = "2"
}
