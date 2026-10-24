package com.filmax.data.tvlink.protocol

import com.filmax.core.domain.tvlink.PlaybackStatus
import com.filmax.core.domain.tvlink.RemoteCommand

internal fun RemoteCommand.toMessage(): TvLinkMessage = when (this) {
    is RemoteCommand.Play -> TvLinkMessage.Play(
        itemId = itemId,
        videoId = videoId,
        season = season,
        startSeconds = startSeconds,
    )
    RemoteCommand.Pause -> TvLinkMessage.Pause
    RemoteCommand.Resume -> TvLinkMessage.Resume
    is RemoteCommand.SeekBy -> TvLinkMessage.SeekBy(deltaMs)
    is RemoteCommand.SeekTo -> TvLinkMessage.SeekTo(positionMs)
    RemoteCommand.NextEpisode -> TvLinkMessage.Next
    RemoteCommand.PreviousEpisode -> TvLinkMessage.Previous
    RemoteCommand.Stop -> TvLinkMessage.Stop
}

/** Команда пульта из сообщения; null — сообщение не команда (ping, рукопожатие). */
internal fun TvLinkMessage.toCommand(): RemoteCommand? = when (this) {
    is TvLinkMessage.Play -> RemoteCommand.Play(
        itemId = itemId,
        videoId = videoId,
        season = season,
        startSeconds = startSeconds,
    )
    TvLinkMessage.Pause -> RemoteCommand.Pause
    TvLinkMessage.Resume -> RemoteCommand.Resume
    is TvLinkMessage.SeekBy -> RemoteCommand.SeekBy(deltaMs)
    is TvLinkMessage.SeekTo -> RemoteCommand.SeekTo(positionMs)
    TvLinkMessage.Next -> RemoteCommand.NextEpisode
    TvLinkMessage.Previous -> RemoteCommand.PreviousEpisode
    TvLinkMessage.Stop -> RemoteCommand.Stop
    else -> null
}

/** Снимок плеера в сообщение; закрытый плеер — [TvLinkMessage.Idle]. */
internal fun PlaybackStatus?.toMessage(): TvLinkMessage = this?.let {
    TvLinkMessage.Status(
        itemId = itemId,
        title = title,
        posterUrl = posterUrl,
        season = season,
        episode = episode,
        episodeTitle = episodeTitle,
        playing = playing,
        buffering = buffering,
        positionMs = positionMs,
        durationMs = durationMs,
        hasNext = hasNext,
        hasPrevious = hasPrevious,
    )
} ?: TvLinkMessage.Idle

internal fun TvLinkMessage.Status.toDomain() = PlaybackStatus(
    itemId = itemId,
    title = title,
    posterUrl = posterUrl,
    season = season,
    episode = episode,
    episodeTitle = episodeTitle,
    playing = playing,
    buffering = buffering,
    positionMs = positionMs,
    durationMs = durationMs,
    hasNext = hasNext,
    hasPrevious = hasPrevious,
)
