package com.filmax.data.catalog.mapper

import com.filmax.core.domain.catalog.model.AudioTrack
import com.filmax.core.domain.catalog.model.MediaTrack
import com.filmax.core.domain.catalog.model.SubtitleTrack
import com.filmax.core.domain.catalog.model.Trailer
import com.filmax.core.domain.catalog.model.VideoFile
import com.filmax.data.catalog.remote.dto.AudioDto
import com.filmax.data.catalog.remote.dto.MediaTrackDto
import com.filmax.data.catalog.remote.dto.SubtitleDto
import com.filmax.data.catalog.remote.dto.TrailerDto
import com.filmax.data.catalog.remote.dto.VideoFileDto

fun MediaTrackDto.toDomain(seasonNumber: Int = snumber) = MediaTrack(
    id = id,
    number = number,
    seasonNumber = seasonNumber,
    title = title,
    thumbnail = thumbnail,
    durationSeconds = duration,
    files = files.map { it.toDomain() },
    audios = audios.map { it.toDomain() },
    subtitles = subtitles.map { it.toDomain() },
    watchedSeconds = watching?.time?.coerceAtLeast(0) ?: 0,
    watchStatus = watching?.status ?: -1,
)

fun VideoFileDto.toDomain() = VideoFile(
    quality = quality,
    hls4 = url?.hls4,
    hls = url?.hls ?: urls?.hls,
    http = url?.http ?: urls?.http,
)

fun AudioDto.toDomain() = AudioTrack(
    id = id,
    lang = lang,
    title = title,
    channels = channels,
)

fun SubtitleDto.toDomain() = SubtitleTrack(
    lang = lang,
    url = url,
    shiftMs = shift,
)

fun TrailerDto.toDomain() = Trailer(id = id.toString(), url = url ?: "")
