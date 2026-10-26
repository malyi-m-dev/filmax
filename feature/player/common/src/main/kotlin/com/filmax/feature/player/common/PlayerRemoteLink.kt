package com.filmax.feature.player.common

import androidx.media3.common.Player
import com.filmax.core.domain.catalog.model.MediaTrack
import com.filmax.core.domain.tvlink.PlaybackStatus
import com.filmax.core.domain.tvlink.RemoteCommand
import com.filmax.core.domain.tvlink.TvLinkReceiver

/**
 * Плеер глазами пульта на телефоне: снимки — приёмнику ТВ, команды пульта — плееру. Транспорт
 * (пауза, перемотка) исполняется на месте, а другая серия и выход — это навигация, её делает
 * модель через [onPlayEpisode] и [onStop].
 *
 * Плеер сообщает о себе всегда, на телефоне тоже: приёмник там не поднят, снимки никому не уходят
 * и команд не приходит. Зато модели не нужно знать, на каком она устройстве.
 */
internal class PlayerRemoteLink(
    private val receiver: TvLinkReceiver,
    private val player: Player,
    private val state: () -> PlayerState,
) {

    /** Последний снимок этого плеера — чтобы, уходя, не стереть снимок плеера следующей серии. */
    private var published: PlaybackStatus? = null

    suspend fun listen(onPlayEpisode: (MediaTrack) -> Unit, onStop: () -> Unit) {
        receiver.commands.collect { command ->
            when (command) {
                RemoteCommand.Pause -> player.pause()
                RemoteCommand.Resume -> player.play()
                is RemoteCommand.SeekBy -> seekTo(player.currentPosition + command.deltaMs)
                is RemoteCommand.SeekTo -> seekTo(command.positionMs)
                RemoteCommand.NextEpisode -> state().nextTrack?.let(onPlayEpisode)
                RemoteCommand.PreviousEpisode -> state().previousTrack?.let(onPlayEpisode)
                RemoteCommand.Stop -> onStop()
                // Новый тайтл открывает граф ТВ (приёмник в :app), а не этот плеер.
                is RemoteCommand.Play -> Unit
            }
        }
    }

    fun publish() {
        val status = playbackStatus(state(), player) ?: return
        published = status
        receiver.publish(status)
    }

    /** Плеер ушёл с экрана: пульт видит «ничего не играет», если следующая серия ещё не заиграла. */
    fun withdraw() {
        val own = published ?: return
        published = null
        if (receiver.playback.value == own) receiver.publish(null)
    }

    /** Перемотка в пределах тайтла; пока длительность неизвестна, ограничено только началом. */
    private fun seekTo(positionMs: Long) {
        val end = player.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
        player.seekTo(positionMs.coerceIn(0, end))
    }
}

/** Снимок для пульта; null — тайтл ещё не загрузился. */
internal fun playbackStatus(state: PlayerState, player: Player): PlaybackStatus? {
    val item = state.item ?: return null
    val track = state.track
    return PlaybackStatus(
        itemId = item.id,
        title = item.title,
        posterUrl = item.posters.medium.ifBlank { null },
        season = track?.seasonNumber ?: -1,
        episode = track?.number ?: -1,
        episodeTitle = track?.title?.ifBlank { null },
        playing = player.isPlaying,
        buffering = player.playbackState == Player.STATE_BUFFERING,
        positionMs = player.currentPosition.coerceAtLeast(0),
        durationMs = player.duration.coerceAtLeast(0),
        hasNext = state.nextTrack != null,
        hasPrevious = state.previousTrack != null,
    )
}
